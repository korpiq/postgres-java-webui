package fi.iki.korpiq.pogrejab;

public class GenerateInvalidJwt {
    public static void main(String[] args) {
        String privateKeyPath = "/home/kato/postgres-java-webui/.secrets/jwt_key";
        String publicKeyPath = "/home/kato/postgres-java-webui/.secrets/jwt_public_key.pem";
        long expirationMs = 3600000;

        JwtService jwtService = new JwtService(privateKeyPath, publicKeyPath, expirationMs);
        String token = jwtService.generateToken("testuser", "fake-session-id");
        System.out.println(token);
    }
}
