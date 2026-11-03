package org.career.navigator.model;

public class SkillTestResult {
  private Long userId;
  private Skill skill;
  private int correctAnswers;
  private int totalQuestions;
  private boolean passed;
  private int confirmedLevel;

  public SkillTestResult(
    Long userId,
    Skill skill,
    int correctAnswers,
    int totalQuestions,
    boolean passed,
    int confirmedLevel
  ) {
    this.userId = userId;
    this.skill = skill;
    this.correctAnswers = correctAnswers;
    this.totalQuestions = totalQuestions;
    this.passed = passed;
    this.confirmedLevel = confirmedLevel;
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

  public int getCorrectAnswers() {
    return correctAnswers;
  }

  public void setCorrectAnswers(int correctAnswers) {
    this.correctAnswers = correctAnswers;
  }

  public int getTotalQuestions() {
    return totalQuestions;
  }

  public void setTotalQuestions(int totalQuestions) {
    this.totalQuestions = totalQuestions;
  }

  public boolean isPassed() {
    return passed;
  }

  public void setPassed(boolean passed) {
    this.passed = passed;
  }

  public int getConfirmedLevel() {
    return confirmedLevel;
  }

  public void setConfirmedLevel(int confirmedLevel) {
    this.confirmedLevel = confirmedLevel;
  }
}