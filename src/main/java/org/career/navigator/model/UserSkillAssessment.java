package org.career.navigator.model;

public class UserSkillAssessment {
  private Long userId;
  private Skill skill;
  private int level;

  public UserSkillAssessment(Long userId, Skill skill, int level) {
    this.userId = userId;
    this.skill = skill;
    this.level = level;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Skill getSkill() {
    return skill;
  }

  public void setSkill(Skill skill) {
    this.skill = skill;
  }

  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }
}