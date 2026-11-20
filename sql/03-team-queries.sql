-- Проверка связей Role -> Vacancy и запрос для статистики по роли.
SELECT r.id, r.name, COUNT(v.id) AS vacancies
FROM roles r LEFT JOIN vacancies v ON v.role_id = r.id
GROUP BY r.id, r.name
ORDER BY r.id;

-- Частота навыков в вакансиях выбранной роли (пример для Java Developer, id=1).
SELECT s.name, COUNT(DISTINCT v.id) AS vacancy_count
FROM vacancies v
JOIN vacancy_skills vs ON vs.vacancy_id = v.id
JOIN skills s ON s.id = vs.skill_id
WHERE v.role_id = 1
GROUP BY s.id, s.name
ORDER BY vacancy_count DESC, s.name;

-- Упрощённая медиана зарплаты по одной роли (примерные данные для демонстрации).
SELECT percentile_cont(0.5) WITHIN GROUP (
    ORDER BY (COALESCE(salary_from, salary_to) + COALESCE(salary_to, salary_from)) / 2
) AS median_salary
FROM vacancies
WHERE role_id = 1 AND (salary_from IS NOT NULL OR salary_to IS NOT NULL);
