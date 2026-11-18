package org.career.navigator.repository;

import org.career.navigator.model.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Хранит пользователей в PostgreSQL (таблица users, см. db/schema.sql). */
public class PostgresUserRepository implements UserRepository {

    private final String url;
    private final String user;
    private final String password;

    public PostgresUserRepository(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Новый email — вставка, существующий — обновление (нужно для PUT /profile). */
    @Override
    public void save(User u) {
        String sql = """
                INSERT INTO users (name, email, password_hash, grade, target_role)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (email) DO UPDATE SET
                    name = EXCLUDED.name,
                    password_hash = EXCLUDED.password_hash,
                    grade = EXCLUDED.grade,
                    target_role = EXCLUDED.target_role
                """;
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.name);
            ps.setString(2, u.email);
            ps.setString(3, u.passwordHash);
            ps.setString(4, u.grade == null ? "" : u.grade);
            ps.setString(5, u.targetRole == null ? "" : u.targetRole);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось сохранить пользователя", e);
        }
    }

    @Override
    public User findByEmail(String email) {
        String sql = "SELECT name, email, password_hash, grade, target_role FROM users WHERE email = ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                User u = new User(rs.getString("name"), rs.getString("email"), rs.getString("password_hash"));
                u.grade = rs.getString("grade");
                u.targetRole = rs.getString("target_role");
                return u;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось найти пользователя", e);
        }
    }
}
