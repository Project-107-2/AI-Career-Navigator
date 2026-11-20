package org.career.navigator.repository;

import org.career.navigator.Database;
import org.career.navigator.dto.RoleStats;
import org.career.navigator.dto.SkillCount;
import org.career.navigator.model.Skill;
import org.career.navigator.model.Vacancy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC-репозиторий для сохранения вакансий (vacancies, vacancy_skills) и ежедневных снимков
 * рыночной статистики (role_stats_snapshots, role_stats_snapshot_skills) в PostgreSQL 18.
 */
public class PostgresVacancyRepository implements VacancyStorage {

  private static final int MAX_EXTERNAL_ID_LEN = 120;
  private static final int MAX_TITLE_LEN = 250;
  private static final int MAX_CURRENCY_LEN = 20;

  @Override
  public void saveVacancies(Long roleId, String source, List<Vacancy> vacancies) {
    String cleanSource = (source == null || source.isBlank()) ? "trudvsem" : source.trim();
    String deleteDemoSql =
        "DELETE FROM vacancies WHERE role_id = ? AND source = 'demo'";
    String upsertVacancySql =
        "INSERT INTO vacancies "
            + "(role_id, external_id, source, title, employer, salary_from, salary_to, currency, fetched_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW()) "
            + "ON CONFLICT (role_id, source, external_id) DO UPDATE SET "
            + "title = EXCLUDED.title, "
            + "salary_from = EXCLUDED.salary_from, "
            + "salary_to = EXCLUDED.salary_to, "
            + "currency = EXCLUDED.currency, "
            + "fetched_at = NOW() "
            + "RETURNING id";
    String deleteSkillsSql = "DELETE FROM vacancy_skills WHERE vacancy_id = ?";
    String insertSkillSql =
        "INSERT INTO vacancy_skills (vacancy_id, skill_id) VALUES (?, ?) ON CONFLICT DO NOTHING";

    try (Connection connection = Database.connect()) {
      connection.setAutoCommit(false);
      try {
        if (!vacancies.isEmpty() && !"demo".equals(cleanSource)) {
          try (PreparedStatement deleteDemo = connection.prepareStatement(deleteDemoSql)) {
            deleteDemo.setLong(1, roleId);
            deleteDemo.executeUpdate();
          }
        }

        try (PreparedStatement upsertVacancy = connection.prepareStatement(upsertVacancySql);
             PreparedStatement deleteSkills = connection.prepareStatement(deleteSkillsSql);
             PreparedStatement insertSkill = connection.prepareStatement(insertSkillSql)) {

          for (Vacancy v : vacancies) {
            if (v.id() == null || v.id().isBlank()) {
              continue;
            }
            upsertVacancy.setLong(1, roleId);
            upsertVacancy.setString(2, truncate(v.id(), MAX_EXTERNAL_ID_LEN));
            upsertVacancy.setString(3, cleanSource);
            upsertVacancy.setString(4, truncate(
                v.name() == null || v.name().isBlank() ? "Без названия" : v.name(),
                MAX_TITLE_LEN));
            upsertVacancy.setNull(5, Types.VARCHAR);
            setSalary(upsertVacancy, 6, v.salaryFrom());
            setSalary(upsertVacancy, 7, v.salaryTo());
            upsertVacancy.setString(8, truncate(
                v.currency() == null || v.currency().isBlank() ? "RUR" : v.currency(),
                MAX_CURRENCY_LEN));

            long vacancyDbId;
            try (ResultSet rs = upsertVacancy.executeQuery()) {
              if (!rs.next()) {
                continue;
              }
              vacancyDbId = rs.getLong(1);
            }

            deleteSkills.setLong(1, vacancyDbId);
            deleteSkills.executeUpdate();

            if (v.skills() != null && !v.skills().isEmpty()) {
              for (Skill skill : v.skills()) {
                insertSkill.setLong(1, vacancyDbId);
                insertSkill.setLong(2, skill.getId());
                insertSkill.addBatch();
              }
              insertSkill.executeBatch();
            }
          }
        }
        connection.commit();
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to save vacancies for roleId=" + roleId, e);
    }
  }

  @Override
  public void saveSnapshot(Long roleId, LocalDate snapshotDate, RoleStats stats) {
    String upsertSnapshotSql =
        "INSERT INTO role_stats_snapshots "
            + "(role_id, snapshot_date, vacancy_count, salary_median, currency, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, NOW()) "
            + "ON CONFLICT (role_id, snapshot_date) DO UPDATE SET "
            + "vacancy_count = EXCLUDED.vacancy_count, "
            + "salary_median = EXCLUDED.salary_median, "
            + "currency = EXCLUDED.currency, "
            + "updated_at = NOW() "
            + "RETURNING id";
    String deleteSkillsSql = "DELETE FROM role_stats_snapshot_skills WHERE snapshot_id = ?";
    String insertSkillSql =
        "INSERT INTO role_stats_snapshot_skills (snapshot_id, skill_id, vacancy_count, share) "
            + "VALUES (?, ?, ?, ?)";

    try (Connection connection = Database.connect()) {
      connection.setAutoCommit(false);
      try {
        long snapshotId;
        try (PreparedStatement stmt = connection.prepareStatement(upsertSnapshotSql)) {
          stmt.setLong(1, roleId);
          stmt.setDate(2, Date.valueOf(snapshotDate));
          stmt.setInt(3, stats.vacancyCount());
          if (stats.salaryMedian() == null) {
            stmt.setNull(4, Types.INTEGER);
          } else {
            stmt.setInt(4, stats.salaryMedian());
          }
          stmt.setString(5, truncate(
              stats.currency() == null || stats.currency().isBlank() ? "RUR" : stats.currency(),
              MAX_CURRENCY_LEN));
          try (ResultSet rs = stmt.executeQuery()) {
            if (!rs.next()) {
              throw new SQLException("No snapshot id returned for roleId=" + roleId);
            }
            snapshotId = rs.getLong(1);
          }
        }

        try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSkillsSql)) {
          deleteStmt.setLong(1, snapshotId);
          deleteStmt.executeUpdate();
        }

        if (stats.skills() != null && !stats.skills().isEmpty()) {
          try (PreparedStatement insertStmt = connection.prepareStatement(insertSkillSql)) {
            for (SkillCount sc : stats.skills()) {
              insertStmt.setLong(1, snapshotId);
              insertStmt.setLong(2, sc.skillId());
              insertStmt.setInt(3, sc.count());
              insertStmt.setDouble(4, sc.share());
              insertStmt.addBatch();
            }
            insertStmt.executeBatch();
          }
        }
        connection.commit();
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to save stats snapshot for roleId=" + roleId, e);
    }
  }

