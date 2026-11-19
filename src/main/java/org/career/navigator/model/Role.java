package org.career.navigator.model;

import java.util.ArrayList;
import java.util.List;

public class Role {
  private Long id;
  private String name;
  private String description;
  private String category;
  private String grade;
  private List<String> searchQueries;
  private List<RoleSkill> skills;

  public Role(Long id, String name, String description) {
    this(id, name, description, "General", "", List.of(name));
  }

  public Role(
      Long id,
      String name,
      String description,
      String category,
      String grade,
      List<String> searchQueries) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.category = category;
    this.grade = grade;
    this.searchQueries = new ArrayList<>(searchQueries);
    this.skills = new ArrayList<>();
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

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getGrade() {
    return grade;
  }

  public void setGrade(String grade) {
    this.grade = grade;
  }

  public List<String> getSearchQueries() {
    return searchQueries;
  }

  public void setSearchQueries(List<String> searchQueries) {
    this.searchQueries = searchQueries;
  }

  public List<RoleSkill> getSkills() {
    return skills;
  }

  public void setSkills(List<RoleSkill> skills) {
    this.skills = skills;
  }
}
