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
            String dbName = credentials.get("dbName");

            if (username == null || password == null || dbName == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("error", "Username, password and database name are required"));
                return;
            }

            // Attempt to connect to PostgreSQL with the provided credentials and specific database
            Connection connection = authenticateAndConnect(username, password, dbName);

            if (connection != null) {
                System.out.println("[DEBUG_LOG] Authentication successful for " + username + " on " + dbName);
                // Generate session ID and JWT token
                String sessionId = jwtService.generateSessionId();
                String token = jwtService.generateToken(username, sessionId);

                // Store the connection mapped to the session
                sessionConnections.put(sessionId, connection);

                // Set database-specific cookie with path / so it's visible everywhere
                io.javalin.http.Cookie cookie = new io.javalin.http.Cookie("pogrejab_" + dbName, token);
                cookie.setPath("/");
                cookie.setMaxAge(3600);
                cookie.setHttpOnly(false);
                ctx.cookie(cookie);

                ctx.status(HttpStatus.OK)
                   .json(Map.of("token", token, "redirect", "/db/" + dbName));
            } else {
                System.out.println("[DEBUG_LOG] Authentication failed for " + username + " on " + dbName);
                ctx.status(HttpStatus.UNAUTHORIZED)
                   .json(Map.of("error", "Invalid credentials or database access denied"));
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.UNAUTHORIZED)
               .json(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }

    private Connection authenticateAndConnect(String username, String password, String dbName) {
        try {
            String url = databaseUrl;
            // Handle JDBC URLs with parameters, e.g., jdbc:postgresql://host:port/dbname?param=value
            int lastSlashIndex = url.lastIndexOf("/");
            int questionMarkIndex = url.indexOf("?", lastSlashIndex);
            
            if (questionMarkIndex != -1) {
                url = url.substring(0, lastSlashIndex + 1) + dbName + url.substring(questionMarkIndex);
            } else {
                url = url.substring(0, lastSlashIndex + 1) + dbName;
            }
            
            Connection connection = DriverManager.getConnection(url, username, password);
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

    public String getDatabaseUrl() {
        return databaseUrl;
    }
}
