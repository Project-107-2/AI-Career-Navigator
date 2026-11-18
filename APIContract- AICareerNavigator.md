# API Contract — AI Career Navigator

**Base URL:** `http://localhost:4567`

Все ответы и запросы используют JSON.

## 1. Регистрация

`POST /auth/register`

Request:
```json
{
  "name": "Alena",
  "email": "alena@example.com",
  "password": "12345678"
}
```

Success — `201 Created`:
```json
{
  "message": "Registration successful"
}
```

Errors:
- `400` — некорректные данные.
- `409` — пользователь уже существует.

## 2. Авторизация

`POST /auth/login`

Request:
```json
{
  "email": "alena@example.com",
  "password": "12345678"
}
```

Success — `200 OK`:
```json
{
  "message": "Login successful"
}
```

Errors:
- `400` — некорректные данные.
- `401` — неверный email или пароль.

## 3. Получение профиля

`GET /profile`

Success — `200 OK`:
```json
{
  "name": "Alena",
  "email": "alena@example.com"
}
```

## 4. Получение списка навыков

`GET /skills`

Success — `200 OK`:
```json
{
  "skills": ["Java", "Spring", "PostgreSQL", "Kafka"]
}
```

## Общие правила

- Формат данных: JSON.
- При ошибке возвращается HTTP-статус и JSON с полем `message`.
- CORS настроен для разработки.
- Регистрация и авторизация пока используют временное хранилище в памяти.
- JWT и проверка авторизации защищённых endpoints планируются на следующем этапе.