# AI-Career-Navigatorasdfvdf
## Проверка API через Postman

### 1. Регистрация пользователя
POST /auth/register — 201 Created

![Регистрация](docs/screenshots/01-register.png)

### 2. Авторизация и получение JWT
POST /auth/login — 200 OK

![Авторизация](docs/screenshots/02-login.png)

### 3. Получение профиля
GET /profile — 200 OK

![Получение профиля](docs/screenshots/03-profile-get.png)

### 4. Изменение профиля
PUT /profile — 200 OK

![Изменение профиля](docs/screenshots/04-profile-put.png)

### 5. Сохранение самооценки навыка
PUT /skills/assessment — 200 OK

![Сохранение самооценки](docs/screenshots/05-assessment-put.png)

### 6. Получение самооценки
GET /skills/assessment — 200 OK

![Получение самооценки](docs/screenshots/06-assessment-get.png)

### 7. Обработка неправильного пароля
POST /auth/login — 401 Unauthorized

![Ошибка авторизации](docs/screenshots/07-error.png)

## PostgreSQL 18 — проверка через DBeaver

### Пользователи

![Пользователи](docs/screenshots/08-database.png)

### Самооценка навыков

![Самооценка навыков](docs/screenshots/09-assessments-db.png)

### Справочник навыков

![Навыки](docs/screenshots/10-skills-db.png)

## Unit-тесты JUnit 5

Все 5 тестов успешно пройдены.

![Результаты тестов](docs/screenshots/11-tests.png)
