# Книжный каталог — ЛР №1 (CRUD на Spring Boot), ЛР №2 (юнит-тестирование), ЛР №3 (Docker), ЛР №4 (Flyway), ЛР №5 (Swagger UI), ЛР №6 (@FeignClient)

ЛР №1 — CRUD на Spring Boot, JUnit, Lombok, MapStruct. ЛР №2 — юнит-тестирование этого же
проекта с JUnit 5 и Mockito (раздел «Тесты ЛР №2» ниже).

Предметная область — «Книжный каталог»: книги (`Book`) и авторы (`Author`).
REST API для книг `/api/books`: CRUD через DTO и MapStruct-маппер, логика в сервисе
(интерфейс `BookService`, реализация `service/impl/BookServiceImpl`), частичное обновление (PUT/PATCH с `JsonNullable`), обработка 404,
валидация (400), поиск книг по автору (`GET /api/books?authorId=...`).

Стек: Java 17+, Spring Boot 3.5, Maven, Spring Data JPA, Flyway, MySQL 8.4 (H2 — в тестах), Lombok,
MapStruct, jackson-databind-nullable, springdoc-openapi 2.8.17 (Swagger UI), Spring Cloud 2025.0.3
(OpenFeign 4.3.3, CircuitBreaker Resilience4j 3.3.3 — ЛР6); тесты — JUnit 5, MockMvc, Mockito, Instancio,
Datafaker, JsonUnit.

## Запуск

```bash
docker compose up -d mysql # MySQL 8.4 из docker-compose.yml (localhost:3306, БД books)
mvn spring-boot:run        # приложение на http://localhost:8081, Flyway применяет миграции
mvn test                   # все тесты (96), Docker не нужен — H2 в режиме MySQL
```

Без Docker можно запустить на in-memory H2 (те же миграции Flyway, консоль H2):
`mvn spring-boot:run -Dspring-boot.run.profiles=h2`.

Порт задан в `src/main/resources/application.properties` (`server.port=8081`).

## Клиент-серверное взаимодействие через @FeignClient (ЛР №6)

Приложение получает заказы пользователей у сервиса **Order-Service** декларативным HTTP-клиентом
Spring Cloud OpenFeign. Реестра сервисов (Eureka) нет, а демонстрация должна работать офлайн, поэтому
Order-Service эмулируется **внутри этого же приложения** контроллером-заглушкой с префиксом
`/order-service` (заказы хранятся в памяти). Клиент `OrderClient` обращается к нему по настоящему HTTP
по адресу из свойства `clients.order-service.url` (по умолчанию `http://localhost:8081/order-service`) —
поэтому видны логи Feign, работают таймауты, повторы и fallback.

### Зависимости (`pom.xml`)

| Что | Артефакт | Версия |
|-----|----------|--------|
| BOM Spring Cloud (`dependencyManagement`, `import`) | `org.springframework.cloud:spring-cloud-dependencies` | `2025.0.3` (`spring-cloud.version`), линия для Spring Boot 3.5 |
| Feign-клиенты | `org.springframework.cloud:spring-cloud-starter-openfeign` | из BOM → 4.3.3 (feign-core 13.6.1) |
| Fallback (замена Hystrix) | `org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j` | из BOM → 3.3.3 (Resilience4j 2.2.0) |

Hystrix и Ribbon из методички удалены из Spring Cloud начиная с 2020.0 и не работают со Spring Boot 3:
механизм резервирования реализован через Spring Cloud CircuitBreaker + Resilience4j, балансировка
нагрузки (Spring Cloud LoadBalancer) не используется — экземпляр Order-Service один и адрес задан явно.

### Устройство

