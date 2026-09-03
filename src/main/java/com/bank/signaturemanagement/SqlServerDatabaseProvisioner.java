package com.bank.signaturemanagement;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Creates only the configured SQL Server database; Flyway owns its schema. */
final class SqlServerDatabaseProvisioner {
    private static final Pattern DATABASE_NAME =
            Pattern.compile("(?i)([;:]databaseName=)([^;]+)");
    private static final Pattern SAFE_DATABASE_NAME = Pattern.compile("[A-Za-z0-9_-]{1,128}");

    private SqlServerDatabaseProvisioner() {
    }

    static void createConfiguredDatabaseIfMissing(String[] args) {
        Properties properties = loadApplicationProperties();
        if (!Boolean.parseBoolean(resolve(args, properties, "app.database.auto-create", "DB_AUTO_CREATE", "true"))) {
            return;
        }

        String url = resolve(args, properties, "spring.datasource.url", "DB_URL", null);
        if (url == null || !url.startsWith("jdbc:sqlserver:")) return;

        Matcher matcher = DATABASE_NAME.matcher(url);
        if (!matcher.find()) return;
        String databaseName = matcher.group(2).trim();
        if (!SAFE_DATABASE_NAME.matcher(databaseName).matches()) {
            throw new IllegalStateException("Unsafe SQL Server database name: " + databaseName);
        }

        String masterUrl = matcher.replaceFirst(Matcher.quoteReplacement(matcher.group(1) + "master"));
        String username = resolve(args, properties, "spring.datasource.username", "DB_USERNAME", null);
        String password = resolve(args, properties, "spring.datasource.password", "DB_PASSWORD", null);

        try (Connection connection = DriverManager.getConnection(masterUrl, username, password);
             PreparedStatement exists = connection.prepareStatement("SELECT 1 FROM sys.databases WHERE name = ?")) {
            exists.setString(1, databaseName);
            try (ResultSet result = exists.executeQuery()) {
                if (result.next()) return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE DATABASE [" + databaseName + "]");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not create SQL Server database '" + databaseName + "'", exception);
        }
    }

    private static Properties loadApplicationProperties() {
        Properties properties = new Properties();
        try (InputStream input = SqlServerDatabaseProvisioner.class.getResourceAsStream("/application.properties")) {
            if (input != null) properties.load(input);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read application.properties", exception);
        }
    }

    private static String resolve(String[] args, Properties properties, String property, String environment,
                                  String fallback) {
        String prefix = "--" + property + "=";
        String commandLine = Arrays.stream(args).filter(arg -> arg.startsWith(prefix))
                .map(arg -> arg.substring(prefix.length())).reduce((first, second) -> second).orElse(null);
        if (commandLine != null) return commandLine;
        String system = System.getProperty(property);
        if (system != null && !system.isBlank()) return system;
        String env = System.getenv(environment);
        if (env != null && !env.isBlank()) return env;
        String configured = properties.getProperty(property);
        if (configured == null) return fallback;
        return unwrapDefaultPlaceholder(configured);
    }

    private static String unwrapDefaultPlaceholder(String value) {
        Matcher placeholder = Pattern.compile("^\\$\\{[^:}]+:(.*)}$").matcher(value);
        return placeholder.matches() ? placeholder.group(1) : value;
    }
}
