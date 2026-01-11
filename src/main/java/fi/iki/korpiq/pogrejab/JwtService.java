package fi.iki.korpiq.pogrejab;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Service for creating and validating JWT tokens
 */
public class JwtService {
    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final long expirationMs;
    private final Algorithm algorithm;

    public JwtService(String privateKeyPath, String publicKeyPath, long expirationMs) {
        try {
            this.privateKey = loadPrivateKey(privateKeyPath);
            this.publicKey = loadPublicKey(publicKeyPath);
            this.expirationMs = expirationMs;
            this.algorithm = Algorithm.RSA256(publicKey, privateKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JWT service", e);
        }
    }

    public String generateToken(String username, String sessionId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return JWT.create()
                .withClaim("username", username)
                .withClaim("sessionId", sessionId)
                .withIssuedAt(now)
                .withExpiresAt(expiration)
                .sign(algorithm);
    }

    public String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    private RSAPrivateKey loadPrivateKey(String path) throws Exception {
        String key = new String(Files.readAllBytes(Paths.get(path)));

        // Remove PEM headers if present
        String b64Key = key.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(b64Key);
        
        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) kf.generatePrivate(spec);
        } catch (Exception e) {
            // If PKCS#8 fails, try PKCS#1
            if (key.contains("BEGIN RSA PRIVATE KEY")) {
                return decodePKCS1PrivateKey(keyBytes);
            }
            throw e;
        }
    }

    private RSAPrivateKey decodePKCS1PrivateKey(byte[] pkcs1Bytes) throws Exception {
        // Simple PKCS#1 to PKCS#8 conversion for RSA
        // This is a minimal implementation, in a real app you might use BouncyCastle
        // But since we want to avoid extra dependencies if possible, we can try to wrap it
        // Or just use the fact that ssh-keygen can produce PKCS#8 if told so.
        // Let's try to just use ssh-keygen to convert it to PKCS#8 in the setup script instead.
        throw new RuntimeException("Private key is in PKCS#1 format. Please convert to PKCS#8 using: openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in key.pem -out key.pkcs8");
    }

    private RSAPublicKey loadPublicKey(String path) throws Exception {
        String key = new String(Files.readAllBytes(Paths.get(path)));

        // Remove PEM headers if present
        String b64Key = key.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(b64Key);
        
        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(spec);
        } catch (Exception e) {
             if (key.contains("BEGIN RSA PUBLIC KEY")) {
                 // This is likely PKCS#1 public key, Java expects X.509 (SubjectPublicKeyInfo)
                 throw new RuntimeException("Public key is in PKCS#1 format. Please convert to X.509 using: openssl rsa -in key.pem -pubout -out key.x509.pem");
             }
             throw e;
        }
    }
}
