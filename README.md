# Basket Service

A REST service for browsing a product catalogue and managing a shopping basket,
built with Java 21 and Spring Boot 3. A customer can list products, add/remove/update
items in a basket, and finalise the basket for checkout to receive a priced breakdown.

> Scope note: there is no UI (a separate team owns that) and no payment/checkout
> processing. The service exposes a clean REST contract that a UI can consume.

---

## Tech stack

| Concern              | Choice                                   |
|----------------------|------------------------------------------|
| Language / framework | Java 21, Spring Boot 3.3                  |
| Web                  | Spring MVC (`spring-boot-starter-web`)   |
| Persistence          | Spring Data JPA + Hibernate              |
| Database             | H2 (in-memory) — see production notes    |
| Validation           | Jakarta Bean Validation                  |
| API docs             | springdoc-openapi (Swagger UI)           |
| Build                | Maven                                    |
| Tests                | JUnit 5, Mockito, Spring MockMvc         |

No Lombok: entities are written explicitly (to control `equals`/`hashCode` and
avoid lazy-loading pitfalls) and DTOs are Java `record`s.

---

## Running it

Prerequisites: JDK 21+ and Maven 3.9+.

```bash
# from the project root
mvn spring-boot:run
```

Then:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- H2 console: http://localhost:8080/h2-console
  (JDBC URL `jdbc:h2:mem:basketdb`, user `sa`, empty password)

Run the tests:

```bash
mvn test
```

On startup the catalogue is seeded with a handful of imaginary products
(see `DataSeeder`), including one deliberately out-of-stock item so the
insufficient-stock path is easy to exercise.

---

## API reference

Base path: `/api/v1`

### Products

| Method | Path             | Description                          |
|--------|------------------|--------------------------------------|
| GET    | `/products`      | List products (paginated)            |
| GET    | `/products/{id}` | Get a single product                 |

### Baskets

| Method | Path                                  | Description                                   |
|--------|---------------------------------------|-----------------------------------------------|
| POST   | `/baskets`                            | Create a basket (returns `201` + `Created`)  |
| GET    | `/baskets/{basketId}`                 | Get a basket and its contents                 |
| POST   | `/baskets/{basketId}/items`           | Add a product (increments existing quantity)  |
| PUT    | `/baskets/{basketId}/items/{productId}` | Set the absolute quantity of a line         |
| DELETE | `/baskets/{basketId}/items/{productId}` | Remove a line                               |
| POST   | `/baskets/{basketId}/checkout`        | Finalise the basket, return priced breakdown  |

### Example flow (curl)

```bash
# Create a basket
curl -s -X POST http://localhost:8080/api/v1/baskets \
  -H 'Content-Type: application/json' -d '{"customerId":"customer-123"}'

# List products to get an id
curl -s http://localhost:8080/api/v1/products

# Add 2 of a product
curl -s -X POST http://localhost:8080/api/v1/baskets/<basketId>/items \
  -H 'Content-Type: application/json' \
  -d '{"productId":"<productId>","quantity":2}'

# Checkout
curl -s -X POST http://localhost:8080/api/v1/baskets/<basketId>/checkout
```

A ready-to-use request collection is in [`requests.http`](./requests.http).

### Checkout response shape

```json
{
  "basketId": "…",
  "status": "CHECKED_OUT",
  "items": [
    { "productId": "…", "productName": "Mechanical Keyboard",
      "quantity": 2, "unitPrice": 89.99, "lineTotal": 179.98 }
  ],
  "totalItems": 2,
  "totalAmount": 179.98
}
```

### Error model

Every non-2xx response uses one consistent shape:

```json
{
  "timestamp": "2026-06-23T10:15:30Z",
  "status": 409,
  "error": "Conflict",
  "message": "Insufficient stock for product … : requested 4, available 3",
  "path": "/api/v1/baskets/…/items",
  "fieldErrors": []
}
```

| Status | When                                                              |
|--------|-------------------------------------------------------------------|
| 400    | Request body fails validation (`fieldErrors` is populated)        |
| 404    | Basket / product / line not found                                 |
| 409    | Insufficient stock, edit after checkout, or concurrent modification |
| 422    | Checkout of an empty basket                                       |
| 500    | Unexpected error (details logged server-side, never leaked)       |

---

## Design decisions

**Package-by-feature.** Code is grouped by domain (`product`, `basket`, `common`,
`config`) rather than by technical layer, so a feature's controller, service,
entity and DTOs sit together and the module stays cohesive as it grows.

**Entities are not exposed over HTTP.** Controllers speak in DTOs (`record`s).
The JPA model can evolve without breaking the API contract the UI team depends on.

**The `Basket` is an aggregate root.** All mutations go through behaviour methods
(`addItem`, `setItemQuantity`, `removeItem`, `checkout`) that enforce invariants
("no duplicate product lines", "no edits after checkout") in one place. Services
validate up front and throw domain exceptions; the entity guards are defence in depth.

**Money uses `BigDecimal`** everywhere, scaled to 2 dp with `HALF_UP` at the API
boundary. `double` is never used for currency.

**Price is snapshotted when an item is added/updated.** The unit price is copied
onto the basket line at that moment ("price locked when added to basket"), so the
basket total is stable even if the catalogue price later changes. Live re-pricing
is a legitimate alternative — the trade-off is discussed below.

**Stock is validated but not reserved.** Adding/updating an item checks the
requested quantity against available stock, and checkout re-validates. The service
never decrements warehouse stock, because inventory is owned by another service in
this design (the catalogue data "comes from another service"). See below for how
real reservation would work.

