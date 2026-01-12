package fi.iki.korpiq.pogrejab;

import io.javalin.Javalin;

/**
 * Main application class
 */
public class App {
    private final Config config;
    private final JwtService jwtService;
    private final LoginHandler loginHandler;
    private final DatabaseHandler databaseHandler;
    private Javalin app;

    public App() {
        this.config = new Config();
        this.jwtService = new JwtService(
                config.getJwtPrivateKeyPath(),
                config.getJwtPublicKeyPath(),
                config.getJwtExpirationMs()
        );
        this.loginHandler = new LoginHandler(jwtService, config.getDatabaseUrl());
        this.databaseHandler = new DatabaseHandler(jwtService, loginHandler);
    }

    public App(Config config, JwtService jwtService, LoginHandler loginHandler) {
        this.config = config;
        this.jwtService = jwtService;
        this.loginHandler = loginHandler;
        this.databaseHandler = new DatabaseHandler(jwtService, loginHandler);
    }

    public void start() {
        start(config.getServerPort());
    }

    public void start(int port) {
        app = Javalin.create(javalinConfig -> {
            javalinConfig.showJavalinBanner = false;
        }).start(port);

        // Register routes
        app.post("/api/login", loginHandler::handleLogin);
        app.get("/api/databases", databaseHandler::handleListDatabases);
        app.get("/api/databases/{dbName}/schemas", databaseHandler::handleListSchemas);
        app.get("/api/databases/{dbName}/schemas/{schemaName}/tables", databaseHandler::handleListTables);

        // Health check endpoint
        app.get("/health", ctx -> ctx.result("OK"));
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }

    public int getPort() {
        return app != null ? app.port() : -1;
    }

    public LoginHandler getLoginHandler() {
        return loginHandler;
    }

    public static void main(String[] args) {
        App app = new App();
        app.start();
        System.out.println("Server started on port " + app.getPort());
    }
}