| Класс | Назначение |
|-------|------------|
| `Lab1Application` | `@EnableFeignClients` — сканирование интерфейсов `@FeignClient` в `io.hexlet.spring` |
| `client/OrderClient` | `@FeignClient(name = "order-service", url = "${clients.order-service.url}", configuration = FeignClientConfig.class, fallback = OrderClientFallback.class)`; методы `getOrdersByUserId(@PathVariable("userId"))` → `GET /orders/{userId}` и `getOrdersByStatus(@RequestParam("status"))` → `GET /orders?status=` |
| `client/OrderClientFallback` | `@Component implements OrderClient` — пустой список, если Order-Service недоступен |
| `client/OneLineLogger` | `extends feign.Logger`: каждая запись — одна строка `configKey - …`; один обмен = одна строка с методом, URL, статусом, временем, заголовками и телом |
| `config/FeignClientConfig` | бины `Logger.Level` (`FULL`), `Logger` (`OneLineLogger`), `Retryer.Default(100, 1000, 3)`, `RequestInterceptor` (заголовок `X-Request-Source: lab6-user-service`) |
| `service/UserOrdersService`, `service/impl/UserOrdersServiceImpl` | клиент внедряется через конструктор, вызовы делегируются ему |
| `controller/UserController` | `GET /users/{id}/orders`, `GET /users/orders/by-status?status=` |
| `orderservice/OrderServiceController`, `orderservice/OrderStore` | заглушка Order-Service: `GET /order-service/orders/{userId}`, `GET /order-service/orders?status=` |
| `dto/Order` | заказ: `id`, `userId`, `bookId`, `bookTitle`, `quantity`, `status` |

Настройки (`application.properties`):

```properties
clients.order-service.url=http://localhost:${server.port}/order-service
spring.cloud.openfeign.client.config.order-service.connect-timeout=2000
spring.cloud.openfeign.client.config.order-service.read-timeout=3000
spring.cloud.openfeign.client.config.order-service.logger-level=full
spring.cloud.openfeign.circuitbreaker.enabled=true            # без него fallback игнорируется
resilience4j.timelimiter.configs.default.timeout-duration=20s # по умолчанию 1 с — меньше таймаутов Feign
```

### Эндпоинты

| Метод и путь | Что делает |
|--------------|------------|
| `GET /users/{id}/orders` | заказы пользователя через `OrderClient.getOrdersByUserId` (200, JSON-массив; `[]`, если заказов нет или Order-Service недоступен) |
| `GET /users/orders/by-status?status=NEW` | заказы в статусе через `OrderClient.getOrdersByStatus` (`@RequestParam("status")`) |
| `GET /order-service/orders/{userId}` | заглушка Order-Service (цель клиента) |
| `GET /order-service/orders?status=NEW` | заглушка Order-Service (цель клиента) |

Демо-заказы: пользователь `1` — заказы 1 (`NEW`), 2 (`PAID`), 3 (`DELIVERED`); пользователь `2` — 4
(`SHIPPED`), 5 (`NEW`). Все эндпоинты описаны в Swagger UI (`users-controller`, `order-service-stub`).

### Демонстрация (curl)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2   # порт 8081, без Docker

# 1. Заказы пользователя 1 через Feign-клиент
curl -s http://localhost:8081/users/1/orders
# [{"id":1,"userId":"1","bookId":1,"bookTitle":"Crime and Punishment","quantity":1,"status":"NEW"},
#  {"id":2,...,"status":"PAID"},{"id":3,...,"status":"DELIVERED"}]

# 2. Фильтр по статусу (параметр запроса status)
curl -s "http://localhost:8081/users/orders/by-status?status=NEW"
# [{"id":1,...,"status":"NEW"},{"id":5,"userId":"2",...,"status":"NEW"}]

# 3. Пользователь без заказов — 200 и []
curl -s -w ' %{http_code}\n' http://localhost:8081/users/999/orders

