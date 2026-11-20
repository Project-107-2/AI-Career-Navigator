package org.career.navigator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {

    private Database() {
    }

    public static Connection connect() throws SQLException {
        String url = System.getenv().getOrDefault(
                "DB_URL", "jdbc:postgresql://localhost:5432/career_navigator");
        String user = System.getenv().getOrDefault("DB_USER", "career");
        String password = required("DB_PASSWORD");
        return DriverManager.getConnection(url, user, password);
    }

    public static String required(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Set environment variable: " + key);
        }
        return value;
    }
}