**Checkout freezes the basket.** It transitions `ACTIVE → CHECKED_OUT`, after which
the basket is immutable. Re-calling checkout is idempotent (returns the same summary).

**Optimistic locking** (`@Version` on `Basket` and `Product`) guards against lost
updates from concurrent edits; conflicts surface as `409`.

---

## Assumptions & scope boundaries

- One basket is identified by its own UUID. With no auth in scope, `customerId` is
  an optional free-text reference; a real deployment would derive it from the
  authenticated principal and never trust a client-supplied value.
- The product catalogue is seeded locally as a stand-in for the external
  catalogue/inventory service.
- Checkout returns a quote and finalises the basket; payment, order creation and
  stock decrement are explicitly out of scope.
- No discounts, taxes, shipping or multi-currency yet (see "evolving requirements").

---

## Thinking beyond the assignment

The brief asks to treat this as an evolving product, not a one-off. Below are the
challenges I'd expect and how I'd approach them. Most are intentionally **not**
implemented here — the goal is to show the path.

### Stock reservation (the central trade-off)

Validating stock at add-time does not *reserve* it, so two customers can both
"successfully" add the last unit and one is disappointed at checkout/payment.
Options, roughly in order of complexity:

1. **Validate only (current approach).** Simple, no held inventory, but can oversell.
   Fine for low-throughput and where the order service is the real source of truth.
2. **Soft reservation with TTL.** Decrement an "available" counter when added to a
   basket and release it after N minutes of inactivity or on basket abandonment.
   Needs a reaper job and careful release-on-edit logic.
3. **Reserve at checkout only.** Keep baskets cheap; attempt an atomic reservation
   against the inventory service when the customer checks out, and fail fast if it
   can't be met. This is what most large e-commerce systems do.

In all cases the authoritative decrement belongs to the **inventory/order service**,
ideally coordinated via events (e.g. an `OrderPlaced` event reduces stock) rather
than this service mutating warehouse counts directly.

### Pricing and price changes

Snapshot-at-add (current) vs. live re-pricing vs. snapshot-at-checkout are business
decisions. A mature system usually: shows live prices while browsing, locks the
price into the **order** at checkout (with a short validity window), and reconciles
if a price changed between basket and order. Promotions, coupons, taxes (VAT/GST by
jurisdiction), shipping and multi-currency would be modelled as an explicit pricing
pipeline producing a line-by-line breakdown — the `CheckoutResponse` is already
shaped to carry that breakdown.

### Concurrency

Optimistic locking handles concurrent edits to the same basket. For high contention
on popular products, stock reservation would need either DB-level atomic decrements
(`UPDATE … SET qty = qty - ? WHERE qty >= ?`) or a dedicated inventory service with
its own concurrency control.

### Production data storage

H2 is for the exercise only. In production I'd choose based on access pattern:

- **Active baskets** are short-lived, high-churn, key-by-id reads/writes — a great
  fit for a **key-value / document store with TTL** (e.g. Redis or DynamoDB), which
  also gives automatic expiry of abandoned baskets.
- **Orders / checkout records** are long-lived and need integrity and reporting — a
  relational store (PostgreSQL) with proper migrations (Flyway/Liquibase).
- **Catalogue** is read-heavy and owned elsewhere; this service would consume it via
  API/events and cache it.

So "one database" is likely the wrong framing: baskets and orders have different
lifecycles and consistency needs. The repository abstraction here keeps that swap localised.

### Scaling

The service is stateless (basket state lives in the DB), so it scales horizontally
behind a load balancer with no sticky sessions. Steps as load grows: add a read
cache for the catalogue; move active baskets to Redis to take write pressure off the
relational DB; introduce connection pooling limits and rate limiting; split read and
write paths if needed; and adopt async/event-driven integration with inventory and
order services so checkout isn't a synchronous fan-out.

### Security & multi-tenancy

Add authentication (OAuth2/OIDC) and authorise every basket operation against the
caller — a customer must only touch their own basket. Validate and rate-limit input,
and never trust client-supplied identity. The `customerId` field becomes a non-null,
indexed, server-derived value.

### Basket lifecycle

Abandoned baskets accumulate. I'd add a `createdAt`/`updatedAt`-based TTL (already
tracked on the entity) plus a cleanup/expiry job, and optionally "saved for later"
baskets that persist longer with explicit semantics.

### API evolution

The API is versioned (`/api/v1`). Additive changes stay within v1; breaking changes
go to v2 with a deprecation window. Idempotency keys on `POST /baskets` and
`/checkout` would make retries safe for clients.

### Observability & deployment

For production: structured JSON logging with correlation IDs, Micrometer metrics +
Prometheus/Grafana dashboards, distributed tracing (OpenTelemetry), and health
probes via Spring Boot Actuator. Package as a container, deploy via CI/CD to
Kubernetes (or similar) with rolling deploys, and externalise config per environment.

---

## Project structure

```
src/main/java/com/example/basketservice
├── BasketServiceApplication.java
├── product/                 # catalogue: entity, repo, service, controller, DTO
├── basket/                  # basket aggregate: entity, item, service, controller, mapper, DTOs
├── common/
│   ├── config/              # DataSeeder, JPA auditing, OpenAPI config
│   ├── dto/                 # PagedResponse
│   └── exception/           # domain exceptions + global handler + ApiError
```

## Known limitations / what I'd do next

- No stock reservation (validate-only) — documented above.
- No auth — every basket is currently reachable by id.
- Catalogue is seeded locally rather than consumed from a real source.
- Pricing is flat (no tax/discount/shipping) — the response shape anticipates it.