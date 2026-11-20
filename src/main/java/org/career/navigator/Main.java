package org.career.navigator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.mindrot.jbcrypt.BCrypt;
import spark.Request;
import spark.Response;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

public class Main {

    private static final JWTService jwtService = new JWTService();
    private static final String PEPPER = Database.required("PASSWORD_PEPPER");
    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    public static void main(String[] args) {
        try (Connection connection = Database.connect()) {
            if (!connection.isValid(3)) {
                throw new IllegalStateException("PostgreSQL connection failed");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot connect to PostgreSQL", exception);
        }

        port(4567);

        before((request, response) -> {
            response.type("application/json");
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        });

        options("/*", (request, response) -> {
            response.status(200);
            return "";
        });

        post("/auth/register", (request, response) -> {
            RegisterRequest data = parse(request, RegisterRequest.class);
            if (data == null || data.name == null || data.name.isBlank()
                    || data.email == null || data.email.isBlank()
                    || data.password == null || data.password.length() < 8) {
                response.status(400);
                return error("INVALID_REGISTRATION_DATA", "Invalid registration data");
            }

            String email = data.email.trim().toLowerCase();
            String sql = "INSERT INTO users (name, email, password_hash) VALUES (?, ?, ?)";
            try (Connection connection = Database.connect()) {
                if (findUserId(connection, email) != -1) {
                    response.status(409);
                    return error("USER_ALREADY_EXISTS", "User already exists");
                }
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, data.name);
                    statement.setString(2, email);
                    statement.setString(3, hashPassword(data.password));
                    statement.executeUpdate();
                }
                response.status(201);
                return gson.toJson(Map.of("message", "Registration successful"));
            } catch (SQLException exception) {
                if ("23505".equals(exception.getSQLState())) {
                    response.status(409);
                    return error("USER_ALREADY_EXISTS", "User already exists");
                }
                return databaseError(response);
            }
        });