# 4. Тот же запрос напрямую в Order-Service (без Feign)
curl -s http://localhost:8081/order-service/orders/2
```

Однострочные записи Feign в логе приложения (по одной на запрос):

```
... INFO ... io.hexlet.spring.client.OneLineLogger : OrderClient#getOrdersByUserId(String) - GET http://localhost:8081/order-service/orders/1 -> 200 (330 ms) headers={X-Request-Source=[lab6-user-service]} body=[{"id":1,"userId":"1",...
... INFO ... io.hexlet.spring.client.OneLineLogger : OrderClient#getOrdersByStatus(String) - GET http://localhost:8081/order-service/orders?status=NEW -> 200 (16 ms) headers={X-Request-Source=[lab6-user-service]} body=[...]
```

Fallback: перезапуск с адресом Order-Service на закрытом порту — вместо 500 ответ 200 и `[]`:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2 \
  -Dspring-boot.run.arguments="--clients.order-service.url=http://localhost:1/order-service"
# или переменной окружения: CLIENTS_ORDER_SERVICE_URL=http://localhost:1/order-service
curl -s -w ' %{http_code}\n' http://localhost:8081/users/1/orders    # [] 200
# в логе: 3 попытки (Retryer) и fallback
# OrderClient#getOrdersByUserId(String) - ERROR ConnectException: Connection refused (9 ms)
# OrderClient#getOrdersByUserId(String) - RETRYING
# ...
# OrderClientFallback : Order-Service is unavailable: fallback for getOrdersByUserId(1) -> []
```

### Тесты ЛР №6

```bash
mvn test -Dtest='OrderClientTest,UserOrdersControllerTest,OrderClientFallbackTest,OneLineLoggerTest,UserOrdersServiceImplTest,OrderServiceControllerTest,FeignApiDocsTest'
```

- `client/OrderClientTest` — клиент против заглушки по HTTP: `@PathVariable`, `@RequestParam`, однострочный лог, заголовок перехватчика;
- `controller/UserOrdersControllerTest` — `GET /users/{id}/orders`, `GET /users/orders/by-status`;
- `client/OrderClientFallbackTest` — Order-Service недоступен (порт 1): клиент и эндпоинт возвращают `[]`, не 500;
- `client/OneLineLoggerTest` — формат однострочной записи;
- `service/UserOrdersServiceImplTest` — Mockito, делегирование клиенту;
- `orderservice/OrderServiceControllerTest`, `docs/FeignApiDocsTest` — заглушка и документация в Swagger.

Адрес `@FeignClient(url)` вычисляется при старте контекста, до того как известен случайный порт
(`RANDOM_PORT`), поэтому `AbstractFeignIntegrationTest` заранее выбирает свободный порт и использует его
как `server.port` (`DEFINED_PORT`) и в `clients.order-service.url`. В остальных тестах клиент направлен на
закрытый порт `1`, чтобы ни один тест не обратился к приложению, запущенному на 8081.

## Swagger UI / OpenAPI (ЛР №5)

Документация REST API генерируется библиотекой springdoc-openapi по контроллерам и DTO.

Зависимость в `pom.xml` (версия вынесена в `<properties>`):

```xml
<springdoc.version>2.8.17</springdoc.version>
...
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
```

Версия 2.1.0 из методички рассчитана на Spring Boot 3.0/3.1; для Spring Boot 3.5
(Spring Framework 6.2) используется линия springdoc 2.8.x (3.x — для Spring Boot 4).
Работает поверх уже имеющегося `spring-boot-starter-web`.

Настройки в `src/main/resources/application.properties` (эквивалент `application.yml` из методички):

```properties
springdoc.api-docs.enabled=true
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operations-sorter=method
springdoc.swagger-ui.tags-sorter=alpha
springdoc.swagger-ui.try-it-out-enabled=true
```

Адреса (порт 8081, а не 8080 из методички):

| Что | URL |
|-----|-----|
| Swagger UI | http://localhost:8081/swagger-ui.html (302 → `/swagger-ui/index.html`) |
| OpenAPI JSON | http://localhost:8081/v3/api-docs |
| OpenAPI YAML | http://localhost:8081/v3/api-docs.yaml |

В Swagger UI две группы: `books-controller` (`/api/books`, ЛР1–4) и `users-controller`
(`/users`, ЛР5); в разделе Schemas — `BookCreateDTO`, `BookDTO`, `BookUpdateDTO`,
`UserRequest`, `UserResponse`. Общая информация (title «Book catalog API») задаётся бином
`config/OpenApiConfig`. Поля `JsonNullable<T>` в `BookUpdateDTO` документируются как обычный
тип `T` благодаря `config/JsonNullableModelConverter` (без него springdoc показывал бы
обёртки `JsonNullableString {present}`).

