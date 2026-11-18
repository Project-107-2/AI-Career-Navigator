package org.career.navigator.service;
import org.career.navigator.dto.*;

import org.career.navigator.model.*;
import org.career.navigator.service.MatchService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchServiceTest {

  private final MatchService matchService = new MatchService();

  @Test
  void calculateMatch_whenAllSkillsMeetRequirements_returns100Percent() {

    Skill java = new Skill(1L, "Java", "Programming language");
    Skill sql = new Skill(2L, "SQL", "Database query language");

    Role role = new Role(1L, "Java Developer", "Backend developer");

    role.getSkills().add(new RoleSkill(java, 4));
    role.getSkills().add(new RoleSkill(sql, 3));

    List<UserSkillAssessment> assessments = List.of(
      new UserSkillAssessment(10L, java, 4),
      new UserSkillAssessment(10L, sql, 3)
    );

    List<SkillTestResult> testResults = List.of();

    MatchResult result = matchService.calculateMatch(
      10L, role, assessments, testResults
    );

    assertEquals(100.0, result.getMatchPercentage(), 0.001);
    assertTrue(result.getMissingSkills().isEmpty());
  }

  @Test
  void calculateMatch_whenSomeSkillsAreMissing_returnsPartialMatch() {

    Skill java = new Skill(1L, "Java", "Programming language");
    Skill sql = new Skill(2L, "SQL", "Database query language");

    Role role = new Role(1L, "Java Developer", "Backend developer");

    role.getSkills().add(new RoleSkill(java, 4));
    role.getSkills().add(new RoleSkill(sql, 3));

    List<UserSkillAssessment> assessments = List.of(
      new UserSkillAssessment(10L, java, 4),
      new UserSkillAssessment(10L, sql, 1)
    );

    List<SkillTestResult> testResults = List.of();

    MatchResult result = matchService.calculateMatch(
      10L, role, assessments, testResults
    );

    assertEquals(71.428, result.getMatchPercentage(), 0.01);

    assertEquals(1, result.getMissingSkills().size());

    MissingSkill missing = result.getMissingSkills().get(0);

    assertEquals("SQL", missing.getSkillName());
    assertEquals(1, missing.getCurrentLevel());
    assertEquals(3, missing.getRequiredLevel());
  }
  @Test
  void calculateMatch_whenUserHasNoSkills_returnsZeroPercent() {

    Skill java = new Skill(1L, "Java", "Programming language");
    Skill sql = new Skill(2L, "SQL", "Database query language");

    Role role = new Role(1L, "Java Developer", "Backend developer");

    role.getSkills().add(new RoleSkill(java, 4));
    role.getSkills().add(new RoleSkill(sql, 3));

    List<UserSkillAssessment> assessments = List.of();

    List<SkillTestResult> testResults = List.of();

    MatchResult result = matchService.calculateMatch(
      10L, role, assessments, testResults
    );

    assertEquals(0.0, result.getMatchPercentage(), 0.001);
    assertEquals(2, result.getMissingSkills().size());
  }
  @Test
  void calculateMatch_whenAssessmentBelongsToAnotherUser_ignoresIt() {

    Skill java = new Skill(1L, "Java", "Programming language");

    Role role = new Role(1L, "Java Developer", "Backend developer");
    role.getSkills().add(new RoleSkill(java, 4));

    List<UserSkillAssessment> assessments = List.of(
      new UserSkillAssessment(20L, java, 4)
    );

    List<SkillTestResult> testResults = List.of();

    MatchResult result = matchService.calculateMatch(
      10L, role, assessments, testResults
    );

    assertEquals(0.0, result.getMatchPercentage(), 0.001);
    assertEquals(1, result.getMissingSkills().size());
  }
  @Test
  void calculateMatch_whenTestIsFailed_ignoresTestResult() {

    Skill java = new Skill(1L, "Java", "Programming language");

    Role role = new Role(1L, "Java Developer", "Backend developer");
    role.getSkills().add(new RoleSkill(java, 4));

    List<UserSkillAssessment> assessments = List.of(
      new UserSkillAssessment(10L, java, 4)
    );

    List<SkillTestResult> testResults = List.of(
      new SkillTestResult(10L, java, 0, 5, false, 0)
    );

    MatchResult result = matchService.calculateMatch(
      10L, role, assessments, testResults
    );

    assertEquals(100.0, result.getMatchPercentage(), 0.001);
    assertTrue(result.getMissingSkills().isEmpty());
  }

  @Test
  void calculateMatch_whenTestPassedWithoutAssessment_usesTestedLevel() {

    Skill java = new Skill(1L, "Java", "Programming language");

    Role role = new Role(1L, "Java Developer", "Backend developer");
    role.getSkills().add(new RoleSkill(java, 4));

    List<UserSkillAssessment> assessments = List.of();

    List<SkillTestResult> testResults = List.of(
      new SkillTestResult(10L, java, 5, 5, true, 4)
    );

    MatchResult result = matchService.calculateMatch(
      10L, role, assessments, testResults
    );

    assertEquals(100.0, result.getMatchPercentage(), 0.001);
    assertTrue(result.getMissingSkills().isEmpty());
  }

  @Test
  void calculateMatch_whenMultipleTestsPassed_usesHighestLevel() {

    Skill java = new Skill(1L, "Java", "Programming language");

    Role role = new Role(1L, "Java Developer", "Backend developer");
    role.getSkills().add(new RoleSkill(java, 4));

    List<UserSkillAssessment> assessments = List.of();

    List<SkillTestResult> testResults = List.of(
      new SkillTestResult(10L, java, 3, 5, true, 2),
      new SkillTestResult(10L, java, 5, 5, true, 4)
    );

    MatchResult result = matchService.calculateMatch(
      10L, role, assessments, testResults
    );

    assertEquals(100.0, result.getMatchPercentage(), 0.001);
    assertTrue(result.getMissingSkills().isEmpty());
  }
}

