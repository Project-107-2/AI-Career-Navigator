package org.career.navigator.repository;

import org.career.navigator.dto.RoleStats;
import org.career.navigator.model.Vacancy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Абстракция хранения собранных вакансий и дневных снимков статистики по ролям. */
public interface VacancyStorage {

  void saveVacancies(Long roleId, String source, List<Vacancy> vacancies);

  void saveSnapshot(Long roleId, LocalDate snapshotDate, RoleStats stats);

  Optional<RoleStats> findLatestSnapshot(Long roleId);

  Map<String, RoleStats> findDailySnapshots(Long roleId);

  int countCachedRoles();

  int countTotalVacancies();

  String storageName();
}
