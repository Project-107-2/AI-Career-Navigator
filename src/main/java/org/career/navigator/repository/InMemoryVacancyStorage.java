package org.career.navigator.repository;

import org.career.navigator.dto.RoleStats;
import org.career.navigator.model.Vacancy;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/** Временное in-memory хранилище вакансий и снимков статистики (используется в оффлайн unit-тестах). */
public class InMemoryVacancyStorage implements VacancyStorage {

  private final Map<Long, List<Vacancy>> vacancyCache = new ConcurrentHashMap<>();
  private final Map<Long, RoleStats> latestStats = new ConcurrentHashMap<>();
  private final Map<Long, Map<String, RoleStats>> dailySnapshots = new ConcurrentHashMap<>();

  @Override
  public void saveVacancies(Long roleId, String source, List<Vacancy> vacancies) {
    vacancyCache.put(roleId, List.copyOf(vacancies));
  }

  @Override
  public void saveSnapshot(Long roleId, LocalDate snapshotDate, RoleStats stats) {
    latestStats.put(roleId, stats);
    dailySnapshots
        .computeIfAbsent(roleId, ignored -> new ConcurrentHashMap<>())
        .put(snapshotDate.toString(), stats);
  }

  @Override
  public Optional<RoleStats> findLatestSnapshot(Long roleId) {
    return Optional.ofNullable(latestStats.get(roleId));
  }

  @Override
  public Map<String, RoleStats> findDailySnapshots(Long roleId) {
    Map<String, RoleStats> history = dailySnapshots.get(roleId);
    if (history == null) {
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(new LinkedHashMap<>(new TreeMap<>(history)));
  }

  @Override
  public int countCachedRoles() {
    return latestStats.size();
  }

  @Override
  public int countTotalVacancies() {
    return vacancyCache.values().stream().mapToInt(List::size).sum();
  }

  @Override
  public String storageName() {
    return "in-memory";
  }
}
