package org.career.navigator.repository;

import org.career.navigator.Database;
import org.career.navigator.model.Role;
import org.career.navigator.model.RoleSkill;
import org.career.navigator.model.Skill;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Репозиторий IT-ролей.
 * В рабочем режиме (RoleRepository.fromDatabase) читает таблицы PostgreSQL
 * (roles, role_skills, role_search_queries) напрямую через JDBC
 * (Connection / PreparedStatement / ResultSet).
 * Конструктор по умолчанию сохраняет in-memory набор для оффлайн unit-тестов.
 */
public class RoleRepository {
  private final Map<Long, Role> roles = new LinkedHashMap<>();
  private final SkillCatalog skillCatalog;
  private final boolean useDatabase;

  /** Создаёт JDBC-репозиторий, работающий напрямую с PostgreSQL. */
  public static RoleRepository fromDatabase(SkillCatalog skillCatalog) {
    return new RoleRepository(skillCatalog, true);
  }

  public RoleRepository() {
    this(new SkillCatalog(), false);
  }

  public RoleRepository(SkillCatalog skillCatalog) {
    this(skillCatalog, false);
  }

  private RoleRepository(SkillCatalog skillCatalog, boolean useDatabase) {
    this.skillCatalog = skillCatalog;
    this.useDatabase = useDatabase;
    if (!useDatabase) {
      seedInMemory();
    }
  }

  public boolean isBackedByDatabase() {
    return useDatabase;
  }

