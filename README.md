# Vape Deal Spot — Web Application (`vapedealspot-webapp`)

Unified full-stack affiliate e-commerce platform and administrative management portal powered by Spring Boot and Thymeleaf server-side rendering.

---

## 1. Technology Architecture

- **Java Version:** 25 LTS
- **Framework:** Spring Boot 4.1.2
- **Presentation Layer:** Spring MVC + Thymeleaf server-rendered HTML5, Vanilla JavaScript, CSS3 design tokens
- **Data Access:** Spring Data JPA + Hibernate
- **Database Schema Policy:** `spring.jpa.hibernate.ddl-auto=validate` (Manual SQL migrations only; zero runtime migration tools)
- **Database:** MySQL 8.x
- **Build Tool:** Maven (`mvnw` wrapper, artifactId: `vapedealspot-webapp`)
- **Packaging:** Executable self-contained JAR (`target/vapedealspot-webapp.jar`)

---

## 2. Authentication & Authorization Foundation

- **Authentication:** Custom Spring MVC session authentication using `HttpSession`.
- **Authorization:** Authoritative database-backed Role-Based Access Control (RBAC) enforced by `AdminAuthInterceptor`.
- **CSRF Protection:** Server-side session-based CSRF protection enforced across all mutating requests (POST, PUT, PATCH, DELETE).

---

## 3. Core Modules & Endpoints

### Customer V2 Storefront
- **Home:** `GET /`
- **Product Search & Filtering:** `GET /search`
- **Product Detail:** `GET /product/{slug}`
- **Categories:** `GET /categories`, `GET /category/{slug}`
- **Brands:** `GET /brands`, `GET /brand/{slug}`
- **Deals & Coupons:** `GET /deals`
- **Editorial / Curated Collections:** `GET /best-of`
- **Blog & News:** `GET /blog`, `GET /blog/{slug}`
- **SEO & Discovery:** `GET /robots.txt`, `GET /sitemap.xml`
- **System Health:** `GET /api/health`

### Admin Management Portal
- **Login / Logout:** `GET /admin/login`, `POST /admin/login`, `POST /admin/logout`
- **Admin Dashboard:** `GET /admin/dashboard`
- **Users Management:** `GET /admin/users`, `POST /admin/users` (create, edit, toggle status)
- **Roles & Permissions:** `GET /admin/roles`, `POST /admin/roles` (create, edit, delete)
- **Product Catalog Management:** `GET /admin/products`
- **Page Builder CMS:** `GET /admin/page-builder`, draft previews, publication pipeline, and version history restore

---

## 4. Strict Environment Separation

The application strictly supports exactly three environments:

| Environment | Profile | Database Target | SSL / TLS | Session Cookie Security |
| :--- | :--- | :--- | :--- | :--- |
| **Development** | `dev` | `vapedealspot` | Optional | `Secure=false`, `HttpOnly=true`, `SameSite=Lax` |
| **Staging** | `staging` | `vapedealspot_staging` | Required | `Secure=true`, `HttpOnly=true`, `SameSite=Lax` |
| **Production** | `prod` | `vapedealspot_prod` | Required | `Secure=true`, `HttpOnly=true`, `SameSite=Lax` |

*Note: `uat` and `test` profiles are prohibited and rejected at startup by `EnvironmentValidator`.*

---

## 5. Local Setup & Execution

### Prerequisites
- JDK 25 LTS installed and configured in `PATH`
- MySQL 8.x running locally with a database named `vapedealspot`

### Running with Maven Wrapper
```bash
# Run locally in development mode
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Building & Running Self-Contained JAR
```bash
# Package application into executable JAR
./mvnw clean package "-DskipTests"

# Run the packaged JAR
java -jar target/vapedealspot-webapp.jar --spring.profiles.active=dev
```

---

## 6. Database Schema Management

Schema migrations are applied manually following the structured 6-step procedure documented in:
[`database/manual-migrations/README.md`](database/manual-migrations/README.md).
