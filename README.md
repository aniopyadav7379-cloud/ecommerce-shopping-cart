# ShopCart — E-Commerce Shopping Cart System

A complete, production-ready, end-to-end shopping cart system built with Spring Boot, Spring Data JPA, and Thymeleaf.

## 1. Description

ShopCart is a full-stack e-commerce shopping cart application. Shoppers can browse a product catalogue, search for
products, view product details, manage a persistent shopping cart, and complete a validated, transactional checkout
that produces a permanent order record. All business rules — pricing, totals, and stock — are enforced on the
server; nothing from the browser is trusted.

## 2. Features

- Product catalogue with responsive product cards (image, name, description, price, stock)
- Case-insensitive product search (`?q=`)
- Dedicated product details page with a stock-aware quantity selector
- Persistent shopping cart (survives page navigation and browser restarts via a cookie-backed cart id)
- Add / increase / decrease / set quantity / remove / clear cart
- Server-side stock validation on every cart mutation and again at checkout
- Checkout with customer name, email, and shipping address validation
- Transactional order creation: cart validation → stock re-check → order + order items → stock deduction → cart
  clear, all inside one `@Transactional` boundary — a failure anywhere rolls back everything
- Order confirmation page and REST endpoint
- Clean REST API with structured JSON error responses
- Centralized error handling (no stack traces ever reach the client)
- Responsive UI for desktop, tablet, and mobile
- Real product photography and branded visual identity (logo, hero banner, footer brand strip, Font Awesome icons) sourced from a licensed front-end asset kit
- Automated unit and integration tests (JUnit 5, Mockito, Spring Boot Test, MockMvc)

## 3. Technology Stack

**Backend:** Java 17, Spring Boot 3.3, Spring MVC, Spring Data JPA (Hibernate), Bean Validation, Maven
**Database:** H2 (file-based, for local development) / PostgreSQL (production)
**Frontend:** Thymeleaf, HTML5, CSS3, vanilla JavaScript
**Testing:** JUnit 5, Mockito, Spring Boot Test, MockMvc, AssertJ

## 4. Architecture

Layered, MVC + REST, with a strict separation of concerns:

```
Browser
   │  HTTP
   ▼
Frontend (Thymeleaf + CSS + JS)
   │
   ▼
Controller Layer          WebController (pages)   ApiController (REST JSON)
   │                       — no business logic in either controller
   ▼
Service Layer             ProductService · CartService · CheckoutService
   │                       — all business rules live here
   ▼
Repository Layer          ProductRepository · CartItemRepository ·
                           OrderRepository · OrderItemRepository
   │
   ▼
JPA / Hibernate
   │
   ▼
Database (H2 / PostgreSQL)
```

Errors are handled by two focused `@ControllerAdvice` classes: `GlobalExceptionHandler` (JSON, scoped to
`ApiController`) and `WebExceptionHandler` (renders `error.html`, scoped to `WebController`).

## 5. Project Structure

```
src/
 ├── main/
 │   ├── java/com/example/ecommerce/
 │   │   ├── EcommerceApplication.java
 │   │   ├── config/
 │   │   │   ├── CartIdProvider.java      # resolves/creates the guest cart-id cookie
 │   │   │   └── DataInitializer.java     # seeds sample products once
 │   │   ├── model/
 │   │   │   ├── Product.java
 │   │   │   ├── CartItem.java
 │   │   │   ├── Order.java
 │   │   │   ├── OrderItem.java
 │   │   │   └── OrderStatus.java
 │   │   ├── repository/
 │   │   │   ├── ProductRepository.java
 │   │   │   ├── CartItemRepository.java
 │   │   │   ├── OrderRepository.java
 │   │   │   └── OrderItemRepository.java
 │   │   ├── service/
 │   │   │   ├── ProductService.java
 │   │   │   ├── CartService.java
 │   │   │   └── CheckoutService.java
 │   │   ├── controller/
 │   │   │   ├── WebController.java
 │   │   │   └── ApiController.java
 │   │   ├── dto/                         # request/response DTOs
 │   │   └── exception/
 │   │       ├── ApiException.java + subclasses
 │   │       ├── ErrorCode.java
 │   │       ├── GlobalExceptionHandler.java
 │   │       └── WebExceptionHandler.java
 │   └── resources/
 │       ├── templates/                   # index, product, cart, checkout, order, error
 │       ├── static/{css,js}
 │       └── application.properties
 └── test/
     ├── java/com/example/ecommerce/service/   # unit tests (Mockito)
     └── java/com/example/ecommerce/controller/# integration tests (MockMvc + H2)
```

## 6. Database Design

**products** — id, name, description, price (`numeric(12,2)`), stock, image_url, created_at, updated_at

**cart_items** — id, cart_id, product_id (FK → products), quantity, created_at, updated_at
Unique constraint on `(cart_id, product_id)` — a product can only appear once per cart; re-adding increases quantity.

**orders** — id, customer_name, email, shipping_address, total_amount (`numeric(12,2)`), status (enum string),
created_at

**order_items** — id, order_id (FK → orders), product_id (FK → products), product_name, unit_price, quantity,
subtotal
`product_name` and `unit_price` are snapshotted at purchase time, so an order's history never changes even if the
product is later renamed or repriced.

All monetary fields use `BigDecimal` — floating point is never used for currency anywhere in the codebase.

## 7. Application Flow

1. **Browse** — `GET /` lists all products; `?q=` filters by name (case-insensitive).
2. **View details** — `GET /product/{id}` shows full details and a stock-capped quantity selector.
3. **Add to cart** — cart id is resolved from (or set into) an `HttpOnly` cookie; the item is persisted immediately.
   Adding a product already in the cart adds to the existing quantity and re-validates against current stock.
