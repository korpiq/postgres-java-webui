const { Given, When, Then, Before, After } = require('@cucumber/cucumber');
const { Builder, By, until } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');
const { spawn } = require('child_process');
const http = require('http');

let driver;
let frontendProcess;
const PORT = 3000;

Before({ timeout: 60000 }, async function () {
    // Start frontend
    frontendProcess = spawn('npm', ['start'], {
        cwd: '.',
        env: { ...process.env, PORT: PORT, BROWSER: 'none' },
        shell: true,
        detached: true
    });

    // Wait for frontend to be ready
    await new Promise((resolve, reject) => {
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
    if (frontendProcess) {
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

Then('I should see {string} on the page', async function (text) {
    const bodyText = await driver.findElement(By.tagName('body')).getText();
    if (!bodyText.includes(text)) {
        throw new Error(`Expected text "${text}" not found on page`);
    }
});
