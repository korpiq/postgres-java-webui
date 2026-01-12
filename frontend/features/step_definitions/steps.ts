import { Given, When, Then, Before, After } from '@cucumber/cucumber';
import { Builder, By, WebDriver, until } from 'selenium-webdriver';
import * as chrome from 'selenium-webdriver/chrome.js';
import { spawn, ChildProcess } from 'child_process';
import http from 'http';
import jwt from 'jsonwebtoken';
import fs from 'fs';
import path from 'path';

let driver: WebDriver;
let frontendProcess: ChildProcess;
const PORT = 3000;

Before({ timeout: 60000 }, async function () {
    // Start frontend
    frontendProcess = spawn('npm', ['start'], {
        cwd: '.',
        env: { ...process.env, PORT: PORT.toString(), BROWSER: 'none' },
        shell: true,
        detached: true
    });

    // Wait for frontend to be ready
    await new Promise<void>((resolve, reject) => {
        const timeout = setTimeout(() => reject(new Error('Frontend start timeout')), 30000);
        const check = () => {
            http.get(`http://localhost:${PORT}`, (res) => {
                if (res.statusCode === 200) {
                    clearTimeout(timeout);
                    resolve();
                } else {
                    setTimeout(check, 500);
                }
            }).on('error', () => {
                setTimeout(check, 500);
            });
        };
        check();
    });

    const options = new chrome.Options();
    options.addArguments('--headless');
    options.addArguments('--no-sandbox');
    options.addArguments('--disable-dev-shm-usage');

    driver = await new Builder()
        .forBrowser('chrome')
        .setChromeOptions(options)
        .build();
});

After(async function () {
    if (driver) {
        await driver.quit();
    }
    if (frontendProcess && frontendProcess.pid) {
        // Kill the process group on Unix
        if (process.platform !== 'win32') {
            try {
                process.kill(-frontendProcess.pid);
            } catch (e) {
                // Ignore errors if process is already dead
            }
        } else {
            frontendProcess.kill();
        }
    }
});

Given('the frontend application is started', async function () {
    // Already handled in Before hook
});

When('I access the frontend root page', async function () {
    await driver.get(`http://localhost:${PORT}`);
});

Then('I should see {string} on the page', async function (text: string) {
    const bodyText = await driver.findElement(By.tagName('body')).getText();
    if (!bodyText.includes(text)) {
        throw new Error(`Expected text "${text}" not found on page`);
    }
});

Given('a temporary Postgres instance is running', async function () {
    // In frontend-only work, we assume the backend handles this or we mock it.
    // For now, we'll just assume it's true if the environment is set up.
});

Given('a Postgres user {string} with password {string} exists', async function (username: string, password: string) {
    // Placeholder for frontend tests
});

Given('I am on the login page', async function () {
    await driver.get(`http://localhost:${PORT}/login`);
});

When('I enter {string} and {string} and click login', async function (username, password) {
    await driver.findElement(By.name('username')).sendKeys(username);
    await driver.findElement(By.name('password')).sendKeys(password);
    await driver.findElement(By.css('button[type="submit"]')).click();
});

Then('a JWT cookie should be set', async function () {
    const cookies = await driver.manage().getCookies();
    const jwtCookie = cookies.find(c => c.name === 'jwt');
    if (!jwtCookie) {
        throw new Error('JWT cookie not found');
    }
});

Then('the JWT cookie should be signed with the backend secret key', async function () {
    const cookies = await driver.manage().getCookies();
    const jwtCookie = cookies.find(c => c.name === 'jwt');
    if (!jwtCookie) {
        throw new Error('JWT cookie not found');
    }

    const token = jwtCookie.value;
    const publicKeyPath = path.resolve(process.cwd(), '../.secrets/jwt_public_key.pem');
    const publicKey = fs.readFileSync(publicKeyPath, 'utf8');

    try {
        jwt.verify(token, publicKey, { algorithms: ['RS256'] });
    } catch (err: any) {
        throw new Error(`JWT verification failed: ${err.message}`);
    }
});
