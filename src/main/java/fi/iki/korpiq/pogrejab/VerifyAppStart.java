package fi.iki.korpiq.pogrejab;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class VerifyAppStart {
    public static void main(String[] args) throws Exception {
        String projectRoot = System.getProperty("user.dir");
        // We need to provide dummy or real values for config
        System.setProperty("DB_URL", "jdbc:postgresql://localhost:5432/testdb");
        System.setProperty("DB_USER", "postgres");
        System.setProperty("DB_PASSWORD", "postgres");
        
        // Use existing keys from .secrets/ if they exist
        System.setProperty("JWT_PRIVATE_KEY", projectRoot + "/.secrets/jwt_key");
        System.setProperty("JWT_PUBLIC_KEY", projectRoot + "/.secrets/jwt_public_key.pem");
        
        App app = new App();
        int port = 9090;
        System.out.println("Starting app on port " + port);
        app.start(port);
        
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/health"))
                .GET()
                .build();
        
        try {
            System.out.println("Sending health check request...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response status: " + response.statusCode());
            System.out.println("Response body: " + response.body());
            
            if (response.statusCode() == 200 && "OK".equals(response.body())) {
                System.out.println("SUCCESS: App is accepting connections!");
            } else {
                System.out.println("FAILURE: Unexpected response");
            }
        } catch (Exception e) {
            System.out.println("FAILURE: Could not connect to app: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("Stopping app...");
            app.stop();
        }
    }
}
