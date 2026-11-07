package org.career.navigator.model;

public class RoleSkill {
  private Long skillId;
  private int requiredLevel;

  public RoleSkill(Long skillId, int requiredLevel) {
    this.skillId = skillId;
    this.requiredLevel = requiredLevel;
  }

  public Long getSkillId() {
    return skillId;
  }

  public void setSkillId(Long skillId) {
    this.skillId = skillId;
  }

  public int getRequiredLevel() {
    return requiredLevel;
  }

  public void setRequiredLevel(int requiredLevel) {
    this.requiredLevel = requiredLevel;
  }
}
