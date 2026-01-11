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
            }
        }
    }

    @And("I am logged in as {string} with password {string}")
    public void iAmLoggedInAsWithPassword(String username, String password) {
        Response response = RestAssured.given()
                .contentType("application/json")
                .body(Map.of("username", username, "password", password))
                .when()
                .post("/api/login");
        
        assertEquals(200, response.getStatusCode(), "Login failed for " + username);
        String token = response.jsonPath().getString("token");
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
}
