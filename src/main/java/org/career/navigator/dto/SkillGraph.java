package org.career.navigator.dto;

import java.util.List;

/** Ответ GET /skills/graph: навыки (вершины) и связи «что учить раньше чего» (рёбра). */
public record SkillGraph(List<Node> nodes, List<Edge> edges) {

  /** Навык. */
  public record Node(Long id, String name, String description) {}

  /** Связь: навык from нужно изучить до навыка to. */
  public record Edge(Long from, Long to) {}
}