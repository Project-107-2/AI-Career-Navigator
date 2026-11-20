package org.career.navigator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Единая точка подключения к PostgreSQL (совместима с конфигом и docker-compose.yml Агаси).
 * Читает переменные окружения DB_URL / DB_USER / DB_PASSWORD, а при локальном запуске из IDEA
 * автоматически подхватывает значения из файла .env в корне проекта.
 */
public final class Database {

    private static volatile Map<String, String> dotEnvCache;

    private Database() {
    }

    public static Connection connect() throws SQLException {
        String url = resolveUrl();
        String user = resolve("DB_USER", "POSTGRES_USER", "career");
        String password = resolve("DB_PASSWORD", "POSTGRES_PASSWORD", null);
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Set environment variable DB_PASSWORD (or POSTGRES_PASSWORD in .env)");
        }
        return DriverManager.getConnection(url, user, password);
    }

    public static String required(String key) {
        String value = lookup(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Set environment variable: " + key);
        }
        return value;
    }

    private static String resolveUrl() {
        String explicit = lookup("DB_URL");
        if (explicit != null && !explicit.isBlank()) {
            return explicit;
        }
        String port = resolve("DB_PORT", null, "5432");
        String dbName = resolve("POSTGRES_DB", null, "career_navigator");
        return "jdbc:postgresql://localhost:" + port + "/" + dbName;
    }

    private static String resolve(String primaryKey, String fallbackKey, String defaultValue) {
        String value = lookup(primaryKey);
        if (value != null && !value.isBlank()) {
            return value;
        }
        if (fallbackKey != null) {
            String fallback = lookup(fallbackKey);
            if (fallback != null && !fallback.isBlank()) {
                return fallback;
            }
        }
        return defaultValue;
    }

    private static String lookup(String key) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return loadDotEnv().get(key);
    }

    private static Map<String, String> loadDotEnv() {
        if (dotEnvCache != null) {
            return dotEnvCache;
        }
        Path envFile = Path.of(".env");
        if (!Files.isRegularFile(envFile)) {
            dotEnvCache = Collections.emptyMap();
            return dotEnvCache;
        }
        Map<String, String> parsed = new HashMap<>();
        try {
            for (String rawLine : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String k = line.substring(0, eq).trim();
                String v = line.substring(eq + 1).trim();
                if ((v.startsWith(""") && v.endsWith("""))
                        || (v.startsWith("'") && v.endsWith("'"))) {
                    v = v.substring(1, v.length() - 1);
                }
                parsed.put(k, v);
            }
        } catch (IOException ignored) {
            // Если .env недоступен для чтения, используем только System.getenv
        }
        dotEnvCache = Collections.unmodifiableMap(parsed);
        return dotEnvCache;
    }
}
