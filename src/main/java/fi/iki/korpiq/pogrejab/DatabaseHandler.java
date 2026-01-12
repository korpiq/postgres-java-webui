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
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                DecodedJWT decodedJWT = jwtService.validateToken(token);
                String sessionId = decodedJWT.getClaim("sessionId").asString();
                String username = decodedJWT.getClaim("username").asString();

                Connection conn = loginHandler.getSessionConnections().get(sessionId);
                if (conn == null || conn.isClosed()) {
                    throw new UnauthorizedResponse("Session expired or invalid");
                }

                listDatabasesForUser(ctx, conn, username);
            } catch (Exception e) {
                ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of("error", "Invalid token or session: " + e.getMessage()));
            }
        } else {
            // Try with username/password from body (for initial login step)
            try {
                Map<String, String> body = ctx.bodyAsClass(Map.class);
                String username = body.get("username");
                String password = body.get("password");

                if (username == null || password == null) {
                    ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Username and password are required"));
                    return;
                }

                // Temporary connection to list databases
                String url = loginHandler.getDatabaseUrl();
                Connection conn = null;
                try {
                    // Try with 'postgres' database first as it usually exists and is accessible
                    String postgresUrl;
                    int lastSlashIndex = url.lastIndexOf("/");
                    int questionMarkIndex = url.indexOf("?", lastSlashIndex);
                    if (questionMarkIndex != -1) {
                        postgresUrl = url.substring(0, lastSlashIndex + 1) + "postgres" + url.substring(questionMarkIndex);
                    } else {
                        postgresUrl = url.substring(0, lastSlashIndex + 1) + "postgres";
                    }
                    conn = java.sql.DriverManager.getConnection(postgresUrl, username, password);
                } catch (Exception e) {
                    // Fallback to original URL
                    conn = java.sql.DriverManager.getConnection(url, username, password);
                }

                try (Connection finalConn = conn) {
                    listDatabasesForUser(ctx, finalConn, username);
                }
            } catch (Exception e) {
                ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of("error", "Authentication failed: " + e.getMessage()));
            }
        }
    }

    private void listDatabasesForUser(Context ctx, Connection conn, String username) throws java.sql.SQLException {
        List<String> databases = new ArrayList<>();
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

            // Verify the connection is to the correct database
            if (!dbName.equals(conn.getCatalog())) {
                ctx.status(HttpStatus.FORBIDDEN).json(Map.of("error", "Session is not associated with database " + dbName));
                return;
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

    public void handleListTables(Context ctx) {
        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedResponse("Missing or invalid Authorization header");
        }

        String dbName = ctx.pathParam("dbName");
        String schemaName = ctx.pathParam("schemaName");
        String token = authHeader.substring(7);
        try {
            DecodedJWT decodedJWT = jwtService.validateToken(token);
            String sessionId = decodedJWT.getClaim("sessionId").asString();
            String username = decodedJWT.getClaim("username").asString();

            Connection conn = loginHandler.getSessionConnections().get(sessionId);
            if (conn == null || conn.isClosed()) {
                throw new UnauthorizedResponse("Session expired or invalid");
            }

            // Ensure we are connected to the correct database
            if (!dbName.equals(conn.getCatalog())) {
                ctx.status(HttpStatus.FORBIDDEN).json(Map.of("error", "Session is not associated with database " + dbName));
                return;
            }

            List<String> tables = new ArrayList<>();
            // Query to list tables in the given schema that the user has SELECT privilege on
            // We use has_table_privilege()
            // In Postgres, tables are in pg_class, joined with pg_namespace for schema name.
            // Relkind 'r' is for ordinary tables.
            String query = "SELECT tablename FROM pg_tables WHERE schemaname = ? AND has_table_privilege(?, quote_ident(schemaname) || '.' || quote_ident(tablename), 'SELECT')";

            try (java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, schemaName);
                pstmt.setString(2, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        tables.add(rs.getString("tablename"));
                    }
                }
            }

            ctx.status(HttpStatus.OK).json(Map.of("tables", tables));
        } catch (Exception e) {
            ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of("error", "Invalid token or session: " + e.getMessage()));
        }
    }
}