Swagger UI не зависит от БД: работает и с MySQL (`docker compose up`), и с профилем `h2`:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
# открыть http://localhost:8081/swagger-ui.html → users-controller → POST /users/create
# → Try it out → тело {"name":"Ivan Petrov","email":"ivan.petrov@example.com"} → Execute
# → GET /users/{userId} → Try it out → userId = 1 → Execute
```

### Пользователи `/users`

`UserController` только делегирует в `UserService` (`service/impl/UserServiceImpl`),
пользователи хранятся в памяти (`ConcurrentHashMap`, id — счётчик `"1"`, `"2"`, …; после
перезапуска список пуст), схема БД не меняется.

| Метод | Путь | Описание | Ответ |
|-------|------|----------|-------|
| POST | `/users/create` | создание (`UserRequest {name, email}`) | 201 `UserResponse {id, name, email}` / 400 |
| GET | `/users/{userId}` | пользователь по id | 200 / 404 |
| GET | `/users` | список пользователей | 200 |

```bash
curl -s http://localhost:8081/v3/api-docs
curl -s http://localhost:8081/v3/api-docs.yaml
curl -s -i http://localhost:8081/swagger-ui.html          # 302 → /swagger-ui/index.html

curl -s -i -X POST http://localhost:8081/users/create \
  -H "Content-Type: application/json" \
  -d '{"name":"Ivan Petrov","email":"ivan.petrov@example.com"}'   # 201 {"id":"1",...}
curl -s http://localhost:8081/users/1                      # 200
curl -s http://localhost:8081/users                        # 200, список
curl -s -i http://localhost:8081/users/999                 # 404 "User with id 999 not found"
curl -s -i -X POST http://localhost:8081/users/create \
  -H "Content-Type: application/json" -d '{"name":"","email":"bad"}'  # 400
```

Тесты ЛР5: `controller/UserControllerTest` (MockMvc: 201, 200, 404, список, 400 — 5 тестов),
`service/UserServiceImplTest` (сервис — 4), `docs/OpenApiDocsTest` (`/v3/api-docs` содержит
пути `/users…` и `/api/books…` и схемы DTO, `BookUpdateDTO` без `JsonNullable`,
`/v3/api-docs.yaml`, редирект `/swagger-ui.html`, `/swagger-ui/index.html` — 5).

## Миграции Flyway (ЛР №4)

Схемой БД управляет только Flyway, Hibernate её лишь проверяет (`ddl-auto=validate`).

Зависимости в `pom.xml` (версии из BOM Spring Boot 3.5, Flyway 11.x): `org.flywaydb:flyway-core`
и `org.flywaydb:flyway-mysql` (без второй Flyway 10+ не работает с MySQL), плюс уже имевшиеся
`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `com.mysql:mysql-connector-j`.

Настройки (`src/main/resources/application.properties`):

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/books
spring.datasource.username=books_user
spring.datasource.password=books_password
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.validate-on-migrate=true
```

Скрипты в `src/main/resources/db/migration` (выполняются по порядку версий, каждый один раз):

| Файл | Что делает |
|------|------------|
| `V1__CREATE_PRODUCT_TABLE.sql` | таблица `PRODUCT` (id, name, description, price, created_at, modified_at) — из методички |
| `V1.1__INSERT_PRODUCT_DATA.sql` | 3 строки Product 1–3 — из методички |
| `V2__CREATE_AUTHORS_AND_BOOKS_TABLES.sql` | таблицы `authors`, `books` (FK `author_id`, unique `email`/`slug`) под маппинг JPA |
| `V3__INSERT_AUTHORS_AND_BOOKS_DATA.sql` | авторы `id=1, 2` и книга `id=1` (раньше были в `data.sql`, он удалён) |

Имя таблицы `PRODUCT` — в верхнем регистре: в MySQL на Linux имена таблиц регистрозависимы,
запрос пишется так же, как в скрипте. `spring.jpa.defer-datasource-initialization` и
`spring.sql.init.mode` удалены — данные приходят только из миграций.

Проверка:

```bash
docker compose up -d mysql
mvn spring-boot:run
# в логе:
#   Migrating schema `books` to version "1 - CREATE PRODUCT TABLE"
#   Migrating schema `books` to version "1.1 - INSERT PRODUCT DATA"
#   Migrating schema `books` to version "2 - CREATE AUTHORS AND BOOKS TABLES"
#   Migrating schema `books` to version "3 - INSERT AUTHORS AND BOOKS DATA"
#   Successfully applied 4 migrations to schema `books`, now at version v3

