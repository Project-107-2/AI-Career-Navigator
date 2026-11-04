package org.career.navigator.model;

import java.util.List;

public class MatchResult {
  private Long roleId;
  private double matchPercentage;
  private List<MissingSkill> missingSkills;

  public MatchResult(
    Long roleId,
    double matchPercentage,
    List<MissingSkill> missingSkills
  ) {
    this.roleId = roleId;
    this.matchPercentage = matchPercentage;
    this.missingSkills = missingSkills;
  }

  public Long getRoleId() {
    return roleId;
  }

  public double getMatchPercentage() {
    return matchPercentage;
  }

  public List<MissingSkill> getMissingSkills() {
    return missingSkills;
  }
}