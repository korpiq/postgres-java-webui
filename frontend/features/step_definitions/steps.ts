import { Given, When, Then, Before, After, BeforeAll, AfterAll } from '@cucumber/cucumber';
import { Builder, By, WebDriver, until } from 'selenium-webdriver';
import * as chrome from 'selenium-webdriver/chrome.js';
import { spawn, ChildProcess, execSync } from 'child_process';
import http from 'http';
import jwt from 'jsonwebtoken';
import fs from 'fs';
import path from 'path';

let driver: WebDriver;
let frontendProcess: ChildProcess;
const PORT = 3000;

BeforeAll({ timeout: 120000 }, async function () {
    console.log('Starting backend for all tests...');
    execSync('cd .. && ./scripts/start-backend.sh', { stdio: 'inherit' });
});

AfterAll(async function () {
    console.log('Stopping backend after all tests...');
    execSync('cd .. && ./scripts/stop-backend.sh', { stdio: 'inherit' });
});

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

    // Cleanup specific test user and database if they were created
    try {
        const cleanupDbCmd = `docker compose exec -T postgres psql -U pogrejab -d pogrejab_db -c "DROP DATABASE IF EXISTS testdb"`;
        execSync(`cd .. && ${cleanupDbCmd}`, { stdio: 'pipe' });
        const cleanupUserCmd = `docker compose exec -T postgres psql -U pogrejab -d pogrejab_db -c "DROP USER IF EXISTS testuser"`;
        execSync(`cd .. && ${cleanupUserCmd}`, { stdio: 'pipe' });
    } catch (e) {
        // Ignore cleanup errors
    }
});

Given('the frontend application is started', async function () {
    // Already handled in Before hook
});

When('I access the frontend root page', async function () {
    await driver.get(`http://localhost:${PORT}`);
});

When('I click on the link for database {string}', { timeout: 30000 }, async function (dbname: string) {
    const link = await driver.wait(until.elementLocated(By.linkText(dbname)), 10000);
    await driver.executeScript("arguments[0].click();", link);
    // Wait for the URL to change to the expected path
    await driver.wait(until.urlContains(`/db/${dbname}`), 10000);
});

Then('I should see {string} on the page', { timeout: 30000 }, async function (text: string) {
    await driver.wait(until.elementLocated(By.tagName('body')), 5000);
    const body = await driver.findElement(By.tagName('body'));
    try {
        await driver.wait(async () => {
            const bodyText = await body.getText();
            return bodyText.includes(text);
        }, 5000, `Expected text "${text}" not found on page after timeout`);
    } catch (err) {
        const logs = await driver.manage().logs().get('browser');
        console.log('Browser logs:', JSON.stringify(logs, null, 2));
        const bodyText = await body.getText();
        console.log('Page body text:', bodyText);
        throw err;
    }
});

Given('a temporary Postgres instance is running', { timeout: 60000 }, async function () {
    // We rely on the docker-compose instance started by setup-db.sh or similar
    try {
        execSync('cd .. && ./scripts/setup-db.sh start', { stdio: 'pipe' });
    } catch (e) {
        // If it fails, maybe it's already running or docker is not available,
        // but we'll try to proceed anyway.
    }
});

Given('a Postgres user {string} with password {string} exists', async function (username: string, password: string) {
    const cmd = `docker compose exec -T postgres psql -U pogrejab -d pogrejab_db -c "DO \\$\\$ BEGIN IF NOT EXISTS (SELECT FROM pg_catalog.pg_user WHERE usename = '${username}') THEN CREATE USER ${username} WITH PASSWORD '${password}'; END IF; END \\$\\$;"`;
    execSync(`cd .. && ${cmd}`, { stdio: 'inherit' });
});

Given('a database {string} owned by {string} exists', async function (dbname: string, username: string) {
    const cmd = `docker compose exec -T postgres psql -U pogrejab -d pogrejab_db -c "SELECT 1 FROM pg_database WHERE datname = '${dbname}'"`;
    const result = execSync(`cd .. && ${cmd}`, { encoding: 'utf8' });
    if (!result.includes('1')) {
        const createCmd = `docker compose exec -T postgres psql -U pogrejab -d pogrejab_db -c "CREATE DATABASE ${dbname} OWNER ${username}"`;
        execSync(`cd .. && ${createCmd}`, { stdio: 'inherit' });
    }
});

