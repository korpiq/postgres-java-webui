package fi.iki.korpiq.pogrejab;

import io.restassured.response.Response;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared test context for Cucumber scenarios
 */
public class TestContext {
    private PostgreSQLContainer<?> postgresContainer;
    private Response lastResponse;
    private String jwtToken;
    private Map<String, Connection> sessionConnections = new HashMap<>();
    private int serverPort;

    public PostgreSQLContainer<?> getPostgresContainer() {
        return postgresContainer;
    }

    public void setPostgresContainer(PostgreSQLContainer<?> postgresContainer) {
        this.postgresContainer = postgresContainer;
    }

    public Response getLastResponse() {
        return lastResponse;
    }

    public void setLastResponse(Response lastResponse) {
        this.lastResponse = lastResponse;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public Map<String, Connection> getSessionConnections() {
        return sessionConnections;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }
}