4. **Manage cart** — `GET /cart` shows persisted items with server-calculated subtotals and total.
5. **Checkout** — `GET/POST /checkout` validates customer name, email, and address, then calls `CheckoutService`
   inside a single transaction: reload products under a pessimistic write lock → re-validate stock → build the
   order and order items → deduct stock → save the order → clear the cart. Any failure rolls back the entire
   transaction — no partial orders, no partial stock deduction.
6. **Confirmation** — `GET /order/{id}` shows the persisted order.

## 8. API Documentation

All API responses are JSON. Errors follow this shape:

```json
{
  "timestamp": "2026-09-01T10:15:30",
  "status": 409,
  "error": "INSUFFICIENT_STOCK",
  "message": "Only 2 unit(s) of \"Rare Collectible\" are available."
}
```

### Products
| Method | Path | Description |
|---|---|---|
| GET | `/api/products` | List all products, or filter with `?q=` |
| GET | `/api/products/{id}` | Get a single product |
| GET | `/api/products/search?q=` | Explicit search endpoint (same behavior as `?q=` above) |

### Cart
| Method | Path | Body | Description |
|---|---|---|---|
| GET | `/api/cart` | — | Current cart contents and total |
| POST | `/api/cart/items` | `{"productId":1,"quantity":2}` | Add to cart (accumulates if already present) |
| PATCH | `/api/cart/items/{productId}` | `{"quantity":3}` | Set an item's quantity |
| DELETE | `/api/cart/items/{productId}` | — | Remove one item |
| DELETE | `/api/cart` | — | Clear the cart |

### Checkout / Orders
| Method | Path | Body | Description |
|---|---|---|---|
| POST | `/api/checkout` | `{"customerName":"...","email":"...","shippingAddress":"..."}` | Places the order |
| GET | `/api/orders/{id}` | — | Fetch an order |

### Example requests

```bash
curl -c cookies.txt -X POST http://localhost:8080/api/cart/items \
  -H "Content-Type: application/json" \
  -d '{"productId": 1, "quantity": 2}'

curl -b cookies.txt -X PATCH http://localhost:8080/api/cart/items/1 \
  -H "Content-Type: application/json" \
  -d '{"quantity": 3}'

curl -b cookies.txt -X POST http://localhost:8080/api/checkout \
  -H "Content-Type: application/json" \
  -d '{"customerName": "Anoop Yadav", "email": "anoop@example.com", "shippingAddress": "New Delhi, India"}'
```

## 9. Installation & Requirements

- JDK 17+
- Maven 3.8+
- Internet access to Maven Central on first build (to download dependencies)
- PostgreSQL 13+ only if you intend to run against Postgres instead of the default H2

## 10. How to Run

```bash
# Build and run tests
mvn clean test

# Run the application (H2 by default)
mvn spring-boot:run
```

Then open:
- Application: http://localhost:8080
- H2 console: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:./data/ecommercedb;AUTO_SERVER=TRUE`
  - Username: `sa`, Password: *(blank)*

## 11. H2 Configuration

H2 runs in **file mode** (`./data/ecommercedb.mv.db`) so data — including seeded products and any orders you place —
survives an application restart. The seed data in `DataInitializer` only inserts when the `products` table is
empty, so restarting never duplicates it.

## 12. PostgreSQL Configuration

For production, set these environment variables (never hard-code credentials):

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/ecommerce
export SPRING_DATASOURCE_USERNAME=<username>
export SPRING_DATASOURCE_PASSWORD=<password>
export SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
export SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect

mvn spring-boot:run
```

`spring.jpa.hibernate.ddl-auto=update` will create/update the schema automatically on both H2 and PostgreSQL. For a
real production rollout, replace this with a managed migration tool (Flyway/Liquibase) instead of `ddl-auto`.

## 13. Testing

```bash
mvn clean test
```

- **Unit tests** (`ProductServiceTest`, `CartServiceTest`, `CheckoutServiceTest`) mock the repository layer with
  Mockito and cover: listing, case-insensitive search, product-not-found, add/accumulate/remove/clear cart, stock
  rejection, and checkout atomicity (empty cart rejected, insufficient stock rejected and nothing persisted, stock
  deducted and cart cleared only on success, stock never goes negative).
- **Integration tests** (`ApiControllerIntegrationTest`) run the full Spring context against an in-memory H2
  database via MockMvc and exercise the real HTTP endpoints end-to-end, including the cart cookie flow, validation
  error responses, and a full add → checkout → verify-stock-deducted → verify-cart-cleared cycle.

## 14. Known Limitations

- No authentication/authorization — carts are anonymous and identified only by a cookie, as scoped by the
  requirements.
- Order status beyond `CONFIRMED` (PROCESSING/SHIPPED/DELIVERED/CANCELLED) is modeled but there's no admin workflow
  to transition an order through those states yet.
- `ddl-auto=update` is convenient for this project but is not a substitute for real schema migrations in
  production.
- Product images are served from an image CDN (Unsplash) for seed data — swap `imageUrl` for real product
  photography in production.

## 15. Future Enhancements

- Admin dashboard for product and order management
- User accounts, authentication (Spring Security), and order history per user
- Payment gateway integration
- Product categories/filters and pagination for large catalogues
- Email notifications on order confirmation
- Flyway/Liquibase-managed schema migrations

## 16. Screenshots

_Add screenshots of the catalogue, product details, cart, checkout, and order confirmation pages here._

## 17. Visual Assets

The logo, hero banner, footer brand strip, favicon, and Font Awesome icon set under
`src/main/resources/static/assets/` come from a purchased/licensed front-end template kit provided as a design
reference for this build. They are used here for a consistent visual identity; swap them for your own branding
before any public deployment. Product photography in `DataInitializer` is sourced separately from Unsplash.
