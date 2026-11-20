package org.career.navigator.repository;

import org.career.navigator.Database;
import org.career.navigator.model.Skill;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Справочник навыков и их синонимов.
 * В рабочем режиме загружается из таблиц PostgreSQL (skills + skill_synonyms) через JDBC,
 * а конструктор по умолчанию сохраняет встроенный набор для оффлайн unit-тестов.
 */
public class SkillCatalog {
  private final Map<Long, Skill> skillsById = new LinkedHashMap<>();
  private SynonymDictionary dictionary = new SynonymDictionary();
  private final boolean backedByDatabase;

  /** Загружает справочник навыков и словарь синонимов напрямую из PostgreSQL. */
  public static SkillCatalog fromDatabase() throws SQLException {
    SkillCatalog catalog = new SkillCatalog(true);
    catalog.reloadFromDatabase();
    return catalog;
  }

  private SkillCatalog(boolean backedByDatabase) {
    this.backedByDatabase = backedByDatabase;
  }

  /** Конструктор со встроенным справочником (для оффлайн unit-тестов). */
  public SkillCatalog() {
    this.backedByDatabase = false;
    registerDefaults();
  }

  /** Перечитывает таблицы skills и skill_synonyms из PostgreSQL и перекомпилирует словарь синонимов. */
  public synchronized void reloadFromDatabase() throws SQLException {
    Map<Long, Skill> loadedSkills = new LinkedHashMap<>();
    Map<Long, List<String>> loadedSynonyms = new LinkedHashMap<>();

    String skillsSql = "SELECT id, name, description FROM skills ORDER BY id";
    String synonymsSql = "SELECT skill_id, synonym FROM skill_synonyms ORDER BY id";

    try (Connection connection = Database.connect()) {
      try (PreparedStatement statement = connection.prepareStatement(skillsSql);
           ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          long id = rs.getLong("id");
          String name = rs.getString("name");
          String description = rs.getString("description");
          loadedSkills.put(id, new Skill(id, name, description));
          loadedSynonyms.put(id, new ArrayList<>());
        }
      }

      try (PreparedStatement statement = connection.prepareStatement(synonymsSql);
           ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          long skillId = rs.getLong("skill_id");
          String synonym = rs.getString("synonym");
          List<String> list = loadedSynonyms.get(skillId);
          if (list != null && synonym != null && !synonym.isBlank()) {
            list.add(synonym);
          }
        }
      }
    }

    SynonymDictionary rebuilt = new SynonymDictionary();
    for (Skill skill : loadedSkills.values()) {
      rebuilt.register(skill.getId(), skill.getName(), loadedSynonyms.get(skill.getId()));
    }

    skillsById.clear();
    skillsById.putAll(loadedSkills);
    this.dictionary = rebuilt;
  }

  public boolean isBackedByDatabase() {
    return backedByDatabase;
  }

  private void registerDefaults() {
    // --- Базовые Java / Backend (ID 1..14 сохранены) ---
    register(1L, "Java", "Язык программирования Java",
        List.of("java core", "java se", "java ee", "jakarta ee", "jdk", "jvm", "джава", "ява"));
    register(2L, "Spring", "Фреймворк Spring / Spring Boot",
        List.of("spring boot", "springboot", "spring framework", "spring mvc", "spring data",
            "spring security", "spring cloud", "спринг"));
    register(3L, "SQL", "Язык запросов к БД",
        List.of("t-sql", "tsql", "pl/sql", "plsql", "pl/pgsql", "ansi sql", "mysql", "ms sql", "mssql"));
    register(4L, "PostgreSQL", "СУБД PostgreSQL",
        List.of("postgres", "postgre", "постгрес", "постгрескл", "psql"));
    register(5L, "Hibernate", "ORM Hibernate / JPA",
        List.of("jpa", "spring data jpa", "хибернейт"));
    register(6L, "Git", "Система контроля версий",
        List.of("github", "gitlab", "bitbucket", "гит"));
    register(7L, "Docker", "Контейнеризация",
        List.of("docker-compose", "docker compose", "докер"));
    register(8L, "Maven", "Сборщик Maven",
        List.of("apache maven", "мавен"));
    register(9L, "Gradle", "Сборщик Gradle",
        List.of("грэдл"));
    register(10L, "Kotlin", "Язык Kotlin",
        List.of("котлин"));
    register(11L, "Linux", "ОС Linux",
        List.of("ubuntu", "debian", "centos", "red hat", "rhel", "bash", "unix", "линукс"));
    register(12L, "REST", "REST API",
        List.of("rest api", "restful", "restful api", "openapi", "swagger", "рест"));
    register(13L, "Kafka", "Брокер сообщений Kafka",
        List.of("apache kafka", "кафка"));
    register(14L, "JUnit", "Тестирование JUnit",
        List.of("junit 5", "junit5", "mockito", "testcontainers"));

    // --- Прочие языки и Backend ---
    register(15L, "Python", "Язык программирования Python",
        List.of("python 3", "python3", "питон", "пайтон"));
    register(16L, "Django", "Веб-фреймворк Django",
        List.of("django rest framework", "drf", "джанго"));
    register(17L, "FastAPI", "Асинхронный веб-фреймворк FastAPI",
        List.of("fast api"));
    register(18L, "Go", "Язык программирования Go",
        List.of("golang", "go lang", "голанг"));
    register(19L, "C#", "Язык программирования C#",
        List.of("c sharp", "csharp", "си шарп"));
    register(20L, ".NET", "Платформа .NET",
        List.of("dotnet", "asp.net", "asp.net core", ".net core", "entity framework"));
    register(21L, "C++", "Язык программирования C++",
        List.of("cpp", "си++"));
    register(22L, "Node.js", "Среда выполнения Node.js",
        List.of("nodejs", "node js", "express", "nestjs", "nest.js"));
    register(23L, "Redis", "In-memory хранилище Redis",
        List.of("редис"));
    register(24L, "RabbitMQ", "Брокер сообщений RabbitMQ",
        List.of("rabbit mq", "amqp"));
    register(25L, "gRPC", "Фреймворк удалённого вызова процедур gRPC",
        List.of("protobuf", "protocol buffers"));
    register(26L, "MongoDB", "NoSQL СУБД MongoDB",
        List.of("mongo", "монго"));
    register(27L, "ClickHouse", "Аналитическая СУБД ClickHouse",
        List.of("click house", "кликхаус"));

    // --- Frontend ---
    register(28L, "JavaScript", "Язык программирования JavaScript",
        List.of("js", "ecmascript", "es6", "es2015", "джаваскрипт"));
    register(29L, "TypeScript", "Типизированный JavaScript (TypeScript)",
        List.of("ts", "тайпскрипт"));
    register(30L, "HTML/CSS", "Вёрстка HTML и CSS",
        List.of("html", "html5", "css", "css3", "scss", "sass", "less", "tailwind", "вёрстка", "верстка"));
    register(31L, "React", "Библиотека React",
        List.of("reactjs", "react.js", "react js", "redux", "redux toolkit", "next.js", "nextjs", "реакт"));
    register(32L, "Vue", "Фреймворк Vue.js",
        List.of("vue.js", "vuejs", "vue 3", "vue3", "vuex", "pinia", "nuxt", "nuxt.js"));
    register(33L, "Angular", "Фреймворк Angular",
        List.of("angularjs", "ангуляр"));

    // --- DevOps & Инфраструктура ---
    register(34L, "Kubernetes", "Оркестрация контейнеров Kubernetes",
        List.of("k8s", "helm", "openshift", "кубернетес", "кубер"));
    register(35L, "CI/CD", "Непрерывная интеграция и доставка",
        List.of("cicd", "ci cd", "gitlab ci", "github actions", "jenkins", "teamcity"));
    register(36L, "Terraform", "Инфраструктура как код (Terraform / Ansible)",
        List.of("ansible", "iac"));
    register(37L, "Nginx", "Веб-сервер и прокси Nginx",
        List.of("apache", "haproxy"));
    register(38L, "Prometheus/Grafana", "Мониторинг и визуализация метрик",
        List.of("prometheus", "grafana", "zabbix", "elk", "kibana", "графана"));

    // --- Data & AI ---
    register(39L, "Pandas/NumPy", "Обработка данных на Python",
        List.of("pandas", "numpy", "scipy", "matplotlib", "seaborn"));
    register(40L, "Machine Learning", "Машинное обучение",
        List.of("ml", "scikit-learn", "sklearn", "catboost", "xgboost", "lightgbm", "машинное обучение"));
    register(41L, "Deep Learning", "Глубокое обучение и нейросети",
        List.of("pytorch", "tensorflow", "keras", "nlp", "llm", "нейросети", "нейронные сети"));
    register(42L, "Apache Spark", "Распределённая обработка данных Spark",
        List.of("spark", "pyspark", "hadoop"));
    register(43L, "Apache Airflow", "Оркестрация ETL-пайплайнов Airflow",
        List.of("airflow", "dbt", "etl"));
    register(44L, "BI-инструменты", "Визуализация данных и дашборды",
        List.of("power bi", "powerbi", "tableau", "superset", "datalens", "metabase", "excel"));

    // --- QA ---
    register(45L, "Тестирование ПО", "Теория тестирования, тест-дизайн, баг-трекинг",
        List.of("тест-кейсы", "чек-листы", "jira", "testrail", "allure", "регрессионное тестирование"));
    register(46L, "Postman", "Тестирование API в Postman",
        List.of("soapui", "insomnia", "charles", "fiddler"));
    register(47L, "Автотесты UI/API", "Автоматизация тестирования",
        List.of("selenium", "selenide", "playwright", "cypress", "pytest", "rest assured", "appium"));

    // --- Mobile ---
    register(48L, "Android SDK", "Разработка под Android",
        List.of("android", "jetpack compose", "retrofit", "room", "dagger", "hilt", "coroutines"));
    register(49L, "Swift / iOS", "Разработка под iOS",
        List.of("swift", "swiftui", "uikit", "xcode", "objective-c", "ios"));

    // --- Analytics & 1C ---
    register(50L, "Системный анализ", "Проектирование требований и моделей",
        List.of("bpmn", "uml", "confluence", "user story", "use case", "тз", "техническое задание"));
    register(51L, "1С", "Платформа 1С:Предприятие",
        List.of("1с:предприятие", "1c", "1с 8", "1с 8.3", "ут", "erp", "зуп", "бсп", "скд"));
  }

  private void register(Long id, String name, String description, List<String> synonyms) {
    skillsById.put(id, new Skill(id, name, description));
    dictionary.register(id, name, synonyms);
  }

  public List<Skill> all() {
    return new ArrayList<>(skillsById.values());
  }

  public Optional<Skill> findById(Long id) {
    return Optional.ofNullable(skillsById.get(id));
  }

  public Skill requireById(Long id) {
    Skill skill = skillsById.get(id);
    if (skill == null) {
      throw new IllegalArgumentException("Неизвестный skillId: " + id);
    }
    return skill;
  }

  /** Приводит произвольный синоним (например, "постгрес" или "k8s") к каноническому Skill. */
  public Optional<Skill> normalize(String rawName) {
    return dictionary.resolve(rawName).map(skillsById::get);
  }

  /** Какие уникальные канонические навыки из справочника упоминаются в тексте. */
  public List<Skill> findIn(String text) {
    List<Skill> found = new ArrayList<>();
    for (Long id : dictionary.findSkillIdsIn(text)) {
      Skill skill = skillsById.get(id);
      if (skill != null) {
        found.add(skill);
      }
    }
    return found;
  }

  public List<String> synonymsOf(Long skillId) {
    return dictionary.getSynonyms(skillId);
  }
}
