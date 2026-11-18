package org.career.navigator.service;

import org.career.navigator.model.*;

import java.util.*;

public class MatchService {

  public MatchResult calculateMatch(
    Long userId,
    Role role,
    List<UserSkillAssessment> assessments,
    List<SkillTestResult> testResults
  ) {
    Map<Long, Integer> userLevels = new HashMap<>();

    // Самооценка пользователя
    for (UserSkillAssessment assessment : assessments) {
      if (!userId.equals(assessment.getUserId())) {
        continue;
      }

      userLevels.put(
        assessment.getSkill().getId(),
        assessment.getLevel()
      );
    }

    // Лучший подтверждённый уровень по результатам тестов
    Map<Long, Integer> testedLevels = new HashMap<>();

    for (SkillTestResult result : testResults) {
      if (!userId.equals(result.getUserId()) || !result.isPassed()) {
        continue;
      }

      Long skillId = result.getSkill().getId();

      testedLevels.merge(
        skillId,
        result.getConfirmedLevel(),
        Math::max
      );
    }

    // Сопоставляем самооценку и тесты
    for (Map.Entry<Long, Integer> entry : testedLevels.entrySet()) {
      Long skillId = entry.getKey();
      int testedLevel = entry.getValue();

      int selfLevel = userLevels.getOrDefault(
        skillId,
        testedLevel
      );

      userLevels.put(
        skillId,
        Math.min(selfLevel, testedLevel)
      );
    }

    int totalRequired = 0;
    int totalAchieved = 0;

    List<MissingSkill> missingSkills = new ArrayList<>();

    // Рассчитываем соответствие роли
    for (RoleSkill roleSkill : role.getSkills()) {
      int required = roleSkill.getRequiredLevel();
      Long skillId = roleSkill.getSkill().getId();

      int actual = userLevels.getOrDefault(skillId, 0);

      totalRequired += required;
      totalAchieved += Math.min(actual, required);

      if (actual < required) {
        missingSkills.add(
          new MissingSkill(
            skillId,
            roleSkill.getSkill().getName(),
            actual,
            required
          )
        );
      }
    }

    double percentage = totalRequired == 0
      ? 0.0
      : (double) totalAchieved / totalRequired * 100;

    return new MatchResult(
      role.getId(),
      percentage,
      missingSkills
    );
  }
}