  public void add(Role role) {
    if (!useDatabase) {
      roles.put(role.getId(), role);
      return;
    }
    String upsertRoleSql =
        "INSERT INTO roles (id, name, description, category, grade) VALUES (?, ?, ?, ?, ?) "
            + "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, "
            + "description = EXCLUDED.description, category = EXCLUDED.category, grade = EXCLUDED.grade";
    String upsertSkillSql =
        "INSERT INTO role_skills (role_id, skill_id, required_level) VALUES (?, ?, ?) "
            + "ON CONFLICT (role_id, skill_id) DO UPDATE SET required_level = EXCLUDED.required_level";
    String upsertQuerySql =
        "INSERT INTO role_search_queries (role_id, query_text) VALUES (?, ?) "
            + "ON CONFLICT (role_id, query_text) DO NOTHING";

    try (Connection connection = Database.connect()) {
      connection.setAutoCommit(false);
      try {
        try (PreparedStatement stmt = connection.prepareStatement(upsertRoleSql)) {
          stmt.setLong(1, role.getId());
          stmt.setString(2, role.getName());
          stmt.setString(3, role.getDescription());
          stmt.setString(4, role.getCategory() == null ? "General" : role.getCategory());
          stmt.setString(5, role.getGrade() == null ? "" : role.getGrade());
          stmt.executeUpdate();
        }
        try (PreparedStatement stmt = connection.prepareStatement(upsertSkillSql)) {
          for (RoleSkill rs : role.getSkills()) {
            stmt.setLong(1, role.getId());
            stmt.setLong(2, rs.getSkill().getId());
            stmt.setInt(3, rs.getRequiredLevel());
            stmt.addBatch();
          }
          stmt.executeBatch();
        }
        try (PreparedStatement stmt = connection.prepareStatement(upsertQuerySql)) {
          for (String q : role.getSearchQueries()) {
            stmt.setLong(1, role.getId());
            stmt.setString(2, q);
            stmt.addBatch();
          }
          stmt.executeBatch();
        }
        connection.commit();
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to save role to PostgreSQL: " + role.getName(), e);
    }
  }

  public Role findById(Long id) {
    if (id == null) {
      return null;
    }
    if (!useDatabase) {
      return roles.get(id);
    }
    String sql = "SELECT id, name, description, category, grade FROM roles WHERE id = ?";
    try (Connection connection = Database.connect();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, id);
      try (ResultSet rs = statement.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        Role role = mapRole(rs);
        loadDetailsForRoles(connection, Map.of(role.getId(), role));
        return role;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find role by id=" + id, e);
    }
  }

  public List<Role> findAll() {
    if (!useDatabase) {
      return new ArrayList<>(roles.values());
    }
    String sql = "SELECT id, name, description, category, grade FROM roles ORDER BY id";
    return queryRoles(sql, null);
  }

  public List<Role> findByCategory(String category) {
    if (category == null || category.isBlank()) {
      return findAll();
    }
    if (!useDatabase) {
      List<Role> result = new ArrayList<>();
      for (Role role : roles.values()) {
        if (role.getCategory() != null && role.getCategory().equalsIgnoreCase(category)) {
          result.add(role);
        }
      }
      return result;
    }
    String sql =
        "SELECT id, name, description, category, grade FROM roles "
            + "WHERE LOWER(category) = LOWER(?) ORDER BY id";
    return queryRoles(sql, category.trim());
  }

  public Role findByName(String name) {
    if (name == null || name.isBlank()) {
      return null;
    }
    if (!useDatabase) {
      for (Role role : roles.values()) {
        if (role.getName().equalsIgnoreCase(name.trim())) {
          return role;
        }
      }
      return null;
    }
    String sql =
        "SELECT id, name, description, category, grade FROM roles "
            + "WHERE LOWER(name) = LOWER(?) LIMIT 1";
    List<Role> found = queryRoles(sql, name.trim());
    return found.isEmpty() ? null : found.get(0);
  }

  private List<Role> queryRoles(String sql, String singleParam) {
    Map<Long, Role> result = new LinkedHashMap<>();
    try (Connection connection = Database.connect();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      if (singleParam != null) {
        statement.setString(1, singleParam);
      }
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          Role role = mapRole(rs);
          result.put(role.getId(), role);
        }
      }
      if (!result.isEmpty()) {
        loadDetailsForRoles(connection, result);
      }
      return new ArrayList<>(result.values());
    } catch (SQLException e) {
      throw new RuntimeException("Failed to load roles from PostgreSQL", e);
    }
  }

  private Role mapRole(ResultSet rs) throws SQLException {
    return new Role(
        rs.getLong("id"),
        rs.getString("name"),
        rs.getString("description"),
        rs.getString("category"),
        rs.getString("grade"),
        new ArrayList<>());
  }

  private void loadDetailsForRoles(Connection connection, Map<Long, Role> roleMap)
      throws SQLException {
    String queriesSql = "SELECT role_id, query_text FROM role_search_queries ORDER BY id";
    try (PreparedStatement stmt = connection.prepareStatement(queriesSql);
         ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        Role role = roleMap.get(rs.getLong("role_id"));
        if (role != null) {
          role.getSearchQueries().add(rs.getString("query_text"));
        }
      }
    }

    String skillsSql =
        "SELECT rs.role_id, s.id AS skill_id, s.name AS skill_name, "
            + "s.description AS skill_desc, rs.required_level "
            + "FROM role_skills rs "
            + "JOIN skills s ON s.id = rs.skill_id "
            + "ORDER BY rs.role_id, s.id";
    try (PreparedStatement stmt = connection.prepareStatement(skillsSql);
         ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        Role role = roleMap.get(rs.getLong("role_id"));
        if (role != null) {
          long skillId = rs.getLong("skill_id");
          Skill skill = skillCatalog
              .findById(skillId)
              .orElseGet(() -> new Skill(skillId, safeGet(rs, "skill_name"), safeGet(rs, "skill_desc")));
          role.getSkills().add(new RoleSkill(skill, rs.getInt("required_level")));
        }
      }
    }

    // Если для роли в БД не заданы явные search_queries, используем название самой роли
    for (Role role : roleMap.values()) {
      if (role.getSearchQueries().isEmpty() && role.getName() != null) {
        role.getSearchQueries().add(role.getName());
      }
    }
  }

  private static String safeGet(ResultSet rs, String column) {
    try {
      return rs.getString(column);
    } catch (SQLException e) {
      return "";
    }
  }

  private void register(Role role, Map<Long, Integer> requiredSkills) {
    requiredSkills.entrySet().stream()
        .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
        .forEach(entry -> role.getSkills().add(
            new RoleSkill(skillCatalog.requireById(entry.getKey()), entry.getValue())));
    roles.put(role.getId(), role);
  }

  private void seedInMemory() {
    register(
        new Role(1L, "Java Developer",
            "Backend-разработка на Java, Spring Boot, PostgreSQL и REST API",
            "Backend", "Middle", List.of("Java разработчик", "Программист Java", "Java")),
        Map.of(1L, 4, 2L, 4, 3L, 3, 4L, 3, 5L, 3, 6L, 3, 7L, 3, 12L, 4));
    register(
        new Role(2L, "Junior Java Developer",
            "Начальная backend-разработка на Java: Java Core, основы Spring, SQL, Git",
            "Backend", "Junior", List.of("Junior Java", "Младший разработчик Java", "Стажер Java")),
        Map.of(1L, 3, 2L, 2, 3L, 2, 4L, 2, 6L, 2, 8L, 2, 12L, 2, 14L, 2));
    register(
        new Role(3L, "Senior Java Developer",
            "Архитектура высоконагруженных систем на Java, Spring Cloud, Kafka, оптимизация БД",
            "Backend", "Senior", List.of("Senior Java", "Ведущий разработчик Java", "Старший разработчик Java")),
        Map.of(1L, 5, 2L, 5, 4L, 4, 5L, 4, 7L, 4, 12L, 5, 13L, 4, 23L, 3, 34L, 3));
    register(
        new Role(4L, "Frontend Developer",
            "Разработка клиентской части веб-приложений на JavaScript/TypeScript, HTML/CSS и React",
            "Frontend", "Middle", List.of("Frontend разработчик", "Фронтенд разработчик", "JavaScript разработчик")),
        Map.of(28L, 4, 29L, 3, 30L, 4, 31L, 4, 6L, 3, 12L, 3));
    register(
        new Role(5L, "Junior Frontend Developer",
            "Адаптивная вёрстка HTML/CSS, базовый JavaScript и компоненты React/Vue",
            "Frontend", "Junior", List.of("Junior Frontend", "HTML верстальщик", "Младший frontend")),
        Map.of(28L, 3, 30L, 3, 31L, 2, 6L, 2));
    register(
        new Role(6L, "Frontend Developer (React)",
            "Разработка SPA-приложений на TypeScript, React, Redux/Next.js",
            "Frontend", "Middle", List.of("React разработчик", "Frontend React")),
        Map.of(28L, 4, 29L, 4, 30L, 4, 31L, 5, 6L, 3, 12L, 3));
    register(
        new Role(7L, "Data Scientist",
            "Машинное обучение, анализ данных, Python, модели ML/DL и статистика",
            "Data & AI", "Middle", List.of("Data Scientist", "Специалист по данным", "Машинное обучение")),
        Map.of(15L, 4, 3L, 3, 39L, 4, 40L, 4, 41L, 3, 6L, 2));
    register(
        new Role(8L, "DevOps Engineer",
            "CI/CD, контейнеризация Docker/Kubernetes, Linux, автоматизация инфраструктуры",
            "DevOps", "Middle", List.of("DevOps", "Инженер DevOps")),
        Map.of(11L, 4, 7L, 4, 34L, 4, 35L, 4, 36L, 3, 37L, 3, 38L, 3, 6L, 3));
    register(
        new Role(9L, "Frontend Developer (Vue)",
            "Разработка интерфейсов на Vue.js, Pinia/Nuxt и TypeScript",
            "Frontend", "Middle", List.of("Vue разработчик", "Frontend Vue")),
        Map.of(28L, 4, 29L, 3, 30L, 4, 32L, 4, 6L, 3, 12L, 3));
    register(
        new Role(10L, "Fullstack Developer",
            "Разработка клиентской и серверной части веб-приложений (JS/TS, Node.js/Java/Python, SQL)",
            "Fullstack", "Middle", List.of("Fullstack разработчик", "Full-stack разработчик")),
        Map.of(28L, 4, 29L, 3, 30L, 3, 31L, 3, 22L, 3, 3L, 3, 4L, 3, 7L, 2, 12L, 3));
    register(
        new Role(11L, "Junior Python Developer",
            "Начальная backend-разработка на Python, основы Django/FastAPI и SQL",
            "Backend", "Junior", List.of("Junior Python", "Младший разработчик Python", "Стажер Python")),
        Map.of(15L, 3, 16L, 2, 3L, 2, 4L, 2, 6L, 2, 12L, 2));
    register(
        new Role(12L, "Middle Python Developer",
            "Backend-разработка на Python, FastAPI/Django, PostgreSQL, Redis, Docker",
            "Backend", "Middle", List.of("Python разработчик", "Программист Python", "Backend Python")),
        Map.of(15L, 4, 16L, 3, 17L, 4, 4L, 3, 7L, 3, 11L, 3, 12L, 4, 23L, 3));
    register(
        new Role(13L, "Go Developer",
            "Разработка высокопроизводительных микросервисов на Go (Golang), gRPC, Kafka",
            "Backend", "Middle", List.of("Golang", "Go разработчик", "Программист Go")),
        Map.of(18L, 4, 4L, 4, 7L, 3, 11L, 3, 13L, 3, 23L, 3, 25L, 3, 6L, 3));
    register(
        new Role(14L, "C# / .NET Developer",
            "Разработка серверных и корпоративных приложений на C# и ASP.NET Core",
            "Backend", "Middle", List.of("C# разработчик", "Программист C#", ".NET разработчик")),
        Map.of(19L, 4, 20L, 4, 3L, 3, 4L, 3, 7L, 2, 12L, 3, 6L, 3));
    register(
        new Role(15L, "ML Engineer",
            "Обучение и вывод ML/DL моделей в продакшен, PyTorch, Docker, MLOps",
            "Data & AI", "Middle", List.of("ML инженер", "Инженер машинного обучения", "Machine Learning")),
        Map.of(15L, 4, 39L, 4, 40L, 4, 41L, 4, 7L, 3, 11L, 3, 6L, 3));
    register(
        new Role(16L, "Data Analyst",
            "Анализ данных, SQL, Python/Pandas, BI-дашборды и продуктовые метрики",
            "Data & AI", "Middle", List.of("Аналитик данных", "Data Analyst", "BI аналитик")),
        Map.of(3L, 4, 15L, 3, 39L, 3, 44L, 4, 27L, 2));
    register(
        new Role(17L, "Data Engineer",
            "Построение ETL/ELT-пайплайнов, хранилищ данных, Airflow, Spark, Kafka",
            "Data & AI", "Middle", List.of("Data Engineer", "Инженер данных")),
        Map.of(15L, 4, 3L, 4, 4L, 4, 13L, 3, 27L, 3, 42L, 4, 43L, 4, 7L, 3));
    register(
        new Role(18L, "QA Engineer (Manual)",
            "Ручное тестирование веб и API, тест-дизайн, баг-репорты, Postman, SQL",
            "QA", "Junior", List.of("Тестировщик ПО", "QA инженер", "Ручное тестирование")),
        Map.of(45L, 4, 46L, 3, 3L, 2, 12L, 3, 6L, 2));
    register(
        new Role(19L, "QA Automation Engineer",
            "Автоматизация тестирования UI и API (Java/Python, Selenium/Playwright, JUnit, CI/CD)",
            "QA", "Middle", List.of("Автоматизатор тестирования", "QA Automation", "Тестировщик автоматизатор")),
        Map.of(45L, 3, 46L, 3, 47L, 4, 1L, 3, 14L, 4, 12L, 3, 6L, 3, 35L, 2));
    register(
        new Role(20L, "Android Developer",
            "Нативная разработка мобильных приложений под Android на Kotlin и Java",
            "Mobile", "Middle", List.of("Android разработчик", "Программист Android", "Kotlin разработчик")),
        Map.of(10L, 4, 1L, 3, 48L, 4, 9L, 3, 12L, 3, 6L, 3));
    register(
        new Role(21L, "iOS Developer",
            "Нативная разработка мобильных приложений под iOS на Swift",
            "Mobile", "Middle", List.of("iOS разработчик", "Swift разработчик")),
        Map.of(49L, 4, 12L, 3, 6L, 3));
    register(
        new Role(22L, "System Analyst",
            "Сбор и формализация требований, проектирование API, БД, BPMN/UML",
            "Analytics", "Middle", List.of("Системный аналитик", "System Analyst")),
        Map.of(50L, 4, 3L, 3, 12L, 4, 46L, 2));
    register(
        new Role(23L, "1C Developer",
            "Разработка и сопровождение конфигураций на платформе 1С:Предприятие",
            "Enterprise", "Middle", List.of("Программист 1С", "Разработчик 1С")),
        Map.of(51L, 4, 3L, 3, 12L, 2));
  }
}
