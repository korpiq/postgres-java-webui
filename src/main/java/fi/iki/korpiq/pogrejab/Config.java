package fi.iki.korpiq.pogrejab;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Application configuration loader
 */
public class Config {
    private final Properties properties;

    public Config() {
        this.properties = new Properties();
        loadProperties();
    }

    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find application.properties");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Error loading configuration", e);
        }
    }

    public String getDatabaseUrl() {
        return properties.getProperty("db.url");
    }

    public String getDatabaseUsername() {
        return properties.getProperty("db.username");
    }

    public String getDatabasePassword() {
        return properties.getProperty("db.password");
    }

    public String getJwtPrivateKeyPath() {
        return properties.getProperty("jwt.privateKey");
    }

    public String getJwtPublicKeyPath() {
        return properties.getProperty("jwt.publicKey");
    }

    public long getJwtExpirationMs() {
        return Long.parseLong(properties.getProperty("jwt.expirationMs", "3600000"));
    }

    public int getServerPort() {
        return Integer.parseInt(properties.getProperty("server.port", "8080"));
    }
}