  @Override
  public Optional<RoleStats> findLatestSnapshot(Long roleId) {
    String sql =
        "SELECT id, role_id, vacancy_count, salary_median, currency "
            + "FROM role_stats_snapshots "
            + "WHERE role_id = ? "
            + "ORDER BY snapshot_date DESC LIMIT 1";
    try (Connection connection = Database.connect();
         PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, roleId);
      try (ResultSet rs = stmt.executeQuery()) {
        if (!rs.next()) {
          return Optional.empty();
        }
        long snapshotId = rs.getLong("id");
        List<SkillCount> skills = loadSnapshotSkills(connection, snapshotId);
        return Optional.of(new RoleStats(
            rs.getLong("role_id"),
            rs.getInt("vacancy_count"),
            getNullableInt(rs, "salary_median"),
            rs.getString("currency"),
            skills));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to load latest snapshot for roleId=" + roleId, e);
    }
  }

  @Override
  public Map<String, RoleStats> findDailySnapshots(Long roleId) {
    String sql =
        "SELECT id, role_id, snapshot_date, vacancy_count, salary_median, currency "
            + "FROM role_stats_snapshots "
            + "WHERE role_id = ? "
            + "ORDER BY snapshot_date ASC";
    Map<String, RoleStats> result = new LinkedHashMap<>();
    try (Connection connection = Database.connect();
         PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, roleId);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          long snapshotId = rs.getLong("id");
          String dateKey = rs.getDate("snapshot_date").toLocalDate().toString();
          List<SkillCount> skills = loadSnapshotSkills(connection, snapshotId);
          result.put(
              dateKey,
              new RoleStats(
                  rs.getLong("role_id"),
                  rs.getInt("vacancy_count"),
                  getNullableInt(rs, "salary_median"),
                  rs.getString("currency"),
                  skills));
        }
      }
      return Collections.unmodifiableMap(result);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to load daily snapshots for roleId=" + roleId, e);
    }
  }

  @Override
  public int countCachedRoles() {
    return queryCount("SELECT COUNT(DISTINCT role_id) FROM role_stats_snapshots");
  }

  @Override
  public int countTotalVacancies() {
    return queryCount("SELECT COUNT(*) FROM vacancies");
  }

  @Override
  public String storageName() {
    return "postgresql";
  }

  private List<SkillCount> loadSnapshotSkills(Connection connection, long snapshotId)
      throws SQLException {
    String sql =
        "SELECT ss.skill_id, s.name, ss.vacancy_count, ss.share "
            + "FROM role_stats_snapshot_skills ss "
            + "JOIN skills s ON s.id = ss.skill_id "
            + "WHERE ss.snapshot_id = ? "
            + "ORDER BY ss.vacancy_count DESC, s.id ASC";
    List<SkillCount> skills = new ArrayList<>();
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, snapshotId);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          skills.add(new SkillCount(
              rs.getLong("skill_id"),
              rs.getString("name"),
              rs.getInt("vacancy_count"),
              rs.getDouble("share")));
        }
      }
    }
    return skills;
  }

  private int queryCount(String sql) {
    try (Connection connection = Database.connect();
         PreparedStatement stmt = connection.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      return rs.next() ? rs.getInt(1) : 0;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to execute count query", e);
    }
  }

  private static void setSalary(PreparedStatement stmt, int idx, Integer value) throws SQLException {
    if (value == null || value < 0) {
      stmt.setNull(idx, Types.NUMERIC);
    } else {
      stmt.setBigDecimal(idx, BigDecimal.valueOf(value));
    }
  }

  private static Integer getNullableInt(ResultSet rs, String col) throws SQLException {
    Object val = rs.getObject(col);
    return val instanceof Number n ? n.intValue() : null;
  }

  private static String truncate(String s, int maxLen) {
    if (s == null) {
      return "";
    }
    return s.length() <= maxLen ? s : s.substring(0, maxLen);
  }
}
