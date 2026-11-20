package org.career.navigator.service;

import org.career.navigator.dto.SkillGraph;
import org.career.navigator.model.Skill;
import org.career.navigator.repository.SkillCatalog;

import java.util.ArrayList;
import java.util.List;

/**
 * Граф навыков: какие навыки нужно изучить раньше других.
 * Связи пока заданы в коде; позже их можно перенести в таблицу skill_prerequisite.
 */
public class SkillGraphService {

  // {откуда, куда}: навык с первым id изучается до навыка со вторым id.
  // id навыков — из SkillCatalog: 1 Java, 2 Spring, 3 SQL, 4 PostgreSQL, 5 Hibernate,
  // 6 Git, 7 Docker, 8 Maven, 9 Gradle, 10 Kotlin, 11 Linux, 12 REST, 13 Kafka, 14 JUnit
  private static final long[][] PREREQUISITES = {
    {1, 2},   // Java -> Spring
    {1, 8},   // Java -> Maven
    {1, 9},   // Java -> Gradle
    {1, 10},  // Java -> Kotlin
    {1, 14},  // Java -> JUnit
    {1, 5},   // Java -> Hibernate
    {3, 4},   // SQL -> PostgreSQL
    {3, 5},   // SQL -> Hibernate
    {2, 12},  // Spring -> REST
    {2, 13},  // Spring -> Kafka
    {11, 7},  // Linux -> Docker
  };

  private final SkillCatalog catalog;

  public SkillGraphService(SkillCatalog catalog) {
    this.catalog = catalog;
  }

  public SkillGraph graph() {
    List<SkillGraph.Node> nodes = new ArrayList<>();
    for (Skill s : catalog.all()) {
      nodes.add(new SkillGraph.Node(s.getId(), s.getName(), s.getDescription()));
    }

    List<SkillGraph.Edge> edges = new ArrayList<>();
    for (long[] p : PREREQUISITES) {
      edges.add(new SkillGraph.Edge(p[0], p[1]));
    }
    return new SkillGraph(nodes, edges);
  }
}