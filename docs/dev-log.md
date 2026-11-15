# Dev Log

A short record of what I did, what blocked me and what I learned. Newest entry first. Details of each problem are in `docs/troubleshooting-log.md` (TS-xxx ids).

---

## 2026-11-15: Sprint 1, Docker, database connection, Flyway

**Done**
- Docker Compose with Postgres 16 and Redis 7, both with healthchecks. Secrets live in `.env` (ignored by Git); `.env.example` is the template.
- Spring Boot backend connected to Postgres and Redis. `/actuator/health` shows `db` and `redis` both `UP`.
- Flyway V1 (`cities`, `users`) applied successfully (`now at version v1`).
- Flyway V2 file written for `vehicle_types`, `vendors` and `vehicles`. Not yet verified as applied.
- IntelliJ now loads the backend as a Maven project (Run button works).

**Blocked**
- Compose said "no configuration file provided": file name had a trailing space (TS-007).
- Port 8080 already in use: the previous app run was still alive (TS-008).
- IntelliJ Run button grey: Maven project not loaded because `pom.xml` is in a subfolder (TS-009).
- App failed with `password authentication failed for user "${POSTGRES_USER}"`: `.env` was not found when the app ran from the repo root (TS-010).

**Learned**
- Check file names with `ls -la` when a tool says a file is missing.
- `lsof -i :8080` shows what holds a port; stop old processes before restarting.
- `optional:` in `spring.config.import` hides a missing file, so a wrong path fails silently.
- Flyway remembers applied migrations and never re-runs them.
- Log confirms `READ_COMMITTED` as the default isolation level, which matches ADR-0001.

**Next**
- Apply the `.env` path fix, run the app, confirm V2 is applied (`\dt` shows 6 tables, `vehicle_types` has 3 rows).
- Switch the runtime JDK from 27 to 21 (TS-011).
- Write V3 (`leads`, `lead_allocations`).

---

## 2026-11-11: Spring Boot project created

**Done**
- Chose Spring Boot 4.1.2 (latest stable; 3.x is out of free support), Java 21 target, Maven, Jar.
- Group and package `com.mulashift`. Dependencies: Web, JPA, PostgreSQL, Flyway, Validation, Lombok, Actuator, Redis. Spring Security deliberately left out until the auth step.
- Placed the project at `services/backend`, build succeeded.

**Blocked**
- `com.4mulashift` is not a valid Java package name (TS-006).

**Learned**
- Pre-release versions (RC, SNAPSHOT) are avoided; library versions are left to Spring Boot's dependency management.

---

## 2026-11-07 to 2026-11-10 (approx.): Design phase

**Done**
- Repo created inside the organization, folders `docs/`, `services/`, `postman/`, `.gitignore`, README.
- PRD v0.1, user stories (16), ER diagram (9 tables), HLD, API spec, three ADRs.
- Interview notes 00 to 06.
- While writing the API spec I found a gap in my own ER design: the partial unique index must cover `PENDING_PAYMENT` as well as `CONFIRMED` bookings (TS-005).

**Blocked**
- `git push` from the terminal timed out (TS-001).
- Extra character in the cloned folder name (TS-002).
- Pasted README text into the terminal by mistake (TS-003).
- File staged at the wrong path, then moved (TS-004).

**Learned**
- Terminal Git and the IDE can use different network paths.
- Reviewing my own design against the next document (API after ER) catches real gaps.
- Keep scope small: one verifiable MVP.

---

## Working agreement

Every step produces three things: the work itself, a dev-log entry, and an interview note. Any problem goes into `troubleshooting-log.md`.