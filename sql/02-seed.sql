INSERT INTO roles (id, name, description) VALUES
(1, 'Java Developer', 'Разработка серверных приложений'),
(2, 'Data Analyst', 'Анализ данных и отчётность'),
(3, 'DevOps Engineer', 'Инфраструктура и автоматизация')
ON CONFLICT (id) DO NOTHING;

INSERT INTO skills (id, name, description) VALUES
(1, 'Java', 'Язык программирования Java'),
(2, 'SQL', 'Работа с реляционными базами данных'),
(3, 'PostgreSQL', 'Реляционная СУБД'),
(4, 'Docker', 'Контейнеризация'),
(5, 'Git', 'Система контроля версий'),
(6, 'Spring', 'Java-фреймворк'),
(7, 'Kafka', 'Система сообщений'),
(8, 'Python', 'Язык Python'),
(9, 'Excel', 'Электронные таблицы'),
(10, 'Linux', 'Операционная система')
ON CONFLICT (id) DO NOTHING;

INSERT INTO role_skills (role_id, skill_id, required_level) VALUES
(1,1,4), (1,2,3), (1,3,3), (1,4,2), (1,5,3), (1,6,3),
(2,2,4), (2,8,3), (2,9,3), (2,3,2),
(3,4,4), (3,10,4), (3,5,3), (3,3,2)
ON CONFLICT DO NOTHING;

INSERT INTO skill_dependencies (skill_id, prerequisite_skill_id) VALUES
(6,1), (3,2), (7,1)
ON CONFLICT DO NOTHING;

INSERT INTO skill_synonyms (skill_id, synonym) VALUES
(1, 'Java SE'), (3, 'Postgres'), (4, 'Docker Engine'), (6, 'Spring Boot')
ON CONFLICT (synonym) DO NOTHING;

INSERT INTO vacancies
(id, role_id, external_id, source, title, employer, salary_from, salary_to) VALUES
(1,1,'demo-1','demo','Junior Java Developer','Example Company',80000,120000),
(2,1,'demo-2','demo','Java Backend Developer','Another Company',150000,220000),
(3,2,'demo-3','demo','Data Analyst','Analytics Company',100000,150000)
ON CONFLICT (source, external_id) DO NOTHING;

INSERT INTO vacancy_skills (vacancy_id, skill_id) VALUES
(1,1),(1,2),(1,5),(2,1),(2,3),(2,6),(3,2),(3,8),(3,9)
ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('roles','id'), GREATEST((SELECT MAX(id) FROM roles),1));
SELECT setval(pg_get_serial_sequence('skills','id'), GREATEST((SELECT MAX(id) FROM skills),1));
SELECT setval(pg_get_serial_sequence('vacancies','id'), GREATEST((SELECT MAX(id) FROM vacancies),1));
