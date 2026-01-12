package fi.iki.korpiq.pogrejab.steps;

import fi.iki.korpiq.pogrejab.TestContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatabaseSteps {
    private final TestContext testContext;

    public DatabaseSteps(TestContext testContext) {
        this.testContext = testContext;
    }

    @Given("a database {string} exists")
    public void aDatabaseExists(String dbName) throws SQLException {
        PostgreSQLContainer<?> postgres = testContext.getPostgresContainer();
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword())) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE DATABASE " + dbName);
                // Revoke CONNECT from PUBLIC so it's not visible by default in our test logic
                stmt.execute("REVOKE CONNECT ON DATABASE " + dbName + " FROM PUBLIC");
            }
        }
    }

    @And("the user {string} has privilege to see the database {string}")
    public void theUserHasPrivilegeToSeeTheDatabase(String username, String dbName) throws SQLException {
        PostgreSQLContainer<?> postgres = testContext.getPostgresContainer();
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword())) {
            try (Statement stmt = conn.createStatement()) {
                // In Postgres, simply having CONNECT privilege or being able to see it in pg_database
                // For this test, we might want to ensure the user can connect or similar.
                // By default all users can see all databases in pg_database.
                // However, the "listing" API might be implemented to filter.
                // Let's grant CONNECT as a way to "authorize" it for our app logic.
                stmt.execute("GRANT CONNECT ON DATABASE " + dbName + " TO " + username);
                
                // Also grant membership if needed? 
                // Wait, if it's a new user, they might not have any permissions.
            }
        }
        
        // IMPORTANT: In Postgres, GRANT CONNECT ON DATABASE only works if we are connected to THAT database
        // or a database where the user exists. Actually user is global.
        // But some permissions are database-specific.
        
        // Let's also connect to the new database and grant some basic permissions if needed
        String url = postgres.getJdbcUrl();
        String baseUrl = url.substring(0, url.lastIndexOf("/") + 1);
        String dbUrl = baseUrl + dbName;
        try (Connection conn = DriverManager.getConnection(dbUrl, postgres.getUsername(), postgres.getPassword())) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("GRANT USAGE ON SCHEMA public TO " + username);
            }
        } catch (SQLException e) {
            // Ignore if public schema doesn't exist or whatever
        }
    }

    @And("I am logged in as {string} with password {string}")
    public void iAmLoggedInAsWithPassword(String username, String password) {
        // Step 1: Get databases
        Response dbResponse = RestAssured.given()
                .contentType("application/json")
                .body(Map.of("username", username, "password", password))
                .when()
                .post("/api/databases");
        
        assertEquals(200, dbResponse.getStatusCode(), "Failed to list databases for " + username);
        List<String> databases = dbResponse.jsonPath().getList("databases", String.class);
        assertFalse(databases.isEmpty(), "No databases found for " + username);
        
        // Find a database we can connect to. 
        // In our tests, we usually grant access to specific databases.
        // Let's try to connect to each until one succeeds.
        String token = null;
        
        for (String dbName : databases) {
            Response loginResponse = RestAssured.given()
                    .contentType("application/json")
                    .body(Map.of("username", username, "password", password, "dbName", dbName))
                    .when()
                    .post("/api/login");
            
            if (loginResponse.getStatusCode() == 200) {
                token = loginResponse.jsonPath().getString("token");
                break;
            }
        }
        
        assertNotNull(token, "Login failed for " + username + " to any of the databases: " + databases);
        testContext.setJwtToken(token);
    }

    @And("I connect to database {string} as {string} with password {string}")
    public void iConnectToDatabaseAsWithPassword(String dbName, String username, String password) {
        Response loginResponse = RestAssured.given()
                .contentType("application/json")
                .body(Map.of("username", username, "password", password, "dbName", dbName))
                .when()
                .post("/api/login");

        assertEquals(200, loginResponse.getStatusCode(), "Login failed for " + username + " to " + dbName);
        String token = loginResponse.jsonPath().getString("token");
        assertNotNull(token, "Login did not return a token");
        testContext.setJwtToken(token);
    }

    @When("I request the list of databases")
    public void iRequestTheListOfDatabases() {
        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + testContext.getJwtToken())
                .when()
                .get("/api/databases");
        
        testContext.setLastResponse(response);
    }

    @And("the response should contain {string}")
    public void theResponseShouldContain(String expectedDb) {
        Response response = testContext.getLastResponse();
        List<String> databases = response.jsonPath().getList("databases", String.class);
        assertTrue(databases.contains(expectedDb), "Response does not contain " + expectedDb);
    }

    @And("the response should not contain {string}")
    public void theResponseShouldNotContain(String unexpectedDb) {
        Response response = testContext.getLastResponse();
        List<String> databases = response.jsonPath().getList("databases", String.class);
        assertFalse(databases.contains(unexpectedDb), "Response contains " + unexpectedDb);
    }

    @And("a schema {string} exists in database {string}")
    public void aSchemaExistsInDatabase(String schemaName, String dbName) throws SQLException {
        PostgreSQLContainer<?> postgres = testContext.getPostgresContainer();
        // To create a schema in a specific database, we need to connect to that database
        String url = postgres.getJdbcUrl();
        // Replace the default database name (usually 'test') with dbName
        String baseUrl = url.substring(0, url.lastIndexOf("/") + 1);
        String dbUrl = baseUrl + dbName;
        
        try (Connection conn = DriverManager.getConnection(dbUrl, postgres.getUsername(), postgres.getPassword())) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE SCHEMA " + schemaName);
            }
        }
    }

    @And("the user {string} has privilege to see the schema {string} in {string}")
    public void theUserHasPrivilegeToSeeTheSchemaIn(String username, String schemaName, String dbName) throws SQLException {
        PostgreSQLContainer<?> postgres = testContext.getPostgresContainer();
        String url = postgres.getJdbcUrl();
        String baseUrl = url.substring(0, url.lastIndexOf("/") + 1);
        String dbUrl = baseUrl + dbName;

        try (Connection conn = DriverManager.getConnection(dbUrl, postgres.getUsername(), postgres.getPassword())) {
            try (Statement stmt = conn.createStatement()) {
                // Grant USAGE on schema to user
                stmt.execute("GRANT USAGE ON SCHEMA " + schemaName + " TO " + username);
            }
        }
    }

    @When("I request the list of schemas for database {string}")
    public void iRequestTheListOfSchemasForDatabase(String dbName) {
        // If we are not logged in to the correct database, we need to log in to it.
        // This simulates selecting the database in the UI.
        // But our step definition for login already tries to log in to ANY available database.
        
        // Let's check if the current token works for this dbName. 
        // If not, try to get a new token for this specific dbName.
        
        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + testContext.getJwtToken())
                .when()
                .get("/api/databases/" + dbName + "/schemas");
        
        if (response.getStatusCode() == 403) {
             // Try to re-login to the specific database
             // We need credentials for this... which we might not have in the context easily
             // but we can assume them from the previous "I am logged in as..." step if we save them.
             // For now, let's see if we can just use the credentials we have.
             
             // Actually, the test should probably have a step "I select database {string}"
        }

        testContext.setLastResponse(response);
    }

    @And("the response should contain schema {string}")
    public void theResponseShouldContainSchema(String expectedSchema) {
        Response response = testContext.getLastResponse();
        List<String> schemas = response.jsonPath().getList("schemas", String.class);
        assertTrue(schemas.contains(expectedSchema), "Response does not contain schema " + expectedSchema);
    }

    @And("the response should not contain schema {string}")
    public void theResponseShouldNotContainSchema(String unexpectedSchema) {
        Response response = testContext.getLastResponse();
        List<String> schemas = response.jsonPath().getList("schemas", String.class);
        assertFalse(schemas.contains(unexpectedSchema), "Response contains schema " + unexpectedSchema);
    }

    @And("a table {string} exists in schema {string} in database {string}")
    public void aTableExistsInSchemaInDatabase(String tableName, String schemaName, String dbName) throws SQLException {
        PostgreSQLContainer<?> postgres = testContext.getPostgresContainer();
        String url = postgres.getJdbcUrl();
        String baseUrl = url.substring(0, url.lastIndexOf("/") + 1);
        String dbUrl = baseUrl + dbName;

        try (Connection conn = DriverManager.getConnection(dbUrl, postgres.getUsername(), postgres.getPassword())) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE " + schemaName + "." + tableName + " (id SERIAL PRIMARY KEY)");
            }
        }
    }

    @And("the user {string} has privilege to see the table {string} in schema {string} in database {string}")
    public void theUserHasPrivilegeToSeeTheTableInSchemaInDatabase(String username, String tableName, String schemaName, String dbName) throws SQLException {
        PostgreSQLContainer<?> postgres = testContext.getPostgresContainer();
        String url = postgres.getJdbcUrl();
        String baseUrl = url.substring(0, url.lastIndexOf("/") + 1);
        String dbUrl = baseUrl + dbName;

        try (Connection conn = DriverManager.getConnection(dbUrl, postgres.getUsername(), postgres.getPassword())) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("GRANT SELECT ON " + schemaName + "." + tableName + " TO " + username);
            }
        }
    }

    @When("I request the list of tables for schema {string} in database {string}")
    public void iRequestTheListOfTablesForSchemaInDatabase(String schemaName, String dbName) {
        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + testContext.getJwtToken())
                .when()
                .get("/api/databases/" + dbName + "/schemas/" + schemaName + "/tables");

        testContext.setLastResponse(response);
    }

    @And("the response should contain table {string}")
    public void theResponseShouldContainTable(String expectedTable) {
        Response response = testContext.getLastResponse();
        List<String> tables = response.jsonPath().getList("tables", String.class);
        assertTrue(tables.contains(expectedTable), "Response does not contain table " + expectedTable);
    }

    @And("the response should not contain table {string}")
    public void theResponseShouldNotContainTable(String unexpectedTable) {
        Response response = testContext.getLastResponse();
        List<String> tables = response.jsonPath().getList("tables", String.class);
        assertFalse(tables.contains(unexpectedTable), "Response contains table " + unexpectedTable);
    }
}
