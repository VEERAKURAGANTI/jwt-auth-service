# 🔐 JWT Auth Service

> A production-ready **JWT Authentication & Authorization** REST API built with Spring Boot 3.5, Spring Security 6, and PostgreSQL — featuring role-based access control, token refresh, token blacklist on logout, and a complete built-in frontend UI.

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.13-brightgreen?style=flat-square&logo=springboot)
![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-blue?style=flat-square&logo=postgresql)
![JWT](https://img.shields.io/badge/JWT-0.12.3-black?style=flat-square&logo=jsonwebtokens)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

**[Live Demo](https://your-app.up.railway.app)** · **[API Docs](https://your-app.up.railway.app/swagger-ui.html)** · **[Report Bug](https://github.com/your-username/jwt-auth-service/issues)**

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Getting Started](#-getting-started)
- [API Reference](#-api-reference)
- [Project Structure](#-project-structure)
- [Environment Variables](#-environment-variables)
- [Database Schema](#-database-schema)
- [Security](#-security)
- [Deployment](#-deployment)
- [Running Tests](#-running-tests)
- [License](#-license)

---

## 🌟 Overview

JWT Auth Service is a complete authentication backend that handles everything from user registration to role-based access control. It uses **stateless JWT access tokens** — the server stores no session state, making it horizontally scalable.

The project includes a **built-in frontend UI** served at the root URL, with pages for login, registration, token inspection, an API tester, user management, and role management.

```
Client ──► POST /api/auth/login   ──► Server validates credentials (BCrypt)
       ◄── accessToken (15 min JWT) + refreshToken (7 day UUID stored in DB)

Client ──► GET /api/users/me      ──► Authorization: Bearer <accessToken>
       ◄── User profile data

Client ──► POST /api/auth/refresh ──► { "refreshToken": "..." }
       ◄── New accessToken

Client ──► POST /api/auth/logout  ──► Token blacklisted + refresh deleted
       ◄── Session fully terminated
```

---

## ✨ Features

| Feature | Details |
|---|---|
| **JWT Authentication** | Stateless tokens signed with HMAC-SHA256 |
| **Token Refresh** | Long-lived refresh tokens (DB-backed), short-lived access tokens |
| **Instant Token Revocation** | In-memory blacklist invalidates access tokens immediately on logout |
| **Role-Based Access Control** | ROLE_USER · ROLE_MODERATOR · ROLE_ADMIN |
| **BCrypt Passwords** | One-way hashed with random salt — never stored plain text |
| **Input Validation** | All DTOs validated with @Valid, clean 400 error responses |
| **Global Error Handling** | Consistent JSON error responses across all exceptions |
| **Swagger UI** | Interactive API docs with JWT bearer auth at /swagger-ui.html |
| **Built-in Frontend** | Dashboard, token inspector, API tester, user table, role manager |
| **Auto Data Seeding** | Roles and default admin seeded automatically on startup |
| **Docker Support** | Multi-stage Dockerfile for lean production images |
| **Profile Separation** | Separate dev and prod configs using Spring profiles |

---

## 🛠 Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Framework | Spring Boot | 3.5.13 |
| Language | Java | 21 |
| Security | Spring Security | 6.x |
| JWT | JJWT | 0.12.3 |
| Database | PostgreSQL | 14+ |
| ORM | Spring Data JPA / Hibernate | 6.x |
| Password Hashing | BCryptPasswordEncoder | — |
| API Docs | SpringDoc OpenAPI | 2.8.6 |
| Build Tool | Maven | 3.9+ |
| Containerization | Docker | Multi-stage |

---

## 🏗 Architecture

```
┌───────────────────────────────────────────────────────┐
│                     HTTP Request                       │
└──────────────────────────┬────────────────────────────┘
                           │
              ┌────────────▼──────────────┐
              │   JwtAuthenticationFilter  │  Intercepts every request
              │   Validates JWT signature  │  Sets SecurityContext
              │   Checks token blacklist   │  or rejects with 401
              └────────────┬──────────────┘
                           │
              ┌────────────▼──────────────┐
              │       SecurityConfig       │  URL-level access rules
              │  permitAll vs hasRole()    │  CORS, CSRF, session policy
              └────────────┬──────────────┘
                           │
         ┌─────────────────┼──────────────────┐
         │                 │                  │
┌────────▼───────┐ ┌───────▼──────┐ ┌────────▼───────┐
│ AuthController │ │UserController│ │AdminController  │
│ /api/auth/**   │ │/api/users/** │ │ /api/admin/**   │
└────────┬───────┘ └───────┬──────┘ └────────┬────────┘
         │                 │                  │
┌────────▼─────────────────▼──────────────────▼──────┐
│                   Service Layer                      │
│  AuthService · UserService · RefreshTokenService    │
│  TokenBlacklistService                              │
└──────────────────────────┬──────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────┐
│             Repository Layer (Spring Data JPA)       │
│  UserRepository · RoleRepository · TokenRepository  │
└──────────────────────────┬──────────────────────────┘
                           │
              ┌────────────▼──────────────┐
              │        PostgreSQL          │
              │  users · roles            │
              │  user_roles · tokens      │
              └───────────────────────────┘
```

---

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL 14+

### 1. Clone the repository

```bash
git clone https://github.com/your-username/jwt-auth-service.git
cd jwt-auth-service
```

### 2. Create the PostgreSQL database

```sql
CREATE DATABASE authdb;
```

### 3. Configure application.yml

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/authdb
    username: postgres
    password: your_password
```

### 4. Run the application

```bash
mvn spring-boot:run
```

On first startup the app automatically creates tables, seeds roles, and creates a default admin.

### 5. Access the app

| URL | Description |
|---|---|
| `http://localhost:8080` | Frontend UI |
| `http://localhost:8080/swagger-ui.html` | Swagger API docs |

**Default admin credentials:**
```
Username : admin
Password : admin123
```

> ⚠️ Change the default admin password before going to production!

---

## 📡 API Reference

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | No | Register a new user |
| POST | `/api/auth/login` | No | Login, get access + refresh tokens |
| POST | `/api/auth/refresh` | No | Exchange refresh token for new access token |
| POST | `/api/auth/logout` | Bearer | Revoke tokens, end session |

### Users

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/users/me` | Bearer (any) | Get own profile |
| GET | `/api/users/all` | Bearer (ADMIN) | List all users |
| DELETE | `/api/users/{id}` | Bearer (ADMIN) | Delete a user |

### Admin

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/admin/dashboard` | Bearer (ADMIN) | Admin dashboard |
| POST | `/api/admin/assign-role` | Bearer (ADMIN) | Assign role to user |
| POST | `/api/admin/revoke-role` | Bearer (ADMIN) | Remove role from user |

### Example

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{ "username": "admin", "password": "admin123" }'
```
```json
{
  "accessToken":  "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType":    "Bearer",
  "id": 1,
  "username": "admin",
  "roles": ["ROLE_ADMIN", "ROLE_USER"]
}
```

---

## 📁 Project Structure

```
src/main/java/com/authservice/
├── config/
│   ├── AppConfig.java               # BCrypt, AuthProvider beans
│   ├── SecurityConfig.java          # Filter chain, CORS, URL rules
│   ├── OpenApiConfig.java           # Swagger UI + Bearer auth scheme
│   └── DataSeeder.java              # Auto-seeds roles + default admin
├── controller/
│   ├── AuthController.java          # /api/auth/** endpoints
│   ├── UserController.java          # /api/users/** endpoints
│   └── AdminController.java         # /api/admin/** endpoints
├── dto/
│   ├── RegisterRequest / LoginRequest / RefreshTokenRequest
│   ├── AuthResponse.java            # Login/refresh response with tokens
│   └── MessageResponse.java         # Generic message wrapper
├── entity/
│   ├── User.java / Role.java / RefreshToken.java
├── exception/
│   ├── GlobalExceptionHandler.java  # Centralized @RestControllerAdvice
│   ├── TokenExpiredException / TokenRefreshException
│   └── UserAlreadyExistsException
├── filter/
│   └── JwtAuthenticationFilter.java # Runs before every request
├── repository/
│   ├── UserRepository / RoleRepository / RefreshTokenRepository
├── security/
│   ├── JwtService.java              # Token generation + validation
│   ├── UserDetailsImpl.java         # UserDetails adapter
│   └── UserDetailsServiceImpl.java  # Loads user from DB
├── service/
│   ├── AuthService.java             # register/login/refresh/logout
│   ├── RefreshTokenService.java     # Token lifecycle (create/verify/delete)
│   ├── TokenBlacklistService.java   # In-memory access token blacklist
│   └── UserService.java             # Profile + management
└── util/
    └── JwtUtil.java                 # Header/prefix constants

src/main/resources/
├── application.yml                  # Development configuration
├── application-prod.yml             # Production (reads env vars)
└── static/index.html                # Built-in frontend UI
```

---

## 🔑 Environment Variables

| Variable | Required | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Yes | Set to `prod` |
| `DATABASE_URL` | Yes | `jdbc:postgresql://host:5432/dbname` |
| `DATABASE_USERNAME` | Yes | DB username |
| `DATABASE_PASSWORD` | Yes | DB password |
| `JWT_SECRET` | Yes | Base64 secret, 64+ characters |
| `PORT` | No | HTTP port (default 8080) |

Generate JWT secret:
```bash
openssl rand -base64 64
# or visit: https://generate-secret.vercel.app/64
```

---

## 🗄 Database Schema

```sql
CREATE TABLE roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(20) UNIQUE NOT NULL   -- ROLE_USER / ROLE_MODERATOR / ROLE_ADMIN
);

CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(20) UNIQUE NOT NULL,
    email      VARCHAR(255) UNIQUE NOT NULL,
    password   VARCHAR(120) NOT NULL,          -- BCrypt hash
    enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP
);

CREATE TABLE user_roles (
    user_id BIGINT REFERENCES users(id),
    role_id BIGINT REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES users(id),
    token       VARCHAR(255) UNIQUE NOT NULL,  -- UUID v4
    expiry_date TIMESTAMP NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE
);
```

All tables are created automatically by Hibernate (`ddl-auto: update`).

---

## 🔒 Security

### Token Flow

```
Login  → BCrypt validates password → issue accessToken (JWT, 15m) + refreshToken (UUID, 7d)
Request → JwtAuthenticationFilter → check blacklist → verify signature → check expiry → proceed
Refresh → validate refreshToken in DB → issue new accessToken
Logout  → delete refreshToken from DB + blacklist accessToken → session ended
```

### Security Measures

| Threat | Mitigation |
|---|---|
| Password theft | BCrypt with strength 10 |
| Token forgery | HMAC-SHA256 with 64-char secret |
| Stolen access token | 15-minute expiry + blacklist on logout |
| Stolen refresh token | DB-stored, deleted on logout |
| Brute force | Vague "Invalid credentials" response |
| Privilege escalation | Double protection: URL rules + @PreAuthorize |
| SQL injection | JPA parameterized queries |

### Production Security Checklist

- [ ] Change default admin password
- [ ] Generate strong JWT_SECRET (64+ chars)
- [ ] Set `show-sql: false`
- [ ] Disable Swagger UI (`springdoc.swagger-ui.enabled: false`)
- [ ] Restrict CORS to your frontend domain only
- [ ] Use HTTPS
- [ ] Change `ddl-auto: validate` once schema is stable

---

## 🐳 Deployment

### Docker

```bash
docker build -t jwt-auth-service .

docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://host:5432/authdb \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=your_password \
  -e JWT_SECRET=your_64_char_secret \
  jwt-auth-service
```

### Railway

1. Push to GitHub
2. New Project → Deploy from GitHub repo
3. Add PostgreSQL service
4. Set the 5 environment variables
5. Auto-deploys on every push

### Manual JAR

```bash
mvn clean package -DskipTests
java -jar -Dspring.profiles.active=prod target/*.jar
```

---

## 🧪 Running Tests

```bash
mvn test                              # Run all tests
mvn test -Dtest=AuthServiceTest       # Single test class
mvn test jacoco:report                # With coverage report
```

---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.

---

<div align="center">

⭐ **Star this repo if it helped you!** ⭐

Built with Spring Boot · Spring Security · JWT · PostgreSQL

</div>
