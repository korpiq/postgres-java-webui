# Testing Documentation

## Running the Gherkin Tests

### Prerequisites

1. Ensure Docker is running (required for Testcontainers)
2. Generate JWT keys if not already done:
   ```bash
   ./scripts/generate-jwt-keys.sh
   ```

### Running All Tests

To run all Cucumber BDD tests:

```bash
./gradlew test
```

### Running Specific Feature Tests

To run only the login feature tests:

```bash
./gradlew test --tests "fi.iki.korpiq.pogrejab.CucumberTestRunner"
```

### Test Reports

After running tests, you can view the reports at:
- HTML Report: `build/reports/cucumber/cucumber.html`
- JSON Report: `build/reports/cucumber/cucumber.json`
- JUnit Report: `build/reports/tests/test/index.html`

### Test Structure

```
src/test/
├── java/
│   └── fi/iki/korpiq/pogrejab/
│       ├── CucumberTestRunner.java      # Main test runner
│       ├── TestContext.java              # Shared test context
│       └── steps/
│           └── LoginSteps.java           # Step definitions for login feature
└── resources/
    ├── features/
    │   └── login.feature                 # Gherkin feature file for login
    └── junit-platform.properties         # Cucumber configuration
```

### How It Works

1. **Testcontainers**: Each test scenario automatically starts a temporary PostgreSQL container
2. **Setup**: The `@Before` hook creates the Postgres instance and test users
3. **Execution**: REST API calls are made using REST Assured
4. **Verification**: JWT tokens are verified using the Auth0 JWT library
5. **Teardown**: The `@After` hook stops the container and closes connections

### Current Test Coverage

- ✅ Successful login with valid Postgres credentials
- ✅ Failed login with invalid password
- ✅ Failed login with non-existent user
- ✅ JWT token contains valid session ID
- ✅ JWT token is properly signed with private key
- ✅ Session mapping to Postgres connection

### Notes

- Tests require the JWT keys to be generated in `.secrets/` directory
- Testcontainers requires Docker to be running
- Each test scenario gets a fresh Postgres instance (isolated testing)
- The login API endpoint must be implemented for tests to pass