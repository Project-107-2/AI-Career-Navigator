package org.career.navigator.controller;

import com.google.gson.Gson;
import org.career.navigator.integration.TrudvsemClient;
import org.career.navigator.model.Role;
import org.career.navigator.model.RoleSkill;
import org.career.navigator.model.Skill;
import org.career.navigator.repository.RoleRepository;
import org.career.navigator.repository.SkillCatalog;
import org.career.navigator.service.StatsService;
import org.career.navigator.service.VacancySyncScheduler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static spark.Spark.get;
import static spark.Spark.post;

/** Эндпоинты ролей, статистики вакансий, планировщика обновлений и словаря синонимов навыков. */
public class RoleController {
  private static final String JSON_UTF8 = "application/json; charset=UTF-8";

  private final Gson gson;
  private final SkillCatalog skillCatalog;
  private final RoleRepository roleRepository;
  private final VacancySyncScheduler scheduler;

  public RoleController(Gson gson) {
    this.gson = gson;
    this.skillCatalog = new SkillCatalog();
    TrudvsemClient trudvsemClient = new TrudvsemClient(skillCatalog);
    this.roleRepository = new RoleRepository(skillCatalog);
    StatsService statsService = new StatsService();
    this.scheduler = new VacancySyncScheduler(roleRepository, trudvsemClient, statsService);
  }

  public void registerRoutes() {
    // Запускаем фоновое обновление вакансий по расписанию и корректную остановку при выключении
    scheduler.start();
    Runtime.getRuntime().addShutdownHook(new Thread(scheduler::stop));

    // Список всех ролей по APIContract: {"roles": [...]} (опционально ?category=Backend)
    get("/roles", (request, response) -> {
      response.type(JSON_UTF8);
      String category = request.queryParams("category");
      List<Role> list = (category == null || category.isBlank())
          ? roleRepository.findAll()
          : roleRepository.findByCategory(category);

      List<Map<String, Object>> payload = new ArrayList<>();
      for (Role r : list) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", r.getId());
        item.put("name", r.getName());
        item.put("description", r.getDescription());
        item.put("category", r.getCategory());
        item.put("grade", r.getGrade());
        payload.add(item);
      }
      return gson.toJson(Map.of("roles", payload));
    });

    // Детали роли и требуемые навыки по APIContract: GET /roles/{id}
    get("/roles/:id", (request, response) -> {
      response.type(JSON_UTF8);
      Long id = parseRoleId(request.params(":id"));
      if (id == null) {
        response.status(400);
        return gson.toJson(Map.of("message", "Role id must be a number"));
      }

      Role role = roleRepository.findById(id);
      if (role == null) {
        response.status(404);
        return gson.toJson(Map.of("message", "Role not found"));
      }

      List<Map<String, Object>> roleSkills = new ArrayList<>();
      for (RoleSkill rs : role.getSkills()) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("id", rs.getSkill().getId());
        s.put("name", rs.getSkill().getName());
        s.put("requiredLevel", rs.getRequiredLevel());
        roleSkills.add(s);
      }

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("id", role.getId());
      result.put("name", role.getName());
      result.put("description", role.getDescription());
      result.put("category", role.getCategory());
      result.put("grade", role.getGrade());
      result.put("skills", roleSkills);
      return gson.toJson(result);
    });

    // Статистика вакансий по роли (из кэша планировщика или с дозагрузкой)
    get("/roles/:id/stats", (request, response) -> {
      response.type(JSON_UTF8);
      Long id = parseRoleId(request.params(":id"));
      if (id == null) {
        response.status(400);
        return gson.toJson(Map.of("message", "Role id must be a number"));
      }

      Role role = roleRepository.findById(id);
      if (role == null) {
        response.status(404);
        return gson.toJson(Map.of("message", "Role not found"));
      }

      return gson.toJson(scheduler.getOrFetchStats(role));
    });

    // История снимков статистики по датам для роли (заготовка под тренды и «рост за год»)
    get("/roles/:id/history", (request, response) -> {
      response.type(JSON_UTF8);
      Long id = parseRoleId(request.params(":id"));
      if (id == null) {
        response.status(400);
        return gson.toJson(Map.of("message", "Role id must be a number"));
      }

      Role role = roleRepository.findById(id);
      if (role == null) {
        response.status(404);
        return gson.toJson(Map.of("message", "Role not found"));
      }

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("roleId", role.getId());
      result.put("roleName", role.getName());
      result.put("snapshots", scheduler.getDailySnapshots(id));
      return gson.toJson(result);
    });

    // Статус фонового расписания обновления вакансий
    get("/sync/status", (request, response) -> {
      response.type(JSON_UTF8);
      return gson.toJson(scheduler.getStatus());
    });

    // Ручной запуск внеочередного обновления всех ролей
    post("/sync/run", (request, response) -> {
      response.type(JSON_UTF8);
      boolean started = scheduler.triggerAsyncSync();
      response.status(started ? 202 : 200);
      return gson.toJson(Map.of(
          "message", started ? "Background sync started" : "Sync is already running"));
    });

    // Справочник всех навыков вместе со списком синонимов
    get("/skills/catalog", (request, response) -> {
      response.type(JSON_UTF8);
      List<Map<String, Object>> items = new ArrayList<>();
      for (Skill skill : skillCatalog.all()) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", skill.getId());
        item.put("name", skill.getName());
        item.put("description", skill.getDescription());
        item.put("synonyms", skillCatalog.synonymsOf(skill.getId()));
        items.add(item);
      }
      return gson.toJson(Map.of("skills", items));
    });

    // Нормализация синонима к каноническому навыку: GET /skills/normalize?q=постгрес
    get("/skills/normalize", (request, response) -> {
      response.type(JSON_UTF8);
      String q = request.queryParams("q");
      if (q == null || q.isBlank()) {
        response.status(400);
        return gson.toJson(Map.of("message", "Query parameter 'q' is required"));
      }
      Optional<Skill> skill = skillCatalog.normalize(q);
      if (skill.isEmpty()) {
        response.status(404);
        return gson.toJson(Map.of("message", "Skill not found in synonym dictionary"));
      }
      return gson.toJson(skill.get());
    });
  }

  private static Long parseRoleId(String raw) {
    try {
      return Long.parseLong(raw);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
