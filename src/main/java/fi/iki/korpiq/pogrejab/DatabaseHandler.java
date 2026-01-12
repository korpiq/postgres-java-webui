package fi.iki.korpiq.pogrejab;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.UnauthorizedResponse;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DatabaseHandler {
    private final JwtService jwtService;
    private final LoginHandler loginHandler;

    public DatabaseHandler(JwtService jwtService, LoginHandler loginHandler) {
        this.jwtService = jwtService;
        this.loginHandler = loginHandler;
    }

    public void handleListDatabases(Context ctx) {
        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedResponse("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        try {
            DecodedJWT decodedJWT = jwtService.validateToken(token);
            String sessionId = decodedJWT.getClaim("sessionId").asString();
            String username = decodedJWT.getClaim("username").asString();

            Connection conn = loginHandler.getSessionConnections().get(sessionId);
            if (conn == null || conn.isClosed()) {
                throw new UnauthorizedResponse("Session expired or invalid");
            }

            List<String> databases = new ArrayList<>();
            // Query to list databases that the user has CONNECT privilege on
            // Note: In Postgres, all users can see all databases in pg_database by default.
            // The requirement says "test should ensure that user sees database only when given privilege for that."
            // We can use has_database_privilege() function.
            String query = "SELECT datname FROM pg_database WHERE datistemplate = false AND has_database_privilege(?, datname, 'CONNECT')";
            
            try (java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        databases.add(rs.getString("datname"));
                    }
                }
            }

            ctx.status(HttpStatus.OK).json(Map.of("databases", databases));
        } catch (Exception e) {
            ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of("error", "Invalid token or session: " + e.getMessage()));
        }
    }

    public void handleListSchemas(Context ctx) {
        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedResponse("Missing or invalid Authorization header");
        }

        String dbName = ctx.pathParam("dbName");
        String token = authHeader.substring(7);
        try {
            DecodedJWT decodedJWT = jwtService.validateToken(token);
            String sessionId = decodedJWT.getClaim("sessionId").asString();
            String username = decodedJWT.getClaim("username").asString();

            Connection conn = loginHandler.getSessionConnections().get(sessionId);
            if (conn == null || conn.isClosed()) {
                throw new UnauthorizedResponse("Session expired or invalid");
            }

            // Since a JDBC connection is to a specific database, we can only list schemas of the current database.
            // If the requested dbName is NOT the one we are connected to, we need to reconnect.
            if (!dbName.equals(conn.getCatalog())) {
                String password = loginHandler.getSessionPasswords().get(sessionId);
                if (password == null) {
                    throw new UnauthorizedResponse("Session password not found, please login again");
                }
                
                String baseUrl = loginHandler.getDatabaseUrl();
                // Replace the database part of the URL. 
                // e.g., jdbc:postgresql://localhost:5432/postgres -> jdbc:postgresql://localhost:5432/dbName
                String newUrl = baseUrl.substring(0, baseUrl.lastIndexOf("/") + 1) + dbName;
                
                try {
                    Connection newConn = java.sql.DriverManager.getConnection(newUrl, username, password);
                    conn.close();
                    loginHandler.getSessionConnections().put(sessionId, newConn);
                    conn = newConn;
                } catch (java.sql.SQLException e) {
                    ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Failed to connect to database " + dbName + ": " + e.getMessage()));
                    return;
                }
            }

            List<String> schemas = new ArrayList<>();
            // Query to list schemas that the user has USAGE privilege on
            // We use has_schema_privilege()
            String query = "SELECT nspname FROM pg_namespace WHERE has_schema_privilege(?, nspname, 'USAGE')";
            
            try (java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        schemas.add(rs.getString("nspname"));
                    }
                }
            }

            ctx.status(HttpStatus.OK).json(Map.of("schemas", schemas));
        } catch (Exception e) {
            ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of("error", "Invalid token or session: " + e.getMessage()));
        }
    }
}
