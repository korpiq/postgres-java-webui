import { Given, When, Then, Before, After } from '@cucumber/cucumber';
import { Builder, By, WebDriver } from 'selenium-webdriver';
import * as chrome from 'selenium-webdriver/chrome.js';
import { spawn, ChildProcess } from 'child_process';
import http from 'http';

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
            process.kill(-frontendProcess.pid);
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