# история миграций
docker compose exec mysql mysql -ubooks_user -pbooks_password books \
  -e "SELECT installed_rank, version, description, script, success FROM flyway_schema_history;"

# таблица из методички — 3 строки
docker compose exec mysql mysql -ubooks_user -pbooks_password books -e "SELECT * FROM PRODUCT;"
```

При повторном запуске Flyway сверяет контрольные суммы (`Successfully validated 4 migrations`)
и пишет `Schema \`books\` is up to date. No migration necessary.` Уже применённый скрипт
менять нельзя — `validate-on-migrate` остановит запуск; изменения схемы — только новым файлом
`V4__...sql`.

**Том MySQL.** В `docker-compose.yml` задано имя проекта `name: lab4-flyway`, поэтому
используется новый том `lab4-flyway_mysql_data`, а не том ЛР3 с таблицами, созданными
Hibernate (на непустой схеме без `flyway_schema_history` Flyway падает с «Found non-empty
schema … without schema history table»). Чтобы начать с чистой БД:

```bash
docker compose down -v     # удалить контейнеры и том lab4-flyway_mysql_data
docker compose up -d mysql
```

## Запуск в Docker (ЛР №3)

Приложение и MySQL 8.4 запускаются через Docker Compose. Файлы в корне проекта:

- `Dockerfile` — multi-stage: стадия `maven:3.9-eclipse-temurin-17` собирает jar
  (`mvn package -DskipTests`, имя `target/app.jar`), итоговый образ `eclipse-temurin:17-jre`
  копирует его в `/app.jar` и запускает `java -jar /app.jar`. Локальный `mvn package` не нужен.
- `docker-compose.yml` — сервисы `mysql` (`mysql:8.4`, `restart: always`, БД `books`,
  пользователь `books_user`, healthcheck `mysqladmin ping`, том `mysql_data`, порт 3306) и
  `app` (`build: .`, порт 8081, `depends_on: mysql: condition: service_healthy`).
- `.dockerignore` — исключает `target/`, `.git`, `.idea` из контекста сборки.

Подключение к MySQL задаётся переменными окружения сервиса `app`, которые Spring Boot
подхватывает автоматически поверх `application.properties`:
`SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/books`, `SPRING_DATASOURCE_USERNAME=books_user`,
`SPRING_DATASOURCE_PASSWORD=books_password` (учебные пароли). Драйвер `mysql-connector-j` и
диалект определяются по URL. Схему и начальные данные создают миграции Flyway (они входят в
jar из `src/main/resources/db/migration`), в логе `docker compose logs app` видны строки
`Migrating schema \`books\` …`. Тесты (`mvn test`) используют H2.

