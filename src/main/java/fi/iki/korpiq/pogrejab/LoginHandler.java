package fi.iki.korpiq.pogrejab;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles login requests and manages sessions
 */
public class LoginHandler {
    private final JwtService jwtService;
    private final String databaseUrl;
    private final Map<String, Connection> sessionConnections;

    public LoginHandler(JwtService jwtService, String databaseUrl) {
        this.jwtService = jwtService;
        this.databaseUrl = databaseUrl;
        this.sessionConnections = new ConcurrentHashMap<>();
    }

    public void handleLogin(Context ctx) {
        try {
            Map<String, String> credentials = ctx.bodyAsClass(Map.class);
            String username = credentials.get("username");
            String password = credentials.get("password");

            if (username == null || password == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("error", "Username and password are required"));
                return;
            }

            // Attempt to connect to PostgreSQL with the provided credentials
            Connection connection = authenticateAndConnect(username, password);

            if (connection != null) {
                // Generate session ID and JWT token
                String sessionId = jwtService.generateSessionId();
                String token = jwtService.generateToken(username, sessionId);

                // Store the connection mapped to the session
                sessionConnections.put(sessionId, connection);

                ctx.status(HttpStatus.OK)
                   .json(Map.of("token", token));
            } else {
                ctx.status(HttpStatus.UNAUTHORIZED)
                   .json(Map.of("error", "Invalid credentials"));
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.UNAUTHORIZED)
               .json(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }

    private Connection authenticateAndConnect(String username, String password) {
        try {
            Connection connection = DriverManager.getConnection(databaseUrl, username, password);
            // Test the connection
            if (connection.isValid(5)) {
                return connection;
            } else {
                connection.close();
                return null;
            }
        } catch (SQLException e) {
            // Invalid credentials or connection failed
            return null;
        }
    }

    public Map<String, Connection> getSessionConnections() {
        return sessionConnections;
    }
}
