package org.career.navigator.model;

public class MissingSkill {
  private Long skillId;
  private String skillName;
  private int currentLevel;
  private int requiredLevel;

  public MissingSkill(
    Long skillId,
    String skillName,
    int currentLevel,
    int requiredLevel
  ) {
    this.skillId = skillId;
    this.skillName = skillName;
    this.currentLevel = currentLevel;
    this.requiredLevel = requiredLevel;
  }

  public Long getSkillId() {
    return skillId;
  }

  public String getSkillName() {
    return skillName;
  }

  public int getCurrentLevel() {
    return currentLevel;
  }

  public int getRequiredLevel() {
    return requiredLevel;
  }
}