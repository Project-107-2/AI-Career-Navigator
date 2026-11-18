package org.career.navigator.integration;
import org.career.navigator.repository.SkillCatalog;
import org.career.navigator.model.Vacancy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Клиент открытого API «Работа России» (trudvsem.ru). Ключ не нужен. */
public class TrudvsemClient {
    private static final String BASE = "https://opendata.trudvsem.ru/api/v1";
    private static final String USER_AGENT = "vacancy-stats/0.1";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private final SkillCatalog catalog;

    public TrudvsemClient(SkillCatalog catalog) {
        this.catalog = catalog;
    }

    /** Ищет вакансии по запросу, возвращает до limit штук (максимум 100 за запрос). */
    public List<Vacancy> search(String text, int limit) throws IOException, InterruptedException {
        String q = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String body = get("/vacancies?text=" + q + "&offset=0&limit=" + limit);
        return parse(body);
    }

    /** Разбор ответа отдельно от сети — так его удобно проверять на сохранённом JSON. */
    public List<Vacancy> parse(String body) throws IOException {
        List<Vacancy> result = new ArrayList<>();
        // ответ: { "results": { "vacancies": [ { "vacancy": {...} }, ... ] } }
        for (JsonNode wrapper : json.readTree(body).path("results").path("vacancies")) {
            result.add(toVacancy(wrapper.path("vacancy")));
        }
        return result;
    }

    private Vacancy toVacancy(JsonNode v) {
        // навыки ищем в обязанностях и требованиях
        String text = v.path("job-name").asText("") + " "
                + v.path("duty").asText("") + " "
                + v.path("requirement").path("qualification").asText("");

        return new Vacancy(
                v.path("id").asText(),
                v.path("job-name").asText(),
                catalog.findIn(text),
                salaryOrNull(v.path("salary_min")),
                salaryOrNull(v.path("salary_max")),
                v.path("currency").asText(null)
        );
    }

    // в trudvsem 0 означает «не указано»
    private static Integer salaryOrNull(JsonNode n) {
        return n.isNumber() && n.asInt() > 0 ? n.asInt() : null;
    }

    private String get(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("trudvsem вернул " + resp.statusCode() + ": " + resp.body());
        }
        return resp.body();
    }
}
