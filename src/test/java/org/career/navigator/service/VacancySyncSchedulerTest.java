package org.career.navigator.service;

import org.career.navigator.dto.RoleStats;
import org.career.navigator.model.Role;
import org.career.navigator.model.Skill;
import org.career.navigator.model.Vacancy;
import org.career.navigator.repository.RoleRepository;
import org.career.navigator.repository.SkillCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class VacancySyncSchedulerTest {

  private final SkillCatalog catalog = new SkillCatalog();
  private final RoleRepository roles = new RoleRepository(catalog);
  private final StatsService statsService = new StatsService();

  @Test
  void syncAllNow_cachesStatsAndStoresDailySnapshots() throws Exception {
    Skill javaSkill = catalog.requireById(1L);
    VacancySyncScheduler scheduler = new VacancySyncScheduler(
        roles,
        statsService,
        role -> List.of(new Vacancy("v-" + role.getId(), role.getName(), List.of(javaSkill), 150000, 150000, "RUR")),
        0L,
        60_000L,
        0L);

    scheduler.syncAllNow();

    Role javaRole = roles.findById(1L);
    RoleStats stats = scheduler.getOrFetchStats(javaRole);
    assertEquals(1, stats.vacancyCount());
    assertEquals(150000, stats.salaryMedian());

    Map<String, RoleStats> snapshots = scheduler.getDailySnapshots(1L);
    assertTrue(snapshots.containsKey(LocalDate.now().toString()));
    assertEquals(roles.findAll().size(), scheduler.getStatus().get("cachedRoles"));
    assertEquals(0, scheduler.getStatus().get("lastSyncErrors"));
  }

  @Test
  void syncAllNow_continuesWhenSingleRoleFails() {
    VacancySyncScheduler scheduler = new VacancySyncScheduler(
        roles,
        statsService,
        role -> {
          if (role.getId() == 1L) {
            throw new IOException("trudvsem timeout");
          }
          return List.of();
        },
        0L,
        60_000L,
        0L);

    scheduler.syncAllNow();

    assertEquals(1, scheduler.getStatus().get("lastSyncErrors"));
    assertEquals(roles.findAll().size() - 1, scheduler.getStatus().get("cachedRoles"));
  }

  @Test
  void start_runsPeriodicallyInBackgroundAndStopsCleanly() throws Exception {
    AtomicInteger calls = new AtomicInteger(0);
    VacancySyncScheduler scheduler = new VacancySyncScheduler(
        roles,
        statsService,
        role -> {
          calls.incrementAndGet();
          return List.of();
        },
        0L,
        50L,
        0L);

    scheduler.start();
    try {
      Thread.sleep(160L);
    } finally {
      scheduler.stop();
    }

    assertTrue(calls.get() >= roles.findAll().size(), "Планировщик должен успеть выполнить хотя бы один проход");
    assertNotNull(scheduler.getStatus().get("lastSyncFinishedAt"));
  }
}
