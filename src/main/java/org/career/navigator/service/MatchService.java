package org.career.navigator.service;

import org.career.navigator.model.Role;
import org.career.navigator.model.RoleSkill;
import org.career.navigator.model.UserSkillAssessment;
import org.career.navigator.model.SkillTestResult;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MatchService {

  public double calculateMatch(
    Role role,
    List<UserSkillAssessment> assessments,
    List<SkillTestResult> testResults
  ) {
    Map<Long, Integer> userLevels = new HashMap<>();

    for (UserSkillAssessment assessment : assessments) {
      userLevels.put(
        assessment.getSkill().getId(),
        assessment.getLevel()
      );
    }

    for (SkillTestResult result : testResults) {
      if (result.isPassed()) {
        Long skillId = result.getSkill().getId();

        int selfAssessedLevel = userLevels.getOrDefault(
          skillId,
          result.getConfirmedLevel()
        );

        userLevels.put(
          skillId,
          Math.min(selfAssessedLevel, result.getConfirmedLevel())
        );
      }
    }

    int totalRequired = 0;
    int totalAchieved = 0;

    for (RoleSkill roleSkill : role.getSkills()) {
      int required = roleSkill.getRequiredLevel();

      int actual = userLevels.getOrDefault(
        roleSkill.getSkill().getId(), 0
      );

      totalRequired += required;
      totalAchieved += Math.min(actual, required);
    }

    if (totalRequired == 0) {
      return 0.0;
    }

    return (double) totalAchieved / totalRequired * 100;
  }
}