import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.port;

public class Main {
    public static void main(String[] args) {
        SkillCatalog catalog = new SkillCatalog();
        TrudvsemClient client = new TrudvsemClient(catalog);
        RoleRepository roles = new RoleRepository();
        StatsService stats = new StatsService();
        ObjectMapper json = new ObjectMapper();

        // вакансии храним в памяти: скачиваем один раз на роль, дальше берём отсюда
        Map<Long, List<Vacancy>> cache = new HashMap<>();

        port(4567);

        // все ответы в JSON; CORS, чтобы фронт Алёны мог обращаться к серверу
        before((req, res) -> {
            res.type("application/json");
            res.header("Access-Control-Allow-Origin", "*");
        });

        get("/roles/:id/stats", (req, res) -> {
            Long id;
            try {
                id = Long.parseLong(req.params(":id"));
            } catch (NumberFormatException e) {
                res.status(400);
                return "{\"error\":\"id должен быть числом\"}";
            }

            Role role = roles.findById(id);
            if (role == null) {
                res.status(404);
                return "{\"error\":\"роль не найдена\"}";
            }

            if (!cache.containsKey(id)) {
                cache.put(id, client.search(role.name(), 100));
            }
            return json.writeValueAsString(stats.compute(role, cache.get(id)));
        });

        System.out.println("Сервер запущен: http://localhost:4567/roles/1/stats");
    }
}
