# Ecommerce Platform

A microservices-based e-commerce backend demonstrating event-driven architecture, distributed JWT authentication, and the transactional outbox pattern.

## Architecture Overview

The platform is composed of three independent Spring Boot microservices, each owning its own PostgreSQL schema with no direct cross-service database access. Services authenticate requests statelessly: every service validates incoming JWTs independently using a shared HMAC signing secret, so no network call to an auth service is required on the request path. Asynchronous workflows — most notably the checkout process — are coordinated via Apache Kafka using a choreography-based saga, where each service reacts to events published by the others rather than being directed by a central orchestrator.

```
                     JWT (shared HMAC secret, validated independently)
        ┌────────────────────┬────────────────────┬────────────────────┐
        │                    │                    │
 auth-service          catalog-inventory-service   order-payment-service
   (8081)                     (8082)                     (8083)
 issues JWTs,          products, categories,       cart, checkout,
 user accounts,        inventory                   orders, payments
 roles

Checkout saga flow (via Kafka):

 order-payment-service ──[order.created]──────────────────► catalog-inventory-service
 catalog-inventory-service ──[inventory.stock.reserved]────► order-payment-service
                        or
 catalog-inventory-service ──[inventory.stock.reservation-failed]──► order-payment-service

 order-payment-service ──[payment.succeeded / payment.failed]────► (future consumers)
 order-payment-service ──[order.confirmed / order.cancelled]─────► (future consumers)
```

## Tech Stack

| Category            | Technology                                      |
|---------------------|--------------------------------------------------|
| Language / Runtime  | Java 21                                          |
| Framework           | Spring Boot 3.5.4                                |
| Persistence         | Spring Data JPA, PostgreSQL 16 (schema-per-service) |
| Security            | Spring Security, JWT (jjwt)                      |
| Messaging           | Spring Kafka, Apache Kafka (KRaft mode, no Zookeeper) |
| Migrations          | Flyway                                           |
| Build               | Maven multi-module monorepo                      |
| Boilerplate         | Lombok                                           |
| API Documentation   | springdoc-openapi / Swagger UI                   |

## Key Architectural Patterns Demonstrated

- **Schema-per-service database isolation** — each service has its own PostgreSQL schema and a dedicated DB role, with cross-schema access explicitly revoked.
- **Stateless JWT authentication** — every service validates JWTs independently using a shared HMAC secret, requiring no session state or per-request calls back to auth-service.
- **Transactional outbox pattern** — database writes and outbound events are committed atomically in a single transaction, with a background poller reliably delivering queued events to Kafka.
- **Idempotent Kafka consumers** — a processed-events table records handled message IDs, allowing consumers to safely tolerate Kafka's at-least-once delivery guarantee.
- **Choreography-based saga** — the checkout flow is coordinated purely through events exchanged between services, with no central saga orchestrator.
- **Optimistic locking** — JPA `@Version` fields protect inventory records from lost updates during concurrent stock reservation.

## Project Structure

```
ecommerce-platform/
├── pom.xml                      # Root Maven parent (multi-module)
├── docker-compose.yml           # Kafka broker (and optionally Postgres)
├── auth-service/                # User accounts, roles, JWT issuance (8081)
├── catalog-inventory-service/   # Products, categories, inventory (8082)
├── order-payment-service/       # Cart, checkout, orders, payments (8083)
└── infra/
    └── postgres/
        └── init-schemas.sql     # Creates per-service schemas and DB roles
```

## Prerequisites

- Java 21 (Eclipse Temurin recommended)
- Maven 3.9+
- PostgreSQL 16+ (native install or Docker)
- Docker Desktop (for running Kafka)
- Postman or the built-in Swagger UI for testing

## Local Setup and Running

1. **Clone the repository**

   ```bash
   git clone <repository-url>
   cd ecommerce-platform
   ```

2. **Start PostgreSQL** and run the schema/role initialization script once:

   ```bash
   psql -U postgres -f infra/postgres/init-schemas.sql
   ```

   This creates the schemas and service-specific database roles used by each service, with cross-schema access revoked.

3. **Start Kafka:**

   ```bash
   docker compose up kafka -d
   ```

4. **Run each service** in its own terminal:

   ```bash
   cd auth-service && mvn spring-boot:run                # http://localhost:8081
   cd catalog-inventory-service && mvn spring-boot:run    # http://localhost:8082
   cd order-payment-service && mvn spring-boot:run        # http://localhost:8083
   ```

5. **Access Swagger UI** for each service:

   - http://localhost:8081/swagger-ui.html
   - http://localhost:8082/swagger-ui.html
   - http://localhost:8083/swagger-ui.html

## Testing the Checkout Saga End to End

1. Register a user via `auth-service` (`POST /api/auth/register`).
2. Log in to obtain a JWT (`POST /api/auth/login`).
3. Add an item to the cart via `order-payment-service`, using the JWT for authentication.
4. Initiate checkout (`POST /api/orders/checkout`).
5. Observe the order status: it starts as `PENDING` and transitions asynchronously — via the Kafka-driven saga with `catalog-inventory-service` — to either `CONFIRMED` (stock reserved successfully) or `CANCELLED` (insufficient stock).

## Known Limitations / Not Yet Implemented

- Product pricing and names are currently passed directly by the API client rather than looked up server-side via gRPC from `catalog-inventory-service`. This is a deliberate, temporary gap — see the code comments on `AddCartItemRequest` / `CheckoutRequest`.
- `PaymentService` is a mock that always succeeds; there is no real payment gateway integration yet.
- There is no scheduled job to auto-release expired, unconfirmed stock reservations.
- There is no API Gateway service yet; each service is accessed directly on its own port.
- There is no `notification-service` yet to react to order confirmation/cancellation events.

## License

This project is for portfolio/educational purposes.
