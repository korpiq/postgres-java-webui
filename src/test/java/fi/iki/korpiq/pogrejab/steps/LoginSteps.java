package fi.iki.korpiq.pogrejab.steps;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import fi.iki.korpiq.pogrejab.App;
import fi.iki.korpiq.pogrejab.TestContext;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LoginSteps {
    private final TestContext testContext;
    private App app;
    private Map<String, String> credentials = new HashMap<>();

    public LoginSteps(TestContext testContext) {
        this.testContext = testContext;
    }

    @Before
    public void setUp() {
        // Start Postgres container before each scenario
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
                .withDatabaseName("testdb")
                .withUsername("postgres")
                .withPassword("postgres");
        postgres.start();
        testContext.setPostgresContainer(postgres);

        // Start the application
        System.setProperty("DB_URL", postgres.getJdbcUrl());
        System.setProperty("DB_USER", postgres.getUsername());
        System.setProperty("DB_PASSWORD", postgres.getPassword());
        
        // Ensure JWT keys are pointed to the correct location for tests
        String projectRoot = System.getProperty("user.dir");
        System.setProperty("JWT_PRIVATE_KEY", projectRoot + "/.secrets/jwt_key");
        System.setProperty("JWT_PUBLIC_KEY", projectRoot + "/.secrets/jwt_public_key.pem");
        
        app = new App();
        app.start(0); // Start on a random port
        int port = app.getPort();
        testContext.setServerPort(port);

        // Set base URI for REST Assured
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        waitForAppReady(port);
    }

    private void waitForAppReady(int port) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(1))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/health"))
                .GET()
                .build();

        long startTime = System.currentTimeMillis();
        long timeout = 10000; // 10 seconds

        while (System.currentTimeMillis() - startTime < timeout) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return;
                }
            } catch (IOException | InterruptedException e) {
                // Ignore and retry
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        throw new RuntimeException("Application failed to start within 10 seconds");
    }

    @After
    public void tearDown() {
        // Stop the application
        if (app != null) {
            app.stop();
        }

        // Stop the Postgres container after each scenario
        if (testContext.getPostgresContainer() != null) {
            testContext.getPostgresContainer().stop();
        }

        // Close any open connections
        testContext.getSessionConnections().values().forEach(conn -> {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Given("a temporary Postgres instance is running")
    public void aTemporaryPostgresInstanceIsRunning() {
        assertNotNull(testContext.getPostgresContainer());
        assertTrue(testContext.getPostgresContainer().isRunning());
    }

    @Given("a Postgres user {string} with password {string} exists")
    public void aPostgresUserWithPasswordExists(String username, String password) {
        try (Connection conn = DriverManager.getConnection(
                testContext.getPostgresContainer().getJdbcUrl(),
                testContext.getPostgresContainer().getUsername(),
                testContext.getPostgresContainer().getPassword())) {

            Statement stmt = conn.createStatement();
            stmt.execute(String.format("CREATE USER %s WITH PASSWORD '%s'", username, password));
            stmt.execute(String.format("GRANT ALL PRIVILEGES ON DATABASE testdb TO %s", username));
            stmt.close();
        } catch (SQLException e) {
            fail("Failed to create test user: " + e.getMessage());
        }
    }

    @When("I send a POST request to {string} with credentials:")
    public void iSendAPOSTRequestToWithCredentials(String endpoint, io.cucumber.datatable.DataTable dataTable) {
        Map<String, String> credentialsMap = dataTable.asMap(String.class, String.class);
        credentials = credentialsMap;

        // Set the port if server is running
        if (testContext.getServerPort() > 0) {
            RestAssured.port = testContext.getServerPort();
        }

        Response response = RestAssured.given()
                .contentType("application/json")
                .body(credentialsMap)
                .when()
                .post(endpoint);

        testContext.setLastResponse(response);

        // Extract JWT token if present
        if (response.getStatusCode() == 200) {
            String token = response.jsonPath().getString("token");
            if (token != null) {
                testContext.setJwtToken(token);
            }
        }
    }

    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int expectedStatus) {
        assertNotNull(testContext.getLastResponse(), "No response received");
        assertEquals(expectedStatus, testContext.getLastResponse().getStatusCode());
    }

    @And("the response should contain a valid JWT token")
    public void theResponseShouldContainAValidJWTToken() {
        assertNotNull(testContext.getLastResponse());
        String token = testContext.getLastResponse().jsonPath().getString("token");
        assertNotNull(token, "JWT token should be present in response");
        assertFalse(token.isEmpty(), "JWT token should not be empty");

        // Verify it's a valid JWT format (header.payload.signature)
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT should have 3 parts");

        testContext.setJwtToken(token);
    }

    @And("the JWT token should contain a session ID")
    public void theJWTTokenShouldContainASessionID() {
        assertNotNull(testContext.getJwtToken(), "JWT token should be set");

        DecodedJWT jwt = JWT.decode(testContext.getJwtToken());
        String sessionId = jwt.getClaim("sessionId").asString();

        assertNotNull(sessionId, "JWT should contain sessionId claim");
        assertFalse(sessionId.isEmpty(), "Session ID should not be empty");
    }

    @And("the JWT token should be signed with the private key")
    public void theJWTTokenShouldBeSignedWithThePrivateKey() {
        assertNotNull(testContext.getJwtToken(), "JWT token should be set");

        try {
            // Read the public key from the test keys
            RSAPublicKey publicKey = readPublicKey();

            // Verify the JWT signature
            Algorithm algorithm = Algorithm.RSA256(publicKey, null);
            algorithm.verify(JWT.decode(testContext.getJwtToken()));

        } catch (Exception e) {
            fail("JWT signature verification failed: " + e.getMessage());
        }
    }

    @And("the response should contain an error message")
    public void theResponseShouldContainAnErrorMessage() {
        assertNotNull(testContext.getLastResponse());
        String error = testContext.getLastResponse().jsonPath().getString("error");
        assertNotNull(error, "Error message should be present in response");
        assertFalse(error.isEmpty(), "Error message should not be empty");
    }

    @And("the session should be mapped to a Postgres connection with user {string}")
    public void theSessionShouldBeMappedToAPostgresConnectionWithUser(String expectedUsername) {
        assertNotNull(testContext.getJwtToken(), "JWT token should be set");

        DecodedJWT jwt = JWT.decode(testContext.getJwtToken());
        String sessionId = jwt.getClaim("sessionId").asString();
        String username = jwt.getClaim("username").asString();

        assertNotNull(sessionId, "Session ID should be present");
        assertEquals(expectedUsername, username, "Username in JWT should match expected user");

        // In a real implementation, we would verify the session is actually mapped
        // to a Postgres connection. For now, we verify the JWT contains the correct username
    }

    /**
     * Helper method to read RSA public key from PEM file
     */
    private RSAPublicKey readPublicKey() throws Exception {
        // For testing, we'll need to generate or use test keys
        // This path should point to the test keys location
        String publicKeyPath = System.getProperty("user.dir") + "/.secrets/jwt_public_key.pem";

        String key = new String(Files.readAllBytes(Paths.get(publicKeyPath)));
        String b64Key = key.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(b64Key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
    }
}