```bash
docker compose up --build -d      # сборка образа и запуск mysql + app
docker compose ps                 # mysql — Up (healthy), app — Up
docker compose logs app           # Flyway: Migrating schema `books` ..., Started Lab1Application

# Проверка API (curl)
curl -s http://localhost:8081/api/books
curl -s -i -X POST http://localhost:8081/api/books \
  -H "Content-Type: application/json" \
  -d '{"authorId":1,"slug":"war-and-peace","title":"War and Peace","description":"Epic novel","publishedYear":1869}'
curl -s http://localhost:8081/api/books/2
curl -s -X PATCH http://localhost:8081/api/books/2 \
  -H "Content-Type: application/json" -d '{"publishedYear":1873}'
curl -s "http://localhost:8081/api/books?authorId=1"

# Данные действительно в MySQL
docker compose exec mysql mysql -ubooks_user -pbooks_password books \
  -e "SELECT id, title, slug, published_year FROM books"

curl -s -i -X DELETE http://localhost:8081/api/books/2   # 204
curl -s -i http://localhost:8081/api/books/2             # 404

docker compose down               # остановка (данные остаются в томе lab4-flyway_mysql_data)
docker compose down -v            # остановка с удалением тома
```

## Консоль H2 (только профиль `h2`)

Запуск: `mvn spring-boot:run -Dspring-boot.run.profiles=h2` (`application-h2.properties`).

- URL: http://localhost:8081/h2-console
- JDBC URL: `jdbc:h2:mem:books;MODE=MySQL;DATABASE_TO_LOWER=TRUE`
- Пользователь: `sa`, пароль — пустой

Демо-данные (миграция `V3__INSERT_AUTHORS_AND_BOOKS_DATA.sql`): авторы `id=1` (Leo Tolstoy) и
`id=2` (Fyodor Dostoevsky), книга `crime-and-punishment` (`id=1`) автора `id=2`.

## API

| Метод | Путь | Описание | Ответ |
|-------|------|----------|-------|
| GET | `/api/books` | список книг | 200 |
| GET | `/api/books?authorId={id}` | книги автора | 200 |
| POST | `/api/books` | создание (`BookCreateDTO`) | 201 / 400 / 404 (нет автора) / 409 (слаг занят) |
| GET | `/api/books/{id}` | книга по id | 200 / 404 |
| PUT | `/api/books/{id}` | частичное обновление (`BookUpdateDTO`) | 200 / 400 / 404 |
| PATCH | `/api/books/{id}` | частичное обновление (`BookUpdateDTO`) | 200 / 400 / 404 |
| DELETE | `/api/books/{id}` | удаление | 204 / 404 |

## Демонстрация (curl)

```bash
# 1. Список книг
curl -s http://localhost:8081/api/books

# 2. Создание книги (201 Created)
curl -s -i -X POST http://localhost:8081/api/books \
  -H "Content-Type: application/json" \
  -d '{"authorId":1,"slug":"war-and-peace","title":"War and Peace","description":"Epic novel","publishedYear":1869}'

# 3. Список книг (две книги)
curl -s http://localhost:8081/api/books

# 4. Книга по id (id из ответа шага 2, обычно 2)
curl -s http://localhost:8081/api/books/2

# 5. Частичное обновление: меняется только title (PUT), затем только publishedYear (PATCH)
curl -s -X PUT http://localhost:8081/api/books/2 \
  -H "Content-Type: application/json" -d '{"title":"War and Peace (2nd ed.)"}'
curl -s -X PATCH http://localhost:8081/api/books/2 \
  -H "Content-Type: application/json" -d '{"publishedYear":1873}'

# 6. Поиск книг по автору
curl -s "http://localhost:8081/api/books?authorId=1"

# 7. Ошибка валидации (400)
curl -s -i -X POST http://localhost:8081/api/books \
  -H "Content-Type: application/json" -d '{"slug":""}'

# 8. Удаление (204 No Content)
curl -s -i -X DELETE http://localhost:8081/api/books/2

# 9. Несуществующая книга (404 "Not Found: 2")
curl -s -i http://localhost:8081/api/books/2
```

Слаг уникален: повторное создание книги с тем же `slug` вернёт 409 Conflict.

## Тесты ЛР №2 (JUnit 5 + Mockito)

