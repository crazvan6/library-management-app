# Library Management System

A full-stack library management application built for the **Web Applications (Microservices track)** course.
Members manage a book catalog, reservations, loans (checkouts/returns), and fines, with role-based access
for students, librarians, and administrators.

> **Monorepo** — the backend REST API and the frontend SPA live in one repository, with a `main` + `dev`
> branch strategy and feature branches merged via pull requests.

## Repository layout

```
library-management-app/
├── backend/     # Spring Boot 3.2 REST API (Java 21, JPA, Spring Security/JWT)
└── frontend/    # React + Vite single-page app  (added in the FE pull requests)
```

## Tech stack

| Layer     | Technologies |
|-----------|--------------|
| Backend   | Java 21, Spring Boot 3.2, Spring Data JPA, Spring Security (JWT), Bean Validation, springdoc/OpenAPI |
| Database  | PostgreSQL (dev profile) · H2 in-memory (test profile) |
| Frontend  | React, Vite, React Router, Axios |
| Tooling   | Maven, JUnit 5, Mockito, JaCoCo |

## Getting started (backend)

```bash
cd backend
# dev profile (PostgreSQL) is active by default; override DB via env vars:
#   SPRING_DATASOURCE_URL / SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD
./mvnw spring-boot:run
# run tests against the in-memory H2 (test profile):
./mvnw test
```

API docs are served at `http://localhost:8080/api/swagger-ui.html` once the app is running.

## Documentation (work in progress)

- [ ] Architecture overview
- [ ] ER diagram
- [ ] API documentation
- [ ] Screenshots
- [ ] Live deployment link

## Team

| Member | GitHub | Area |
|--------|--------|------|
| Răzvan Cutuliga | [@crazvan6](https://github.com/crazvan6) | Backend foundation · Frontend shell & auth |
| Bogdan Manolache | [@bogdiz](https://github.com/bogdiz) | Backend security & catalog · Frontend catalog |
| Ștefan Georgian | [@Georgian2003](https://github.com/Georgian2003) | Backend lending & tests · Frontend lending |

_Detailed per-feature contributions are reflected in the pull-request history._
