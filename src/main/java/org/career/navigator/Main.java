package org.career.navigator;
import org.career.navigator.repository.PostgresUserRepository;
import org.career.navigator.integration.TrudvsemClient;
import org.career.navigator.service.StatsService;
import org.career.navigator.repository.SkillCatalog;
import org.career.navigator.repository.RoleRepository;
import org.career.navigator.model.Vacancy;
import org.career.navigator.model.Role;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import org.career.navigator.security.JWTService;
import org.career.navigator.repository.InMemoryUserRepository;
import org.career.navigator.repository.UserRepository;
import org.career.navigator.model.User;
import org.career.navigator.dto.RegisterRequest;
import org.career.navigator.dto.LoginRequest;
import org.career.navigator.service.SkillGraphService;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Map;

import static spark.Spark.*;

public class Main {

    private static final UserRepository users = createUserRepository();

    /** Если задана переменная окружения DB_URL — PostgreSQL, иначе хранение в памяти. */
    private static UserRepository createUserRepository() {
        String url = System.getenv("DB_URL");
        if (url == null || url.isBlank()) {
            System.out.println("DB_URL не задан: пользователи хранятся в памяти");
            return new InMemoryUserRepository();
        }
        System.out.println("Пользователи хранятся в PostgreSQL: " + url);
        return new PostgresUserRepository(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
    }


    private static final JWTService jwtService =
            new JWTService();

    public static void main(String[] args) {

        port(4567);

        Gson gson = new Gson();

        before((request, response) -> {
            response.type("application/json");
            response.header("Access-Control-Allow-Origin", "*");
            response.header(
                    "Access-Control-Allow-Methods",
                    "GET, POST, PUT, OPTIONS"
            );
            response.header(
                    "Access-Control-Allow-Headers",
                    "Content-Type, Authorization"
            );
        });

        options("/*", (request, response) -> {
            response.status(200);
            return "";
        });

        post("/auth/register", (request, response) -> {

            RegisterRequest data =
                    gson.fromJson(
                            request.body(),
                            RegisterRequest.class
                    );

            if (data == null
                    || data.name == null
                    || data.email == null
                    || data.password == null
                    || data.name.isBlank()
                    || data.email.isBlank()
                    || data.password.length() < 8) {

                response.status(400);

                return gson.toJson(
                        Map.of(
                                "message",
                                "Invalid registration data"
                        )
                );
            }

            String email =
                    data.email.trim().toLowerCase();

            if (users.findByEmail(email) != null) {
                response.status(409);

                return gson.toJson(
                        Map.of(
                                "message",
                                "User already exists"
                        )
                );
            }

            String passwordHash =
                    BCrypt.hashpw(
                            data.password,
                            BCrypt.gensalt()
                    );

            User user =
                    new User(
                            data.name,
                            email,
                            passwordHash
                    );

            users.save(user);

            response.status(201);

            return gson.toJson(
                    Map.of(
                            "message",
                            "Registration successful"
                    )
            );
        });

        post("/auth/login", (request, response) -> {

            LoginRequest data =
                    gson.fromJson(
                            request.body(),
                            LoginRequest.class
                    );

            if (data == null
                    || data.email == null
                    || data.password == null
                    || data.email.isBlank()
                    || data.password.isBlank()) {

                response.status(400);

                return gson.toJson(
                        Map.of(
                                "message",
                                "Invalid login data"
                        )
                );
            }

            String email =
                    data.email.trim().toLowerCase();

            User user =
                    users.findByEmail(email);

            if (user == null
                    || !BCrypt.checkpw(
                            data.password,
                            user.passwordHash
                    )) {

                response.status(401);

                return gson.toJson(
                        Map.of(
                                "message",
                                "Invalid email or password"
                        )
                );
            }

            String token =
                    jwtService.createToken(email);

            response.status(200);

            return gson.toJson(
                    Map.of(
                            "message",
                            "Login successful",
                            "token",
                            token
                    )
            );
        });

        String[] skills = {
                "Java",
                "Spring",
                "PostgreSQL",
                "Kafka"
        };

        get("/skills", (request, response) -> {
            return gson.toJson(
                    Map.of(
                            "skills",
                            skills
                    )
            );
        });

        before("/profile", (request, response) -> {

            if (request.requestMethod().equals("OPTIONS")) {
                return;
            }

            String authHeader =
                    request.headers("Authorization");

            if (authHeader == null
                    || !authHeader.startsWith("Bearer ")) {

                halt(
                        401,
                        gson.toJson(
                                Map.of(
                                        "message",
                                        "Unauthorized"
                                )
                        )
                );
            }

            String token =
                    authHeader.substring(7);

            String email =
                    jwtService.getEmail(token);

            if (email == null) {
                halt(
                        401,
                        gson.toJson(
                                Map.of(
                                        "message",
                                        "Invalid token"
                                )
                        )
                );
            }

            request.attribute(
                    "email",
                    email
            );
        });

        get("/profile", (request, response) -> {

            String email =
                    request.attribute("email");

            User user =
                    users.findByEmail(email);

            if (user == null) {
                response.status(404);

                return gson.toJson(
                        Map.of(
                                "message",
                                "User not found"
                        )
                );
            }

            return gson.toJson(
                    Map.of(
                            "name", user.name,
                            "email", user.email,
                            "grade", user.grade,
                            "targetRole", user.targetRole
                    )
            );
        });

        put("/profile", (request, response) -> {

            String email =
                    request.attribute("email");

            User user =
                    users.findByEmail(email);

            if (user == null) {
                response.status(404);

                return gson.toJson(
                        Map.of(
                                "message",
                                "User not found"
                        )
                );
            }

            JsonObject data =
                    gson.fromJson(
                            request.body(),
                            JsonObject.class
                    );

            if (data != null && data.has("grade")) {
                user.grade =
                        data.get("grade")
                                .getAsString();
            }

            if (data != null
                    && data.has("targetRole")) {

                user.targetRole =
                        data.get("targetRole")
                                .getAsString();
            }

            users.save(user);

            return gson.toJson(
                    Map.of(
                            "message",
                            "Profile updated"
                    )
            );
        });
        // Статистика вакансий по роли (перенесено из vacancy-stats)
        SkillCatalog skillCatalog = new SkillCatalog();
        TrudvsemClient trudvsemClient = new TrudvsemClient(skillCatalog);
        RoleRepository roleRepository = new RoleRepository();
        StatsService statsService = new StatsService();
        Map<Long, List<Vacancy>> vacancyCache = new ConcurrentHashMap<>();

        get("/roles/:id/stats", (request, response) -> {
            long id;
            try {
                id = Long.parseLong(request.params(":id"));
            } catch (NumberFormatException e) {
                response.status(400);
                return gson.toJson(Map.of("message", "Role id must be a number"));
            }

            Role role = roleRepository.findById(id);
            if (role == null) {
                response.status(404);
                return gson.toJson(Map.of("message", "Role not found"));
            }

            if (!vacancyCache.containsKey(id)) {
                vacancyCache.put(id, trudvsemClient.search(role.getName(), 100));
            }
            return gson.toJson(statsService.compute(role, vacancyCache.get(id)));
        });



        // Граф навыков: вершины — навыки, рёбра — что учить раньше чего
        SkillGraphService skillGraphService = new SkillGraphService(skillCatalog);

        get("/skills/graph", (request, response) ->
          gson.toJson(skillGraphService.graph()));awaitInitialization();

        System.out.println(
                "Server started on http://localhost:4567"
        );
    }
}