Тесты используют отдельную конфигурацию `src/test/resources/application.properties`:
in-memory H2 `jdbc:h2:mem:books_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE`, Flyway включён и
выполняет те же миграции, что и в MySQL, Hibernate проверяет схему (`ddl-auto=validate`) —
рабочая БД `books` не затрагивается, Docker не нужен. Начальные данные (авторы `id=1, 2`,
книга `id=1`) приходят из миграции V3. `@DataJpaTest` использует свою H2 `books_repo_test`
(`@AutoConfigureTestDatabase(replace = NONE)` + `@TestPropertySource`), миграции применяются и там.

| Класс | Слой | Средства | Тестов |
|-------|------|----------|--------|
| `repository/BookRepositoryUnitTests` | репозиторий | `@DataJpaTest`, AssertJ, `@TestMethodOrder`/`@Order`, `@Rollback(false)`, `@DisplayName`; `@Import(JpaAuditingConfig.class)` для аудита дат | 8 |
| `controller/BookControllerUnitTests` | контроллер | `@WebMvcTest(BooksController.class)`, MockMvc, `@MockitoBean BookService`, BDDMockito `given`/`willDoNothing`, `jsonPath` + Hamcrest `is`; `@Import(JacksonConfig.class)` | 9 |
| `service/BookServiceUnitTests` | сервис | `@ExtendWith(MockitoExtension.class)`, `@Mock` репозитории и маппер, `@InjectMocks BookServiceImpl`, BDDMockito, `verify(…, times(1))` | 10 |
| `controller/BooksControllerTest` (ЛР1) | интеграционные | `@SpringBootTest`, MockMvc, Instancio, Datafaker, JsonUnit | 16 |
| `service/BookServiceTest` (ЛР1) | сервис | Mockito, `@InjectMocks BookServiceImpl` | 11 |
| `migration/FlywayMigrationTest` (ЛР4) | миграции | `@SpringBootTest`, `JdbcTemplate`: `flyway_schema_history` — версии 1, 1.1, 2, 3; `PRODUCT` — 3 строки; данные authors/books | 3 |
| `controller/UserControllerTest` (ЛР5) | интеграционные `/users` | `@SpringBootTest`, MockMvc, Datafaker, JsonUnit | 5 |
| `service/UserServiceImplTest` (ЛР5) | сервис пользователей | JUnit 5, AssertJ | 4 |
| `docs/OpenApiDocsTest` (ЛР5) | документация | `@SpringBootTest`, MockMvc: `/v3/api-docs`, `.yaml`, `/swagger-ui.html` | 5 |

В каждом классе ЛР2 первые 5 тестов — позитивные сценарии по методичке (создание, по id,
список, обновление, удаление), остальные — негативные (не найдено → `ResourceNotFoundException`
/ 404, неизвестный автор, ошибка валидации → 400). Репозиторий дополнительно проверяет
`findBySlug` и `findByAuthorId`. `DELETE` в контроллере возвращает 204 No Content.

Аудит дат (`@EnableJpaAuditing`) вынесен в `config/JpaAuditingConfig`: если оставить его на
главном классе, срез `@WebMvcTest` падает с «JPA metamodel must not be empty».

Запуск:

```bash
mvn test                                        # все тесты (71)
mvn test -Dtest='UserControllerTest,UserServiceImplTest,OpenApiDocsTest'  # тесты ЛР5
mvn test -Dtest=FlywayMigrationTest             # только проверка миграций
mvn test -Dtest=BookRepositoryUnitTests         # только репозиторий
mvn test -Dtest=BookControllerUnitTests         # только контроллер
mvn test -Dtest=BookServiceUnitTests            # только сервис
```

## Структура проекта