Given('a database {string} exists', async function (dbname: string) {
    const cmd = `docker compose exec -T postgres psql -U pogrejab -d pogrejab_db -c "SELECT 1 FROM pg_database WHERE datname = '${dbname}'"`;
    const result = execSync(`cd .. && ${cmd}`, { encoding: 'utf8' });
    if (!result.includes('1')) {
        const createCmd = `docker compose exec -T postgres psql -U pogrejab -d pogrejab_db -c "CREATE DATABASE ${dbname}"`;
        execSync(`cd .. && ${createCmd}`, { stdio: 'inherit' });
    }
});

Given('a schema {string} exists in database {string}', async function (schemaname: string, dbname: string) {
    // We assume the testuser exists and has rights or we use the superuser to create and grant
    const cmd = `docker compose exec -T postgres psql -U pogrejab -d ${dbname} -c "CREATE SCHEMA IF NOT EXISTS ${schemaname}; GRANT USAGE ON SCHEMA ${schemaname} TO public;"`;
    execSync(`cd .. && ${cmd}`, { stdio: 'inherit' });
});

When('I access the schemas page for {string}', { timeout: 30000 }, async function (dbname: string) {
    await driver.get(`http://localhost:${PORT}/db/${dbname}`);
});

Given('I am logged in as {string} with password {string}', { timeout: 30000 }, async function (username, password) {
    await driver.get(`http://localhost:${PORT}/login`);
    await driver.findElement(By.name('username')).sendKeys(username);
    await driver.findElement(By.name('password')).sendKeys(password);
    await driver.findElement(By.css('button[type="submit"]')).click();
    // Two-step login: Select database
    const select = await driver.wait(until.elementLocated(By.tagName('select')), 10000);
    await driver.findElement(By.css('button[type="submit"]')).click(); // Connect to the first one
    await driver.wait(until.urlContains('/db/'), 5000);
});

When('I enter {string} and {string} and click {string}', async function (username, password, buttonText) {
    await driver.findElement(By.name('username')).sendKeys(username);
    await driver.findElement(By.name('password')).sendKeys(password);
    const button = await driver.findElement(By.xpath(`//button[contains(text(), '${buttonText}')]`));
    await button.click();
});

Then('I should see a list of databases including {string}', async function (dbname: string) {
    await driver.wait(until.elementLocated(By.tagName('select')), 10000);
    const select = await driver.findElement(By.tagName('select'));
    const options = await select.findElements(By.tagName('option'));
    let found = false;
    for (const option of options) {
        if (await option.getText() === dbname) {
            found = true;
            break;
        }
    }
    if (!found) {
        throw new Error(`Database ${dbname} not found in the list`);
    }
});

When('I select {string} and click {string}', async function (dbname: string, buttonText: string) {
    const select = await driver.findElement(By.tagName('select'));
    await select.sendKeys(dbname);
    const button = await driver.findElement(By.xpath(`//button[contains(text(), '${buttonText}')]`));
    await button.click();
});

Then('I should be redirected to {string}', async function (path: string) {
    await driver.wait(until.urlContains(path), 5000);
});

Then('a cookie {string} should be set', async function (cookieName: string) {
    const cookies = await driver.manage().getCookies();
    const myCookie = cookies.find(c => c.name === cookieName);
    if (!myCookie) {
        throw new Error(`Cookie ${cookieName} not found`);
    }
});

Given('I am on the login page', async function () {
    await driver.get(`http://localhost:${PORT}/login`);
});

Given('an obsolete JWT cookie is set', async function () {
    const token = execSync('cd .. && ./gradlew -q run -PmainClass=fi.iki.korpiq.pogrejab.GenerateInvalidJwt', { encoding: 'utf8' }).trim();
    // Navigate to the domain first before setting cookie
    await driver.get(`http://localhost:${PORT}/login`);
    await driver.wait(until.elementLocated(By.tagName('body')), 5000);
    // Use template literal for the token and handle potential newlines/quotes
    const escapedToken = token.replace(/[\n\r]/g, '');
    await driver.executeScript(`document.cookie = "pogrejab_testdb=${escapedToken}; path=/";`);
});

Then('I should be redirected to the login page', async function () {
    await driver.wait(until.urlContains('/login'), 5000);
    const url = await driver.getCurrentUrl();
    if (!url.includes('/login')) {
        throw new Error(`Expected to be on login page, but was at ${url}`);
    }
});
