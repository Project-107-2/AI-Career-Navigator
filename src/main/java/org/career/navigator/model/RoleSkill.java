package org.career.navigator.model;

public class RoleSkill {
  private Skill skill;
  private int requiredLevel;

  public RoleSkill(Skill skill, int requiredLevel) {
    this.skill = skill;
    this.requiredLevel = requiredLevel;
  }

  public Skill getSkill() {
    return skill;
  }

  public void setSkill(Skill skill) {
    this.skill = skill;
  }

  public int getRequiredLevel() {
    return requiredLevel;
  }

  public void setRequiredLevel(int requiredLevel) {
    this.requiredLevel = requiredLevel;
  }
}