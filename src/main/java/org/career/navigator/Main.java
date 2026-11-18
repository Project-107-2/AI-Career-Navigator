package org.career.navigator;

import com.google.gson.Gson;

import java.util.Map;

import static spark.Spark.*;

import java.util.concurrent.ConcurrentHashMap;

import org.mindrot.jbcrypt.BCrypt;

public class Main {
  private static final Map<String, User> users = new ConcurrentHashMap<>();

  public static void main(String[] args) {
    port(4567);
    before((request, response) -> {
      response.header("Access-Control-Allow-Origin", "*"); //Позже ограничим доступ конкретным адресом frontend.
      response.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
      response.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
    });
    options("/*", (request, response) -> {  //позже проверим его вместе с реальными запросами frontend
      response.status(200);
      return "OK";
    });
    Gson gson = new Gson();
    Map<String, String> profile = Map.of(
      "name", "Alena",
      "email", "alena@example.com"
    );
    String[] skills = {"Java", "Spring", "PostgreSQL", "Kafka"};
    String json = gson.toJson(skills);

    post("/auth/register", (request, response) -> { //позже подключим PostgreSQL.
      response.type("application/json");

      RegisterRequest data = gson.fromJson(
        request.body(), RegisterRequest.class
      );

      if (data == null || data.name == null || data.email == null
        || data.password == null
        || data.name.isBlank() || data.email.isBlank()
        || data.password.length() < 8) {
        response.status(400);
        return gson.toJson(Map.of(
          "message", "Invalid registration data"
        ));
      }

      String email = data.email.trim().toLowerCase();

      String passwordHash = BCrypt.hashpw(
        data.password, BCrypt.gensalt()
      );

      User user = new User(data.name, email, passwordHash);

      if (users.putIfAbsent(email, user) != null) {
        response.status(409);
        return gson.toJson(Map.of(
          "message", "User already exists"
        ));
      }

      response.status(201);
      return gson.toJson(Map.of(
        "message", "Registration successful"
      ));
    });

    post("/auth/login", (request, response) -> {
      response.type("application/json");

      LoginRequest data = gson.fromJson(
        request.body(), LoginRequest.class
      );

      if (data == null || data.email == null || data.password == null
        || data.email.isBlank() || data.password.isBlank()) {
        response.status(400);
        return gson.toJson(Map.of(
          "message", "Invalid login data"
        ));
      }

      String email = data.email.trim().toLowerCase();
      User user = users.get(email);

      if (user == null || !BCrypt.checkpw(data.password, user.passwordHash)) {
        response.status(401);
        return gson.toJson(Map.of(
          "message", "Invalid email or password"
        ));
      }

      response.status(200);
      return gson.toJson(Map.of(
        "message", "Login successful"
      ));
    });

    get("/skills", (request, response) -> {
      response.type("application/json");
      return gson.toJson(Map.of("skills", skills));
    });
    before("/profile", (request, response) -> { //пока только проверка наличия заголовка
      String authHeader = request.headers("Authorization");

      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        halt(401, "{\"message\":\"Unauthorized\"}");
      }
    });
    get("/profile", (request, response) -> {
      response.type("application/json");
      return gson.toJson(profile);
    });
  }
}