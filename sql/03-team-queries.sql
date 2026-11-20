-- Проверка связей Role -> Vacancy и число вакансий по каждой роли.
SELECT r.id, r.name, r.category, r.grade, COUNT(v.id) AS vacancies
FROM roles r
LEFT JOIN vacancies v ON v.role_id = r.id
GROUP BY r.id, r.name, r.category, r.grade
ORDER BY r.id;

-- Частота навыков в вакансиях выбранной роли (пример для Java Developer, id=1).
SELECT s.name, COUNT(DISTINCT v.id) AS vacancy_count
FROM vacancies v
JOIN vacancy_skills vs ON vs.vacancy_id = v.id
JOIN skills s ON s.id = vs.skill_id
WHERE v.role_id = 1
GROUP BY s.id, s.name
ORDER BY vacancy_count DESC, s.name;

-- Медиана зарплаты по одной роли напрямую в PostgreSQL.
SELECT percentile_cont(0.5) WITHIN GROUP (
    ORDER BY (COALESCE(salary_from, salary_to) + COALESCE(salary_to, salary_from)) / 2
) AS median_salary
FROM vacancies
WHERE role_id = 1 AND (salary_from IS NOT NULL OR salary_to IS NOT NULL);

-- Последние сохранённые дневные снимки статистики от VacancySyncScheduler.
SELECT r.name, s.snapshot_date, s.vacancy_count, s.salary_median, s.currency, s.updated_at
FROM role_stats_snapshots s
JOIN roles r ON r.id = s.role_id
ORDER BY s.snapshot_date DESC, r.id;

-- Словарь синонимов по навыкам.
SELECT sk.id, sk.name, COUNT(syn.id) AS synonyms_count
FROM skills sk
LEFT JOIN skill_synonyms syn ON syn.skill_id = sk.id
GROUP BY sk.id, sk.name
ORDER BY sk.id;
