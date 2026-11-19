package org.career.navigator.repository;

import org.career.navigator.model.Role;
import org.career.navigator.model.Skill;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SkillCatalogTest {

  private final SkillCatalog catalog = new SkillCatalog();
  private final RoleRepository roles = new RoleRepository(catalog);

  @Test
  void findIn_recognizesRussianAndEnglishSynonymsWithoutDuplicates() {
    String vacancyText = "Ищем разработчика: Постгрес, PostgreSQL, Spring Boot, K8s, Докер и Кафка.";
    List<String> names = catalog.findIn(vacancyText).stream().map(Skill::getName).toList();

    assertTrue(names.containsAll(List.of("PostgreSQL", "Spring", "Kubernetes", "Docker", "Kafka")));
    assertEquals(1, names.stream().filter("PostgreSQL"::equals).count());
  }

  @Test
  void findIn_respectsWordBoundariesForJavaAndJsAndSpecialSymbols() {
    List<String> jsOnly = catalog.findIn("Нужен опыт с Node.js и JavaScript, без бэкенда.")
        .stream().map(Skill::getName).toList();
    assertFalse(jsOnly.contains("Java"));
    assertTrue(jsOnly.contains("Node.js"));
    assertTrue(jsOnly.contains("JavaScript"));

    List<String> cFamily = catalog.findIn("Опыт коммерческой разработки на C++20 и C#/.NET Core.")
        .stream().map(Skill::getName).toList();
    assertTrue(cFamily.containsAll(List.of("C++", "C#", ".NET")));
  }

  @Test
  void normalize_resolvesAliasesToCanonicalSkill() {
    assertEquals("PostgreSQL", catalog.normalize("Постгрес").orElseThrow().getName());
    assertEquals("Kubernetes", catalog.normalize("k8s").orElseThrow().getName());
    assertTrue(catalog.normalize("несуществующий-навык").isEmpty());
  }

  @Test
  void roleRepository_preservesFrontendBaseRolesAndPopulatesSkills() {
    assertEquals("Java Developer", roles.findById(1L).getName());
    assertNotNull(roles.findByName("Frontend Developer"));
    assertNotNull(roles.findByName("Data Scientist"));
    assertNotNull(roles.findByName("DevOps Engineer"));
    assertTrue(roles.findAll().size() >= 20);

    for (Role role : roles.findAll()) {
      assertFalse(role.getSkills().isEmpty(), "У роли должны быть навыки: " + role.getName());
      assertFalse(role.getSearchQueries().isEmpty(), "У роли должны быть запросы: " + role.getName());
    }
  }
}