```
project/
├── pom.xml
├── Dockerfile                # multi-stage сборка образа (ЛР3)
├── docker-compose.yml        # mysql + app (ЛР3), name: lab4-flyway (ЛР4)
├── .dockerignore
├── README.md
└── src
    ├── main
    │   ├── java/io/hexlet/spring
    │   │   ├── Lab1Application.java          # точка входа, @EnableFeignClients (ЛР6)
    │   │   ├── client/OrderClient.java       # @FeignClient Order-Service (ЛР6)
    │   │   ├── client/OrderClientFallback.java # fallback клиента (ЛР6)
    │   │   ├── client/OneLineLogger.java     # однострочный логгер Feign (ЛР6)
    │   │   ├── config/FeignClientConfig.java # уровень лога, логгер, Retryer, перехватчик (ЛР6)
    │   │   ├── orderservice/OrderServiceController.java, OrderStore.java # заглушка Order-Service (ЛР6)
    │   │   ├── dto/Order.java                # заказ (ЛР6)
    │   │   ├── service/UserOrdersService.java, service/impl/UserOrdersServiceImpl.java # ЛР6
    │   │   ├── config/JacksonConfig.java     # регистрация JsonNullableModule
    │   │   ├── config/JpaAuditingConfig.java # @EnableJpaAuditing (аудит createdAt/updatedAt)
    │   │   ├── config/OpenApiConfig.java     # информация OpenAPI для Swagger UI (ЛР5)
    │   │   ├── config/JsonNullableModelConverter.java # JsonNullable<T> → T в схемах (ЛР5)
    │   │   ├── controller/BooksController.java
    │   │   ├── controller/UserController.java   # /users (ЛР5), /users/{id}/orders (ЛР6)
    │   │   ├── dto/BookCreateDTO.java, BookUpdateDTO.java, BookDTO.java
    │   │   ├── dto/UserRequest.java, UserResponse.java   # ЛР5
    │   │   ├── exception/ResourceNotFoundException.java, GlobalExceptionHandler.java
    │   │   ├── mapper/BookMapper.java, JsonNullableMapper.java, ReferenceMapper.java
    │   │   ├── model/BaseEntity.java, Book.java, Author.java
    │   │   ├── repository/BookRepository.java, AuthorRepository.java
    │   │   ├── service/BookService.java      # интерфейс сервиса
    │   │   ├── service/UserService.java      # интерфейс сервиса пользователей (ЛР5)
    │   │   ├── service/impl/BookServiceImpl.java
    │   │   └── service/impl/UserServiceImpl.java # хранение пользователей в памяти (ЛР5)
    │   └── resources
    │       ├── application.properties        # MySQL + Flyway + springdoc (ЛР5) + OpenFeign (ЛР6)
    │       ├── application-h2.properties     # профиль h2 (запуск без Docker)
    │       └── db/migration                  # миграции Flyway (ЛР4)
    │           ├── V1__CREATE_PRODUCT_TABLE.sql
    │           ├── V1.1__INSERT_PRODUCT_DATA.sql
    │           ├── V2__CREATE_AUTHORS_AND_BOOKS_TABLES.sql
    │           └── V3__INSERT_AUTHORS_AND_BOOKS_DATA.sql
    └── test
        ├── java/io/hexlet/spring
        │   ├── config/TestConfig.java                    # бин Faker
        │   ├── controller/BooksControllerTest.java       # интеграционные (ЛР1)
        │   ├── controller/BookControllerUnitTests.java   # @WebMvcTest (ЛР2)
        │   ├── repository/BookRepositoryUnitTests.java   # @DataJpaTest (ЛР2)
        │   ├── migration/FlywayMigrationTest.java        # проверка миграций (ЛР4)
        │   ├── service/BookServiceTest.java              # Mockito (ЛР1)
        │   ├── service/BookServiceUnitTests.java         # Mockito + BDDMockito (ЛР2)
        │   ├── client/AbstractFeignIntegrationTest.java  # общий контекст Feign-тестов (ЛР6)
        │   ├── client/OrderClientTest.java, OrderClientFallbackTest.java, OneLineLoggerTest.java # ЛР6
        │   ├── controller/UserOrdersControllerTest.java  # эндпоинты заказов (ЛР6)
        │   ├── orderservice/OrderServiceControllerTest.java # заглушка (ЛР6)
        │   ├── docs/FeignApiDocsTest.java                # Swagger для эндпоинтов ЛР6
        │   └── service/UserOrdersServiceImplTest.java    # Mockito (ЛР6)
        └── resources/application.properties             # тестовая БД H2 books_test
```
