package org.career.navigator.repository;

import org.career.navigator.model.Role;
import org.career.navigator.model.RoleSkill;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Справочник IT-ролей с разбиением по направлениям, специализациям и грейдам.
 * Для каждой роли указаны поисковые запросы для trudvsem.ru и требуемые навыки (RoleSkill).
 */
public class RoleRepository {
  private final Map<Long, Role> roles = new LinkedHashMap<>();
  private final SkillCatalog skillCatalog;

  public RoleRepository() {
    this(new SkillCatalog());
  }

  public RoleRepository(SkillCatalog skillCatalog) {
    this.skillCatalog = skillCatalog;

    // --- Backend: Java (id=1 оставлен базовой Java-ролью для совместимости с фронтом) ---
    register(
        new Role(
            1L,
            "Java Developer",
            "Backend-разработка на Java, Spring Boot, PostgreSQL и REST API",
            "Backend",
            "Middle",
            List.of("Java разработчик", "Программист Java", "Java")),
        Map.of(1L, 4, 2L, 4, 3L, 3, 4L, 3, 5L, 3, 6L, 3, 7L, 3, 12L, 4));

    register(
        new Role(
            2L,
            "Junior Java Developer",
            "Начальная backend-разработка на Java: Java Core, основы Spring, SQL, Git",
            "Backend",
            "Junior",
            List.of("Junior Java", "Младший разработчик Java", "Стажер Java")),
        Map.of(1L, 3, 2L, 2, 3L, 2, 4L, 2, 6L, 2, 8L, 2, 12L, 2, 14L, 2));

    register(
        new Role(
            3L,
            "Senior Java Developer",
            "Архитектура высоконагруженных систем на Java, Spring Cloud, Kafka, оптимизация БД",
            "Backend",
            "Senior",
            List.of("Senior Java", "Ведущий разработчик Java", "Старший разработчик Java")),
        Map.of(1L, 5, 2L, 5, 4L, 4, 5L, 4, 7L, 4, 12L, 5, 13L, 4, 23L, 3, 34L, 3));

    // --- Frontend & Fullstack ---
    register(
        new Role(
            4L,
            "Frontend Developer",
            "Разработка клиентской части веб-приложений на JavaScript/TypeScript, HTML/CSS и React",
            "Frontend",
            "Middle",
            List.of("Frontend разработчик", "Фронтенд разработчик", "JavaScript разработчик")),
        Map.of(28L, 4, 29L, 3, 30L, 4, 31L, 4, 6L, 3, 12L, 3));

    register(
        new Role(
            5L,
            "Junior Frontend Developer",
            "Адаптивная вёрстка HTML/CSS, базовый JavaScript и компоненты React/Vue",
            "Frontend",
            "Junior",
            List.of("Junior Frontend", "HTML верстальщик", "Младший frontend")),
        Map.of(28L, 3, 30L, 3, 31L, 2, 6L, 2));

    register(
        new Role(
            6L,
            "Frontend Developer (React)",
            "Разработка SPA-приложений на TypeScript, React, Redux/Next.js",
            "Frontend",
            "Middle",
            List.of("React разработчик", "Frontend React")),
        Map.of(28L, 4, 29L, 4, 30L, 4, 31L, 5, 6L, 3, 12L, 3));

    register(
        new Role(
            7L,
            "Data Scientist",
            "Машинное обучение, анализ данных, Python, модели ML/DL и статистика",
            "Data & AI",
            "Middle",
            List.of("Data Scientist", "Специалист по данным", "Машинное обучение")),
        Map.of(15L, 4, 3L, 3, 39L, 4, 40L, 4, 41L, 3, 6L, 2));

    register(
        new Role(
            8L,
            "DevOps Engineer",
            "CI/CD, контейнеризация Docker/Kubernetes, Linux, автоматизация инфраструктуры",
            "DevOps",
            "Middle",
            List.of("DevOps", "Инженер DevOps")),
        Map.of(11L, 4, 7L, 4, 34L, 4, 35L, 4, 36L, 3, 37L, 3, 38L, 3, 6L, 3));

    register(
        new Role(
            9L,
            "Frontend Developer (Vue)",
            "Разработка интерфейсов на Vue.js, Pinia/Nuxt и TypeScript",
            "Frontend",
            "Middle",
            List.of("Vue разработчик", "Frontend Vue")),
        Map.of(28L, 4, 29L, 3, 30L, 4, 32L, 4, 6L, 3, 12L, 3));

    register(
        new Role(
            10L,
            "Fullstack Developer",
            "Разработка клиентской и серверной части веб-приложений (JS/TS, Node.js/Java/Python, SQL)",
            "Fullstack",
            "Middle",
            List.of("Fullstack разработчик", "Full-stack разработчик")),
        Map.of(28L, 4, 29L, 3, 30L, 3, 31L, 3, 22L, 3, 3L, 3, 4L, 3, 7L, 2, 12L, 3));

    // --- Backend: другие языки ---
    register(
        new Role(
            11L,
            "Junior Python Developer",
            "Начальная backend-разработка на Python, основы Django/FastAPI и SQL",
            "Backend",
            "Junior",
            List.of("Junior Python", "Младший разработчик Python", "Стажер Python")),
        Map.of(15L, 3, 16L, 2, 3L, 2, 4L, 2, 6L, 2, 12L, 2));

    register(
        new Role(
            12L,
            "Middle Python Developer",
            "Backend-разработка на Python, FastAPI/Django, PostgreSQL, Redis, Docker",
            "Backend",
            "Middle",
            List.of("Python разработчик", "Программист Python", "Backend Python")),
        Map.of(15L, 4, 16L, 3, 17L, 4, 4L, 3, 7L, 3, 11L, 3, 12L, 4, 23L, 3));

    register(
        new Role(
            13L,
            "Go Developer",
            "Разработка высокопроизводительных микросервисов на Go (Golang), gRPC, Kafka",
            "Backend",
            "Middle",
            List.of("Golang", "Go разработчик", "Программист Go")),
        Map.of(18L, 4, 4L, 4, 7L, 3, 11L, 3, 13L, 3, 23L, 3, 25L, 3, 6L, 3));

    register(
        new Role(
            14L,
            "C# / .NET Developer",
            "Разработка серверных и корпоративных приложений на C# и ASP.NET Core",
            "Backend",
            "Middle",
            List.of("C# разработчик", "Программист C#", ".NET разработчик")),
        Map.of(19L, 4, 20L, 4, 3L, 3, 4L, 3, 7L, 2, 12L, 3, 6L, 3));

    // --- Data & AI (детализация) ---
    register(
        new Role(
            15L,
            "ML Engineer",
            "Обучение и вывод ML/DL моделей в продакшен, PyTorch, Docker, MLOps",
            "Data & AI",
            "Middle",
            List.of("ML инженер", "Инженер машинного обучения", "Machine Learning")),
        Map.of(15L, 4, 39L, 4, 40L, 4, 41L, 4, 7L, 3, 11L, 3, 6L, 3));

    register(
        new Role(
            16L,
            "Data Analyst",
            "Анализ данных, SQL, Python/Pandas, BI-дашборды и продуктовые метрики",
            "Data & AI",
            "Middle",
            List.of("Аналитик данных", "Data Analyst", "BI аналитик")),
        Map.of(3L, 4, 15L, 3, 39L, 3, 44L, 4, 27L, 2));

    register(
        new Role(
            17L,
            "Data Engineer",
            "Построение ETL/ELT-пайплайнов, хранилищ данных, Airflow, Spark, Kafka",
            "Data & AI",
            "Middle",
            List.of("Data Engineer", "Инженер данных")),
        Map.of(15L, 4, 3L, 4, 4L, 4, 13L, 3, 27L, 3, 42L, 4, 43L, 4, 7L, 3));

    // --- QA ---
    register(
        new Role(
            18L,
            "QA Engineer (Manual)",
            "Ручное тестирование веб и API, тест-дизайн, баг-репорты, Postman, SQL",
            "QA",
            "Junior",
            List.of("Тестировщик ПО", "QA инженер", "Ручное тестирование")),
        Map.of(45L, 4, 46L, 3, 3L, 2, 12L, 3, 6L, 2));

    register(
        new Role(
            19L,
            "QA Automation Engineer",
            "Автоматизация тестирования UI и API (Java/Python, Selenium/Playwright, JUnit, CI/CD)",
            "QA",
            "Middle",
            List.of("Автоматизатор тестирования", "QA Automation", "Тестировщик автоматизатор")),
        Map.of(45L, 3, 46L, 3, 47L, 4, 1L, 3, 14L, 4, 12L, 3, 6L, 3, 35L, 2));

    // --- Mobile ---
    register(
        new Role(
            20L,
            "Android Developer",
            "Нативная разработка мобильных приложений под Android на Kotlin и Java",
            "Mobile",
            "Middle",
            List.of("Android разработчик", "Программист Android", "Kotlin разработчик")),
        Map.of(10L, 4, 1L, 3, 48L, 4, 9L, 3, 12L, 3, 6L, 3));

    register(
        new Role(
            21L,
            "iOS Developer",
            "Нативная разработка мобильных приложений под iOS на Swift",
            "Mobile",
            "Middle",
            List.of("iOS разработчик", "Swift разработчик")),
        Map.of(49L, 4, 12L, 3, 6L, 3));

    // --- Analytics & Enterprise ---
    register(
        new Role(
            22L,
            "System Analyst",
            "Сбор и формализация требований, проектирование API, БД, BPMN/UML",
            "Analytics",
            "Middle",
            List.of("Системный аналитик", "System Analyst")),
        Map.of(50L, 4, 3L, 3, 12L, 4, 46L, 2));

    register(
        new Role(
            23L,
            "1C Developer",
            "Разработка и сопровождение конфигураций на платформе 1С:Предприятие",
            "Enterprise",
            "Middle",
            List.of("Программист 1С", "Разработчик 1С")),
        Map.of(51L, 4, 3L, 3, 12L, 2));
  }

  private void register(Role role, Map<Long, Integer> requiredSkills) {
    requiredSkills.entrySet().stream()
        .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
        .forEach(entry -> role.getSkills().add(
            new RoleSkill(skillCatalog.requireById(entry.getKey()), entry.getValue())));
    add(role);
  }

  public void add(Role role) {
    roles.put(role.getId(), role);
  }

  public Role findById(Long id) {
    return roles.get(id);
  }

  public List<Role> findAll() {
    return new ArrayList<>(roles.values());
  }

  public List<Role> findByCategory(String category) {
    List<Role> result = new ArrayList<>();
    for (Role role : roles.values()) {
      if (role.getCategory() != null && role.getCategory().equalsIgnoreCase(category)) {
        result.add(role);
      }
    }
    return result;
  }

  public Role findByName(String name) {
    if (name == null) return null;
    for (Role role : roles.values()) {
      if (role.getName().equalsIgnoreCase(name.trim())) {
        return role;
      }
    }
    return null;
  }
}
