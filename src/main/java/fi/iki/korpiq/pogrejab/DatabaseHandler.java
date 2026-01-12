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
}
