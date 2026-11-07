# intelliCar

> An AI-powered used car marketplace backend built with Spring Boot, Spring AI (Google Gemini), PostgreSQL, Redis and Apache Kafka.

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot)
![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0-6DB33F?logo=spring)
![Gemini](https://img.shields.io/badge/Google-Gemini-4285F4?logo=googlegemini&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-KRaft-231F20?logo=apachekafka)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)

intelliCar is the backend of a car gallery platform where sellers publish vehicle listings and buyers browse, filter and make price offers. On top of the marketplace, an AI assistant helps both sides:

- **Buyers** tell the assistant their budget and get a ranked list of the best matching cars from the real listings.
- **Sellers** describe the car they own and get a suggested price range, based on similar listings on the platform.

Domain events (new offers, listing views, status changes) are processed asynchronously through Kafka, and frequently read data is cached in Redis.

This project was developed as a graduation project.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [AI Features](#ai-features)
- [Event-Driven Design with Kafka](#event-driven-design-with-kafka)
- [Domain Model](#domain-model)
- [API Overview](#api-overview)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)
- [Project Structure](#project-structure)
- [Design Decisions](#design-decisions)
- [Roadmap](#roadmap)

---

## Features

- **Authentication and authorization:** registration, login, JWT access tokens, refresh tokens stored server-side, logout, role-based users (`BUYER`, `SELLER`, `ADMIN`).
- **Listings:** create, update and soft-delete car listings with multiple images and a primary image.
- **Search and filtering:** dynamic, paginated filtering by brand, model, city, price range, year range, fuel type and transmission.
- **Offers:** buyers make offers on active listings, sellers accept or reject them. Business rules prevent offers on your own car, on inactive listings, and duplicate pending offers.
- **AI budget assistant:** ranks the best cars for a given budget and preferences, with a score, a reason, pros and cons for each car.
- **AI price estimation:** suggests a selling price range for the user's own car by analyzing similar listings.
- **Asynchronous processing with Kafka:** seller and buyer notifications, listing view counting, and automatic expiry of pending offers when a car is sold or deactivated.
- **E-mail notifications:** the seller is e-mailed when a new offer arrives and the buyer when the offer is accepted or rejected. Mails are sent asynchronously from a Kafka consumer.
- **AI rate limiting:** per-user limits on the AI endpoints (default 5 per minute and 50 per day) protect the Gemini quota.
- **Caching:** car detail responses are cached in Redis and evicted on update or delete.
- **API documentation:** interactive Swagger UI generated from the code.
- **Containerized setup:** PostgreSQL, Redis, Kafka and the backend start with a single Docker Compose command, together with Mailpit, a local inbox for development e-mails.

---

## Tech Stack

| Area | Technology | Purpose |
|---|---|---|
| Language | Java 21 | Main language |
| Framework | Spring Boot 4.1.1 | Application framework and auto-configuration |
| AI | Spring AI 2.0, Google Gemini | `ChatClient`, structured output, AI features |
| Web | Spring Web MVC | REST controllers |
| Persistence | Spring Data JPA, Hibernate | ORM, repositories, `Specification`-based dynamic queries |
| Database | PostgreSQL 15 | Primary relational data store |
| Cache | Redis 7, Spring Cache | Caching of listing details |
| Messaging | Apache Kafka (KRaft mode), Spring for Apache Kafka | Asynchronous, event-driven processing |
| E-mail | Spring Boot Mail (`JavaMailSender`), Mailpit for development | Offer notifications |
| Security | Spring Security, JJWT 0.12.5, BCrypt | Stateless JWT authentication, password hashing |
| Validation | Jakarta Bean Validation | Request validation |
| Mapping | MapStruct 1.5.5 | Entity to DTO mapping |
| Boilerplate | Lombok | Getters, constructors, logging |
| API docs | springdoc-openapi 3.1.1 | OpenAPI 3 spec and Swagger UI |
| Build | Maven (wrapper included) | Build and dependency management |
| Containers | Docker, Docker Compose | Local environment and deployment image |

---

## Architecture

The application follows a classic layered architecture. Controllers only handle HTTP concerns, services contain business rules and transactions, repositories talk to the database. Request and response objects are immutable Java records.

```mermaid
flowchart LR
    Client([Client / Swagger UI]) --> Sec[Security Filter Chain<br/>JWT]
    Sec --> C[Controllers]
    C --> S[Services]
    S --> R[Repositories]
    R --> DB[(PostgreSQL)]
    S <--> Cache[(Redis)]
    S -- publish events --> K{{Kafka}}
    K -- consume --> CO[Consumers]
    CO --> R
    CO -- e-mail --> SMTP([SMTP server / Mailpit])
    S -- prompts --> AI[Spring AI ChatClient]
    AI <--> G([Google Gemini])
```

---

## AI Features

Both AI features follow the same principle: **the database provides the facts, the model provides the judgment.** The application first selects real listings from PostgreSQL, then asks Gemini to analyze only that data. The model never decides which cars exist and never invents prices.

### 1. Budget-based car recommendation

`POST /intelliCar/api/v1/ai/recommend`

The user enters a budget and, optionally, free-text preferences and filters. The assistant returns the best matching cars, ranked.

How it works:

1. Active listings within the budget are loaded (up to 30, newest and lowest mileage first). Optional filters: fuel type, transmission, city. The user's own listings are excluded.
2. Gemini ranks the listings and writes a score (0 to 100), a short reason, pros and cons for each pick.
3. The response is validated: ids that are not in the candidate list are dropped, duplicates are removed, and price, year and mileage are always taken from the database, never from the model.
4. If no listing fits, the model is not called at all and a helpful message is returned.

Example request:

```json
{
  "budget": 1200000,
  "preferences": "family car, low fuel consumption",
  "fuelType": "DIESEL",
  "limit": 3
}
```

Only `budget` is required. `limit` accepts 1 to 10 (default 5).

Example response (shortened):

```json
{
  "budget": 1200000,
  "candidatesAnalyzed": 14,
  "summary": "...",
  "recommendations": [
    {
      "rank": 1,
      "score": 88,
      "carId": "3f6c...",
      "brand": "Skoda",
      "model": "Octavia",
      "year": 2020,
      "mileage": 72000,
      "price": 1150000,
      "reason": "...",
      "pros": ["..."],
      "cons": ["..."]
    }
  ]
}
```

### 2. Selling price estimation

`POST /intelliCar/api/v1/ai/price-estimate`

The user describes the car they want to sell. The assistant analyzes similar listings of the same brand and suggests a price range.

How it works:

1. Active and sold listings of the same brand are loaded (the user's own listings are excluded).
2. A deterministic analyzer (plain Java, no AI) picks up to 15 listings most similar to the car, scored by model, year, mileage, fuel type, transmission and city. It calculates the market statistics: minimum, median, maximum and the lower/upper quartile.
3. Gemini adjusts the range for differences in year, mileage and condition, using the statistics and the listings as context.
4. The suggestion is checked before it is returned: it must be ordered, positive, close to the prices of the similar listings, and not unreasonably wide. If Gemini is down or gives an implausible answer, the statistical range is returned instead (`aiAssisted: false`).
5. If there is no listing of that brand, the API responds with `400`, because there is nothing to compare against.

The `confidence` field (`LOW`, `MEDIUM`, `HIGH`) is calculated in code from the number of listings of the same model (3 or more is `MEDIUM`, 8 or more is `HIGH`). It is not asked from the model. Prices are rounded to the nearest 1,000.

Example request:

```json
{
  "brand": "Toyota",
  "model": "Corolla",
  "year": 2019,
  "mileage": 85000,
  "fuelType": "GASOLINE",
  "transmission": "AUTOMATIC",
  "city": "Ankara",
  "description": "No paint, no accident record, regular service"
}
```

The response contains `minPrice`, `suggestedPrice`, `maxPrice`, `currency`, `confidence`, `aiAssisted`, an explanation, the main price factors, the market statistics and the comparable listings that were used.

> The estimate is based on asking prices of listings on this platform. Actual sale prices and the real condition of a car can differ.

### Safety and reliability of the AI layer

- **No hallucinated cars or prices.** Model output is validated against the data that was sent to it.
- **Structured output.** Spring AI maps the model response to Java records, so malformed answers fail early.
- **Prompt injection resistance.** User preferences and listing descriptions are sent as data, and the prompt tells the model not to follow instructions inside them.
- **Authenticated only.** Every AI call costs quota, so the endpoints require a valid JWT.
- **Rate limited.** Each user may make 5 AI requests per minute and 50 per 24 hours (shared by both AI endpoints). Counters live in Redis. Beyond the limit the API returns `429 Too Many Requests` with a `Retry-After` header. If Redis is unreachable, requests are allowed rather than blocked.
- **Graceful failure.** If Gemini is unavailable, the recommendation endpoint returns `503`, and the price endpoint falls back to the statistical range.
- **Low temperature** (0.2) and a short retry policy keep answers consistent and failures fast.

---

## Event-Driven Design with Kafka

Slow or secondary work is moved out of the request path and handled asynchronously.

| Topic | Published by | Consumer (group) | What it does |
|---|---|---|---|
| `offer-events` | Offer service when an offer is created or answered | `OfferNotificationConsumer` (`notification-group`) | E-mails the seller about a new offer and the buyer about the decision. |
| `car-view-events` | Car controller on every listing view | `CarViewConsumer` (`view-counter-group`) | Increments the view counter with an atomic SQL update. |
| `car-status-events` | Car service when the status changes or a listing is deleted | `CarStatusConsumer` (`offer-expiry-group`) | Sets all `PENDING` offers of a car to `EXPIRED` when it becomes `SOLD` or `INACTIVE`. |

Key points:

- **Publish after commit.** Events are sent only after the database transaction commits, so a rolled-back request never produces a phantom event.
- **Resilient producer.** If Kafka is unreachable, the request still succeeds and the failure is logged (`max.block.ms` is limited to 3 seconds).
- **Retries.** If a consumer fails (for example the mail server is briefly down), the record is retried 3 times, 5 seconds apart, then logged and skipped.
- **No e-mail addresses in events.** Events carry user ids only. The consumer reads the address from the database at send time, so personal data does not sit in Kafka.
- **Ordering per entity.** The entity id is used as the message key, so all events of one offer or car land in the same partition and stay ordered.
- **View counting outside the cache.** The detail endpoint is cached, so a cache hit never reaches the service method. The view event is therefore published from the controller, which runs on every request.
- **JSON events** are defined as Java records in the `event` package.

---

## Domain Model

| Entity | Description | Key fields |
|---|---|---|
| `User` | Registered account, implements Spring Security `UserDetails` | email, passwordHash, firstName, lastName, phone, role |
| `Car` | A vehicle listing | brand, model, year, mileage, price, fuelType, transmission, city, description, viewCount, status, seller |
| `CarImage` | Image of a listing | imageUrl, isPrimary, car |
| `Offer` | A buyer's price offer for a car | offeredPrice, note, status, car, buyer |
| `RefreshToken` | Server-side refresh token, one per user | token, expiryDate, revoked, user |

Enumerations:

- `UserRole`: `BUYER`, `SELLER`, `ADMIN`
- `CarStatus`: `ACTIVE`, `PENDING_REVIEW`, `SOLD`, `INACTIVE`
- `OfferStatus`: `PENDING`, `ACCEPTED`, `REJECTED`, `EXPIRED`
- `FuelType`: `GASOLINE`, `DIESEL`, `HYBRID`, `ELECTRIC`
- `Transmission`: `MANUAL`, `AUTOMATIC`

---

## API Overview

Base path: `/intelliCar/api/v1`. Full interactive documentation is available in Swagger UI once the app is running.

Protected endpoints require the header `Authorization: Bearer <access_token>`.

### Auth

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | Public | Create an account and receive tokens |
| POST | `/auth/login` | Public | Log in and receive tokens |
| POST | `/auth/refresh-token` | Public | Exchange a refresh token for a new token pair |
| POST | `/auth/logout` | Required | Revoke the user's refresh token |

### Users

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/users/me` | Required | Current user's profile |
| GET | `/users/{id}` | Required | Profile of a user |

### Cars

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/car` | Required | Create a listing |
| GET | `/car` | Public | List active cars (filter and paginate) |
| GET | `/car/{id}` | Public | Listing details |
| GET | `/car/my-cars` | Required | Current user's listings |
| PUT | `/car/{id}` | Owner | Update price, mileage, description, status |
| DELETE | `/car/{id}` | Owner | Soft delete (status becomes `INACTIVE`) |

Filter parameters for `GET /car` (all optional): `brand`, `model`, `city`, `minPrice`, `maxPrice`, `minYear`, `maxYear`, `fuelType`, `transmission`, plus standard `page`, `size`, `sort`.

```
GET /intelliCar/api/v1/car?brand=bmw&city=ankara&maxPrice=1500000&fuelType=DIESEL&page=0&size=10&sort=price,asc
```

### Car images

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/carImage/{carId}` | Owner | Add images to a listing |
| PATCH | `/carImage/{carId}/images/{imageId}/primary` | Owner | Set the primary image |
| DELETE | `/carImage/{imageId}` | Owner | Delete an image |

### Offers

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/offer` | Required | Make an offer on an active car |
| PATCH | `/offer/{id}/respond?newStatus=ACCEPTED\|REJECTED` | Car owner | Accept or reject an offer |
| GET | `/offer/cars/{carId}` | Car owner | Offers received for a car |
| GET | `/offer/my-offers` | Required | Offers made by the current user |

### AI

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/ai/recommend` | Required | Best cars for a budget and preferences |
| POST | `/ai/price-estimate` | Required | Suggested selling price range for your car |

Both AI endpoints are limited per user. When the limit is exceeded they return `429` with a `Retry-After` header (seconds).

---

## Getting Started

### Prerequisites

- JDK 21
- Docker and Docker Compose
- A Google Gemini API key (free to create at [Google AI Studio](https://aistudio.google.com/apikey))
- (Optional) Maven, the included `./mvnw` wrapper is enough

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/intelliCar.git
cd intelliCar
```

### 2. Create your environment file

```bash
cp .env.example .env
```

Open `.env` and set your own values (see [Configuration](#configuration)). Generate a strong JWT secret with:

```bash
openssl rand -hex 32
```

### 3. Run

**Option A: everything in Docker**

```bash
docker compose up -d --build
```

**Option B: infrastructure in Docker, application from your IDE or terminal**

```bash
docker compose up -d postgres redis kafka mailpit
./mvnw spring-boot:run
```

The application must be started from the project root so that it can read the `.env` file. Do not use both options at the same time, both want port 8080.

### 4. Open the API docs

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

In Swagger UI, call `/auth/register` or `/auth/login`, copy the `accessToken`, click **Authorize** and paste it to call protected endpoints.

### 5. Try the AI features

The AI endpoints work on existing listings, and your own listings are excluded from the results. So create at least two users: one adds a few listings (`POST /car`), the other calls `/ai/recommend` or `/ai/price-estimate`. The more listings of the same brand exist, the better the price estimate.

### 6. See the e-mail notifications

By default e-mails are sent to Mailpit, a development inbox that catches every message and delivers nothing to real people. Open <http://localhost:8025>, then let one user create an offer on another user's listing (the seller gets a mail) and let the seller accept or reject it (the buyer gets a mail).

### Useful commands

```bash
# Stop everything
docker compose down

# Stop and delete all data (database, Redis, Kafka)
docker compose down -v

# Watch Kafka messages
docker exec -it intellicar-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic offer-events --from-beginning

# Run tests (requires PostgreSQL, Redis and Kafka to be running)
./mvnw test
```

---

## Configuration

Secrets are never stored in the repository. They are read from a local `.env` file (ignored by git). A template is provided in `.env.example`.

| Variable | Description | Example |
|---|---|---|
| `POSTGRES_DB` | Database name | `intellicar_db` |
| `POSTGRES_USER` | Database user | `postgres` |
| `POSTGRES_PASSWORD` | Database password | use a strong value |
| `JWT_SECRET` | HMAC signing key for JWTs, at least 32 characters | output of `openssl rand -hex 32` |
| `GEMINI_API_KEY` | Google Gemini API key | from Google AI Studio |
| `GEMINI_MODEL` | (Optional) Gemini model name | default `gemini-3.8-flash` |
| `MAIL_HOST` / `MAIL_PORT` | (Optional) SMTP server | default Mailpit (`mailpit:1025` in Docker, `localhost:1025` otherwise) |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | (Optional) SMTP credentials | Gmail app password |
| `MAIL_SMTP_AUTH` / `MAIL_STARTTLS` | (Optional) Enable authentication and STARTTLS | `true` for Gmail |
| `MAIL_FROM` | (Optional) Sender address | default `no-reply@intellicar.local` |

#### Sending real e-mails

Create an app password for your Google account (<https://myaccount.google.com/apppasswords>) and add this to `.env`:

```
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=you@gmail.com
MAIL_PASSWORD=your_app_password
MAIL_SMTP_AUTH=true
MAIL_STARTTLS=true
MAIL_FROM=you@gmail.com
```

Mails then go to the addresses users registered with.

The application refuses to start if `JWT_SECRET` or `GEMINI_API_KEY` is missing, so a misconfiguration is noticed immediately.

Other settings live in `src/main/resources/application.properties`:

| Property | Default | Description |
|---|---|---|
| `intellicar.security.jwt.access-expiration` | `900000` (15 min) | Access token lifetime in ms |
| `intellicar.security.jwt.refresh-expiration` | `604800000` (7 days) | Refresh token lifetime in ms |
| `spring.ai.google.genai.chat.temperature` | `0.2` | Lower values give more consistent answers |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka address (`kafka:29092` inside Docker) |
| `spring.data.redis.host` / `port` | `localhost` / `6379` | Redis address |
| `intellicar.ai.rate-limit.per-minute` | `5` | AI requests per user per minute (`0` disables this limit) |
| `intellicar.ai.rate-limit.per-day` | `50` | AI requests per user per 24 hours (`0` disables this limit) |
| `intellicar.ai.rate-limit.enabled` | `true` | Turns AI rate limiting on or off |
| `intellicar.notification.email.enabled` | `true` | Turns e-mail notifications on or off |

When the backend runs inside Docker Compose, connection settings are overridden through environment variables in `docker-compose.yaml`.

---

## Troubleshooting

- **The app cannot connect to PostgreSQL or fails with a Hibernate dialect error.** The database is not reachable. Check `docker compose ps`. PostgreSQL only applies `POSTGRES_PASSWORD` when its volume is first created, so after changing the password in `.env` run `docker compose down -v` and start again (this deletes the data).
- **Gemini returns "model not found" (404).** Model names are retired over time. Pick a current model in Google AI Studio and set `GEMINI_MODEL` in `.env`.
- **The app does not start and mentions a missing placeholder.** `JWT_SECRET` or `GEMINI_API_KEY` is not defined in `.env`, or the app was not started from the project root.
- **AI endpoints return 429.** The per-user rate limit was reached. Wait for the number of seconds in the `Retry-After` header or raise the limits in `application.properties`.
- **No e-mails arrive.** Check that Mailpit is running (`docker compose ps`) and open <http://localhost:8025>. For a real SMTP server check the `MAIL_*` values in `.env`. Failed sends are retried 3 times and then logged.
- **AI endpoints return 503.** Gemini is unavailable or the quota is exhausted. Try again later.

---

## Project Structure

```
intelliCar
├── src/main
│   ├── java/com/efeerturk/intelliCar
│   │   ├── ai/            AI helper types and the deterministic price analyzer
│   │   ├── config/        Security, Redis, Kafka, Spring AI, Swagger and application beans
│   │   ├── controller/    REST controllers (including the AI controller)
│   │   ├── dto/           Request and response records
│   │   ├── enums/         Domain enumerations and error message types
│   │   ├── event/         Kafka event records
│   │   ├── exception/     Custom exceptions and error models
│   │   ├── handler/       Global exception handler
│   │   ├── jwt/           JWT service, authentication filter, entry point
│   │   ├── kafka/         Event publisher, topic names
│   │   │   └── consumer/  Kafka listeners
│   │   ├── mapper/        MapStruct mappers
│   │   ├── notification/  E-mail templates and sender
│   │   ├── model/         JPA entities
│   │   ├── ratelimit/     AI rate limiter, Redis counter and interceptor
│   │   ├── repository/    Spring Data repositories and JPA specifications
│   │   └── service/       Service interfaces and implementations (impl/)
│   └── resources
│       └── application.properties
├── src/test               Tests
├── Dockerfile             Multi-stage build (Maven, then a slim JRE image)
├── docker-compose.yaml    PostgreSQL, Redis, Kafka, Mailpit and backend
├── .env.example           Environment variable template
└── pom.xml
```

---

## Design Decisions

- **Retrieve first, then let the model reason.** The AI only sees listings that the database selected, and everything it returns is validated against that data.
- **Deterministic code for numbers, AI for judgment.** Market statistics, plausibility checks and confidence levels are calculated in plain Java and unit-testable without a model.
- **Stateless authentication.** Short-lived JWT access tokens plus a refresh token stored in the database, so a session can be revoked server-side (deleted on logout or when expired).
- **Records for DTOs and events.** Immutable, concise and easy to serialize.
- **Soft delete.** Deleting a listing sets its status to `INACTIVE` instead of removing rows, which keeps offer history intact.
- **Specification-based filtering.** Filters are optional and combined dynamically into a single query. Only `ACTIVE` listings appear in search results.
- **Ownership checks in the service layer.** Only the seller of a car can update it, manage its images, or answer its offers.
- **Rate limits in Redis, fail-open.** Counters are shared by all application instances. If Redis is down, AI calls are allowed instead of blocked.
- **Notifications decoupled from requests.** E-mails are sent by a Kafka consumer, so a slow or unavailable mail server never slows down or breaks an offer request.
- **Docker image.** Multi-stage build, runs as a non-root user and respects container memory limits.

---

## Roadmap

- [x] Event-driven processing with Kafka
- [x] AI budget assistant: rank the best cars for a user's budget (Spring AI)
- [x] AI price estimation: suggest a price range from similar listings (Spring AI)
- [x] Rate limiting for the AI endpoints
- [x] Real e-mail delivery for offer notifications
- [ ] Push notifications (needs a client app and device tokens)
- [ ] Integration tests with Testcontainers (PostgreSQL, Redis, Kafka)
- [ ] CI pipeline with GitHub Actions
- [ ] Admin moderation flow for `PENDING_REVIEW` listings

---

## Author

Developed by **Efe Erturk** as a graduation project.
