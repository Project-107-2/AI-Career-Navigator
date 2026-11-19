package org.career.navigator.model;

import java.util.Objects;

public class Skill {
  private Long id;
  private String name;
  private String description;

  public Skill(Long id, String name, String description) {
    this.id = id;
    this.name = name;
    this.description = description;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Skill skill)) return false;
    return Objects.equals(id, skill.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