        post("/auth/login", (request, response) -> {
            LoginRequest data = parse(request, LoginRequest.class);
            if (data == null || data.email == null || data.email.isBlank()
                    || data.password == null || data.password.isBlank()) {
                response.status(400);
                return error("INVALID_LOGIN_DATA", "Invalid login data");
            }

            String email = data.email.trim().toLowerCase();
            String sql = "SELECT password_hash FROM users WHERE email = ?";
            try (Connection connection = Database.connect();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, email);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next() || !checkPassword(
                            data.password, result.getString("password_hash"))) {
                        response.status(401);
                        return error("INVALID_CREDENTIALS", "Invalid email or password");
                    }
                }
                String token = jwtService.createToken(email);
                return gson.toJson(Map.of("message", "Login successful", "token", token));
            } catch (SQLException exception) {
                return databaseError(response);
            }
        });

        before("/profile", (request, response) -> {
            if (!request.requestMethod().equals("OPTIONS")) {
                authorize(request);
            }
        });
        before("/skills/assessment", (request, response) -> {
            if (!request.requestMethod().equals("OPTIONS")) {
                authorize(request);
            }
        });

        get("/profile", (request, response) -> {
            String email = request.attribute("email");
            String sql = "SELECT u.name, u.email, u.grade, u.target_role_id, "
                    + "r.name AS target_role_name FROM users u LEFT JOIN roles r "
                    + "ON r.id = u.target_role_id WHERE u.email = ?";
            try (Connection connection = Database.connect();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, email);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        response.status(404);
                        return error("USER_NOT_FOUND", "User not found");
                    }
                    Map<String, Object> profile = new HashMap<>();
                    profile.put("name", result.getString("name"));
                    profile.put("email", result.getString("email"));
                    profile.put("grade", result.getString("grade"));
                    profile.put("targetRoleId", result.getObject("target_role_id"));
                    profile.put("targetRoleName", result.getString("target_role_name"));
                    return gson.toJson(profile);
                }
            } catch (SQLException exception) {
                return databaseError(response);
            }
        });

        put("/profile", (request, response) -> {
            JsonObject data = parse(request, JsonObject.class);
            if (data == null || (!data.has("grade") && !data.has("targetRoleId"))) {
                response.status(400);
                return error("INVALID_PROFILE_DATA", "Grade or targetRoleId is required");
            }
            try (Connection connection = Database.connect()) {
                if (data.has("grade")) {
                    if (!data.get("grade").isJsonPrimitive()
                            || !data.get("grade").getAsJsonPrimitive().isString()) {
                        response.status(400);
                        return error("INVALID_PROFILE_DATA", "Grade must be a string");
                    }
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE users SET grade = ? WHERE email = ?")) {
                        statement.setString(1, data.get("grade").getAsString());
                        statement.setString(2, request.attribute("email"));
                        statement.executeUpdate();
                    }
                }
                if (data.has("targetRoleId")) {
                    Integer roleId = null;
                    if (!data.get("targetRoleId").isJsonNull()) {
                        try {
                            roleId = Integer.valueOf(data.get("targetRoleId").getAsString());
                        } catch (NumberFormatException | IllegalStateException exception) {
                            response.status(400);
                            return error("INVALID_PROFILE_DATA", "targetRoleId must be an integer");
                        }
                        if (roleId <= 0 || !roleExists(connection, roleId)) {
                            response.status(400);
                            return error("INVALID_ROLE", "Target role does not exist");
                        }
                    }
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE users SET target_role_id = ? WHERE email = ?")) {
                        if (roleId == null) {
                            statement.setNull(1, Types.INTEGER);
                        } else {
                            statement.setInt(1, roleId);
                        }
                        statement.setString(2, request.attribute("email"));
                        statement.executeUpdate();
                    }
                }
                return gson.toJson(Map.of("message", "Profile updated"));
            } catch (SQLException exception) {
                return databaseError(response);
            }
        });

        get("/skills", (request, response) -> {
            List<Map<String, Object>> skills = new ArrayList<>();
            try (Connection connection = Database.connect();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT id, name, description FROM skills ORDER BY id");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Map<String, Object> skill = new HashMap<>();
                    skill.put("id", result.getInt("id"));
                    skill.put("name", result.getString("name"));
                    skill.put("description", result.getString("description"));
                    skills.add(skill);
                }
                return gson.toJson(Map.of("skills", skills));
            } catch (SQLException exception) {
                return databaseError(response);
            }
        });

        put("/skills/assessment", (request, response) -> {
            JsonObject data = parse(request, JsonObject.class);
            Integer skillId;
            Integer level;
            try {
                if (data == null || !data.has("skillId") || !data.has("level")) {
                    throw new IllegalArgumentException();
                }
                skillId = Integer.valueOf(data.get("skillId").getAsString());
                level = Integer.valueOf(data.get("level").getAsString());
            } catch (IllegalArgumentException | IllegalStateException exception) {
                response.status(400);
                return error("INVALID_SKILL_ASSESSMENT", "skillId and level must be integers");
            }
            if (skillId <= 0 || level < 1 || level > 5) {
                response.status(400);
                return error("INVALID_SKILL_ASSESSMENT", "Level must be from 1 to 5");
            }
            try (Connection connection = Database.connect()) {
                int userId = findUserId(connection, request.attribute("email"));
                if (userId == -1) {
                    response.status(404);
                    return error("USER_NOT_FOUND", "User not found");
                }
                if (!skillExists(connection, skillId)) {
                    response.status(404);
                    return error("SKILL_NOT_FOUND", "Skill not found");
                }
                String sql = "INSERT INTO user_skill_assessments (user_id, skill_id, level) "
                        + "VALUES (?, ?, ?) ON CONFLICT (user_id, skill_id) "
                        + "DO UPDATE SET level = EXCLUDED.level, updated_at = NOW()";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, userId);
                    statement.setInt(2, skillId);
                    statement.setInt(3, level);
                    statement.executeUpdate();
                }
                return gson.toJson(Map.of("message", "Skill assessment saved"));
            } catch (SQLException exception) {
                return databaseError(response);
            }
        });

        get("/skills/assessment", (request, response) -> {
            String sql = "SELECT s.id, s.name, a.level FROM user_skill_assessments a "
                    + "JOIN users u ON u.id = a.user_id "
                    + "JOIN skills s ON s.id = a.skill_id WHERE u.email = ? ORDER BY s.id";
            List<Map<String, Object>> assessments = new ArrayList<>();
            try (Connection connection = Database.connect();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, request.attribute("email"));
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        assessments.add(Map.of(
                                "skillId", result.getInt("id"),
                                "skill", result.getString("name"),
                                "level", result.getInt("level")));
                    }
                }
                return gson.toJson(Map.of("assessments", assessments));
            } catch (SQLException exception) {
                return databaseError(response);
            }
        });

        awaitInitialization();
        System.out.println("Server started on http://localhost:4567");
    }

    private static <T> T parse(Request request, Class<T> type) {
        try {
            return gson.fromJson(request.body(), type);
        } catch (JsonParseException | IllegalStateException exception) {
            return null;
        }
    }

    private static void authorize(Request request) {
        String header = request.headers("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            halt(401, error("UNAUTHORIZED", "Authorization Bearer token required"));
        }
        String email = jwtService.getEmail(header.substring(7));
        if (email == null) {
            halt(401, error("INVALID_TOKEN", "Invalid or expired token"));
        }
        request.attribute("email", email);
    }

    private static int findUserId(Connection connection, String email) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM users WHERE email = ?")) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt("id") : -1;
            }
        }
    }

    private static boolean roleExists(Connection connection, int id) throws SQLException {
        return exists(connection, "SELECT 1 FROM roles WHERE id = ?", id);
    }

    private static boolean skillExists(Connection connection, int id) throws SQLException {
        return exists(connection, "SELECT 1 FROM skills WHERE id = ?", id);
    }

    private static boolean exists(Connection connection, String sql, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    static String hashPassword(String password) {
        return BCrypt.hashpw(password + PEPPER, BCrypt.gensalt());
    }

    static boolean checkPassword(String password, String passwordHash) {
        return BCrypt.checkpw(password + PEPPER, passwordHash);
    }

    private static String databaseError(Response response) {
        response.status(500);
        return error("DATABASE_ERROR", "Database operation failed");
    }

    private static String error(String code, String message) {
        return gson.toJson(Map.of("error", code, "message", message));
    }
}
