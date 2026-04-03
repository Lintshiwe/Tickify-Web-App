package za.ac.tut.databaseConnection;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

public class DatabaseConnection {
    
    private static final String DB_HOST     = resolve("tickify.db.host", "TICKIFY_DB_HOST", "localhost");
    private static final String DB_PORT     = resolve("tickify.db.port", "TICKIFY_DB_PORT", "1527");
    private static final String DB_NAME     = resolve("tickify.db.name", "TICKIFY_DB_NAME", "tickifyDB");
    private static final String DB_MODE     = resolve("tickify.db.mode", "TICKIFY_DB_MODE", "auto");
    private static final String DB_USER     = resolveRequired("tickify.db.user", "TICKIFY_DB_USER");
    private static final String DB_PASSWORD = resolveRequired("tickify.db.password", "TICKIFY_DB_PASSWORD");

    private static final String CLIENT_DRIVER_CLASS = "org.apache.derby.jdbc.ClientDriver";
    private static final String EMBEDDED_DRIVER_CLASS = "org.apache.derby.jdbc.EmbeddedDriver";
    private static final String EMBEDDED_AUTOLOAD_DRIVER_CLASS = "org.apache.derby.iapi.jdbc.AutoloadedDriver";
    private static final String CLIENT_JDBC_URL = "jdbc:derby://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + ";create=true;ssl=off";
    private static final String EMBEDDED_JDBC_URL = "jdbc:derby:" + DB_NAME + ";create=true";

    static {
        try {
            Class.forName(CLIENT_DRIVER_CLASS).newInstance();
            Class.forName(EMBEDDED_DRIVER_CLASS).newInstance();
            System.out.println("Tickify: Derby drivers loaded (client + embedded).");
        } catch (Exception e) {
            System.err.println("Tickify Error: Driver check failed: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        String mode = DB_MODE == null ? "auto" : DB_MODE.trim().toLowerCase();
        if ("embedded".equals(mode)) {
            return connectEmbedded();
        }
        if ("client".equals(mode)) {
            return connectClient();
        }

        // Auto mode: prefer client/server Derby, fallback to embedded when server is down.
        try {
            return connectClient();
        } catch (SQLException e) {
            if (isConnectivityFailure(e)) {
                System.err.println("Tickify: Derby client connection unavailable, falling back to embedded mode.");
                return connectEmbedded();
            }
            throw e;
        }
    }

    private static Connection connectClient() throws SQLException {
        ensureClientDriverRegistered();
        try {
            return DriverManager.getConnection(CLIENT_JDBC_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            System.err.println("Tickify SQL Error (client): " + e.getMessage() + " | State: " + e.getSQLState());
            throw e;
        }
    }

    private static Connection connectEmbedded() throws SQLException {
        ensureEmbeddedDriverRegistered();
        try {
            return DriverManager.getConnection(EMBEDDED_JDBC_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            System.err.println("Tickify SQL Error (embedded): " + e.getMessage() + " | State: " + e.getSQLState());
            throw e;
        }
    }

    private static void ensureEmbeddedDriverRegistered() throws SQLException {
        boolean driverFound = false;
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            String name = drivers.nextElement().getClass().getName();
            if (EMBEDDED_DRIVER_CLASS.equals(name) || EMBEDDED_AUTOLOAD_DRIVER_CLASS.equals(name)) {
                driverFound = true;
                break;
            }
        }

        if (driverFound) {
            return;
        }

        try {
            Class.forName(EMBEDDED_DRIVER_CLASS);
            return;
        } catch (ClassNotFoundException ignored) {
            // Fall through to Derby versions that expose the autoloaded driver only.
        }

        try {
            Class.forName(EMBEDDED_AUTOLOAD_DRIVER_CLASS);
        } catch (ClassNotFoundException ex) {
            throw new SQLException("Unable to load Derby embedded driver", ex);
        }
    }

    private static void ensureClientDriverRegistered() throws SQLException {
        boolean driverFound = false;
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            if (drivers.nextElement().getClass().getName().equals(CLIENT_DRIVER_CLASS)) {
                driverFound = true;
                break;
            }
        }

        if (!driverFound) {
            DriverManager.registerDriver(new org.apache.derby.jdbc.ClientDriver());
        }
    }

    private static boolean isConnectivityFailure(SQLException e) {
        String state = e.getSQLState();
        if (state != null && state.startsWith("08")) {
            return true;
        }
        String message = e.getMessage();
        return message != null && message.toLowerCase().contains("connection refused");
    }

    private static String resolve(String propertyKey, String envKey, String fallback) {
        String prop = System.getProperty(propertyKey);
        if (prop != null && !prop.trim().isEmpty()) {
            return prop.trim();
        }

        String env = System.getenv(envKey);
        if (env != null && !env.trim().isEmpty()) {
            return env.trim();
        }

        return fallback;
    }

    private static String resolveRequired(String propertyKey, String envKey) {
        String prop = System.getProperty(propertyKey);
        if (prop != null && !prop.trim().isEmpty()) {
            return prop.trim();
        }

        String env = System.getenv(envKey);
        if (env != null && !env.trim().isEmpty()) {
            return env.trim();
        }

        throw new IllegalStateException(
                "Missing required database credential. Set JVM property '"
                + propertyKey + "' or environment variable '" + envKey + "'.");
    }

    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }
}