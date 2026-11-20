package org.career.navigator.service;

import org.career.navigator.dto.RoleStats;
import org.career.navigator.integration.TrudvsemClient;
import org.career.navigator.model.Role;
import org.career.navigator.model.Vacancy;
import org.career.navigator.repository.InMemoryVacancyStorage;
import org.career.navigator.repository.RoleRepository;
import org.career.navigator.repository.VacancyStorage;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Фоновый планировщик обновления вакансий и рыночной статистики с trudvsem.ru по расписанию.
 * Сохраняет собранные вакансии и ежедневные снимки статистики через VacancyStorage
 * (в рабочем режиме — PostgreSQL 18 через JDBC).
 */
public class VacancySyncScheduler {

  @FunctionalInterface
  public interface VacancyFetcher {
    List<Vacancy> fetch(Role role) throws IOException, InterruptedException;
  }

  private static final long DEFAULT_INTERVAL_HOURS = 6L;
  private static final long DEFAULT_INITIAL_DELAY_MS = 2_000L;
  private static final long DEFAULT_PAUSE_BETWEEN_ROLES_MS = 250L;

  private final RoleRepository roleRepository;
  private final StatsService statsService;
  private final VacancyFetcher fetcher;
  private final VacancyStorage storage;
  private final long initialDelayMs;
  private final long intervalMs;
  private final long pauseBetweenRolesMs;

  private final AtomicBoolean syncing = new AtomicBoolean(false);
  private volatile Instant lastSyncStartedAt;
  private volatile Instant lastSyncFinishedAt;
  private volatile int lastSyncErrors;
  private ScheduledExecutorService executor;

  public VacancySyncScheduler(
      RoleRepository roleRepository,
      TrudvsemClient trudvsemClient,
      StatsService statsService,
      VacancyStorage storage) {
    this(
        roleRepository,
        statsService,
        role -> trudvsemClient.searchAll(role.getSearchQueries(), 50),
        storage,
        DEFAULT_INITIAL_DELAY_MS,
        TimeUnit.HOURS.toMillis(resolveIntervalHours()),
        DEFAULT_PAUSE_BETWEEN_ROLES_MS);
  }

  public VacancySyncScheduler(
      RoleRepository roleRepository,
      TrudvsemClient trudvsemClient,
      StatsService statsService) {
    this(roleRepository, trudvsemClient, statsService, new InMemoryVacancyStorage());
  }

  public VacancySyncScheduler(
      RoleRepository roleRepository,
      StatsService statsService,
      VacancyFetcher fetcher,
      long initialDelayMs,
      long intervalMs,
      long pauseBetweenRolesMs) {
    this(
        roleRepository,
        statsService,
        fetcher,
        new InMemoryVacancyStorage(),
        initialDelayMs,
        intervalMs,
        pauseBetweenRolesMs);
  }

  public VacancySyncScheduler(
      RoleRepository roleRepository,
      StatsService statsService,
      VacancyFetcher fetcher,
      VacancyStorage storage,
      long initialDelayMs,
      long intervalMs,
      long pauseBetweenRolesMs) {
    this.roleRepository = roleRepository;
    this.statsService = statsService;
    this.fetcher = fetcher;
    this.storage = storage;
    this.initialDelayMs = initialDelayMs;
    this.intervalMs = intervalMs;
    this.pauseBetweenRolesMs = pauseBetweenRolesMs;
  }

  /** Запускает периодическое фоновое обновление по расписанию. */
  public synchronized void start() {
    if (executor != null && !executor.isShutdown()) {
      return;
    }
    executor = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "vacancy-sync-scheduler");
      t.setDaemon(true);
      return t;
    });
    executor.scheduleWithFixedDelay(
        this::syncAllNow,
        initialDelayMs,
        intervalMs,
        TimeUnit.MILLISECONDS);
  }

  /** Корректно останавливает планировщик. */
  public synchronized void stop() {
    if (executor != null) {
      executor.shutdownNow();
      executor = null;
    }
  }

  /** Запускает разовое внеочередное обновление в фоне (если оно ещё не идёт). */
  public boolean triggerAsyncSync() {
    if (syncing.get()) {
      return false;
    }
    if (executor != null && !executor.isShutdown()) {
      executor.execute(this::syncAllNow);
    } else {
      Thread t = new Thread(this::syncAllNow, "vacancy-sync-manual");
      t.setDaemon(true);
      t.start();
    }
    return true;
  }

  /** Обходит все роли, скачивает свежие вакансии, пересчитывает статистику и сохраняет в хранилище. */
  public void syncAllNow() {
    if (!syncing.compareAndSet(false, true)) {
      return;
    }
    lastSyncStartedAt = Instant.now();
    int errors = 0;
    try {
      List<Role> allRoles = roleRepository.findAll();
      for (Role role : allRoles) {
        if (Thread.currentThread().isInterrupted()) {
          break;
        }
        try {
          syncRole(role);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        } catch (Exception e) {
          errors++;
          System.err.println(
              "[VacancySyncScheduler] Error syncing role "
                  + role.getName() + ": " + e.getMessage());
        }
        if (pauseBetweenRolesMs > 0) {
          try {
            Thread.sleep(pauseBetweenRolesMs);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }
    } finally {
      lastSyncErrors = errors;
      lastSyncFinishedAt = Instant.now();
      syncing.set(false);
    }
  }

  /** Возвращает последний снимок из хранилища (БД) или сразу загружает роль с trudvsem.ru. */
  public RoleStats getOrFetchStats(Role role) throws IOException, InterruptedException {
    Optional<RoleStats> cached = storage.findLatestSnapshot(role.getId());
    if (cached.isPresent()) {
      return cached.get();
    }
    return syncRole(role);
  }

  /** Возвращает историю снимков статистики по датам (YYYY-MM-DD -> RoleStats). */
  public Map<String, RoleStats> getDailySnapshots(Long roleId) {
    return storage.findDailySnapshots(roleId);
  }

  /** Статус планировщика в формате, безопасном для прямой сериализации через Gson. */
  public Map<String, Object> getStatus() {
    Map<String, Object> status = new LinkedHashMap<>();
    status.put("storage", storage.storageName());
    status.put("syncingNow", syncing.get());
    status.put("intervalSeconds", TimeUnit.MILLISECONDS.toSeconds(intervalMs));
    status.put("totalRoles", roleRepository.findAll().size());
    status.put("cachedRoles", storage.countCachedRoles());
    status.put("totalVacanciesCached", storage.countTotalVacancies());
    status.put("lastSyncStartedAt", lastSyncStartedAt == null ? null : lastSyncStartedAt.toString());
    status.put("lastSyncFinishedAt", lastSyncFinishedAt == null ? null : lastSyncFinishedAt.toString());
    status.put("lastSyncErrors", lastSyncErrors);
    return status;
  }

  private RoleStats syncRole(Role role) throws IOException, InterruptedException {
    List<Vacancy> vacancies = fetcher.fetch(role);
    RoleStats computed = statsService.compute(role, vacancies);
    storage.saveVacancies(role.getId(), "trudvsem", vacancies);
    storage.saveSnapshot(role.getId(), LocalDate.now(), computed);
    return computed;
  }

  private static long resolveIntervalHours() {
    String env = System.getenv("VACANCY_SYNC_HOURS");
    if (env == null || env.isBlank()) {
      return DEFAULT_INTERVAL_HOURS;
    }
    try {
      long parsed = Long.parseLong(env.trim());
      return parsed > 0 ? parsed : DEFAULT_INTERVAL_HOURS;
    } catch (NumberFormatException e) {
      return DEFAULT_INTERVAL_HOURS;
    }
  }
}
