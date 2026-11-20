package org.career.navigator.repository;

import org.career.navigator.model.Role;
import org.career.navigator.model.Skill;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SqlSchemaAndSeedTest {

  @Test
  void schemaContainsAllRequiredTables() throws Exception {
    String schema = Files.readString(Path.of("sql/01-schema.sql"), StandardCharsets.UTF_8);

    assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS roles"));
    assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS skills"));
    assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS role_skills"));
    assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS role_search_queries"));
    assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS skill_synonyms"));
    assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS vacancies"));
    assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS vacancy_skills"));
    assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS role_stats_snapshots"));
    assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS role_stats_snapshot_skills"));
    assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS users"));
    assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS user_skill_assessments"));
  }

  @Test
  void seedContainsAllCatalogSkillsAndRoles() throws Exception {
    String seed = Files.readString(Path.of("sql/02-seed.sql"), StandardCharsets.UTF_8);
    SkillCatalog catalog = new SkillCatalog();
    RoleRepository roles = new RoleRepository(catalog);

    for (Skill skill : catalog.all()) {
      assertTrue(
          seed.contains("'" + skill.getName().replace("'", "''") + "'"),
          "Навык отсутствует в 02-seed.sql: " + skill.getName());
    }
    for (Role role : roles.findAll()) {
      assertTrue(
          seed.contains("'" + role.getName().replace("'", "''") + "'"),
          "Роль отсутствует в 02-seed.sql: " + role.getName());
    }
  }
}
