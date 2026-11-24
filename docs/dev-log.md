# Dev Log

A short record of what I did, what blocked me and what I learned. Oldest entry first; add each new day at the bottom. Details of each problem are in `docs/troubleshooting-log.md` (TS-xxx ids).

## Working agreement

Every step produces three things: the work itself, a dev-log entry, and an interview note. Any problem goes into `troubleshooting-log.md`.

Every entry starts with its date (`## YYYY-MM-DD: title`), the day the work actually happened. Several days in one entry are written as a range.

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

## 2026-11-15: Sprint 1, Docker, database connection, Flyway V1 and V2

**Done**
- Docker Compose with Postgres 16 and Redis 7, both with healthchecks. Secrets live in `.env` (ignored by Git); `.env.example` is the template.
- Spring Boot backend connected to Postgres and Redis. `/actuator/health` shows `db` and `redis` both `UP`.
- Flyway V1 (`cities`, `users`) applied.
- Flyway V2 (`vehicle_types`, `vendors`, `vehicles`) applied and verified: the startup log shows `Successfully validated 2 migrations`, `Current version of schema "public": 2` and `No migration necessary`.
- Fixed the `.env` lookup (`spring.config.import` now lists both `../../.env` and `./.env`). The app now starts from IntelliJ with the repo root as working directory.
- IntelliJ loads the backend as a Maven project (Run button works).

**Blocked**
- Compose said "no configuration file provided": file name had a trailing space (TS-007).
- Port 8080 already in use: the previous app run was still alive (TS-008).
- IntelliJ Run button grey: Maven project not loaded because `pom.xml` is in a subfolder (TS-009).
- `password authentication failed for user "${POSTGRES_USER}"`: `.env` was not found when the app ran from the repo root. Fixed (TS-010).

**Learned**
- Check file names with `ls -la` when a tool says a file is missing.
- `lsof -i :8080` shows what holds a port; stop old processes before restarting.
- `optional:` in `spring.config.import` hides a missing file, so a wrong path fails silently.
- Flyway validates applied migrations on every start and never re-runs them.
- The log confirms `READ_COMMITTED` as the default isolation level, which matches ADR-0001.

**Next**
- Write Flyway V3 (`leads`, `lead_allocations`).
- Switch the runtime JDK from 27 to 21 (TS-011).
- Update the partial unique index in `er-diagram.md` (TS-005) before writing the bookings migration.

---

## 2026-11-21: Resuming Sprint 1, Docker was not running

**Done**
- Reopened the project after a break. The `.env` fix from TS-010 held: the app read the user and password correctly.

**Blocked**
- `Connection to localhost:5432 refused`: the Postgres container was not running because Docker Desktop was closed (TS-013).

**Learned**
- "Connection refused" (service down) and "password authentication failed" (wrong credentials) are different problems. Read the error class first.
- Containers do not restart on their own without a `restart` policy.
- Named volumes keep the database data across container restarts.

**Next**
- Start Docker, confirm both containers are healthy, run the app so Flyway V3 (`leads`, `lead_allocations`) is applied, and verify with `\dt`.
- Write V4 (`bookings`, `payments`) with the corrected partial unique index (TS-005).
- Switch the runtime JDK to 21 (TS-011).

## 2026-11-22: Flyway V3 applied, V4 prepared

**Done**
- Flyway V3 (`leads`, `lead_allocations`) applied and verified: `flyway_schema_history` lists versions 1, 2 and 3 with `success = t`, and `\dt` shows 8 tables.
- `\d lead_allocations` confirms the planned rules: unique `(lead_id, vendor_id)`, unique `(lead_id, rank_position)`, a partial unique index allowing one `ACCEPTED` allocation per lead, a partial index on `expires_at` for `OFFERED` rows, and CHECK constraints on status, rank and expiry.
- Postgres and Redis containers are running again (TS-013 resolved).
- Flyway V4 (`bookings`, `payments`) written, including the corrected double-booking index from TS-005. SQL syntax checked with the PostgreSQL parser; not yet applied.

**Blocked**
- None today. The earlier blockers (TS-013, TS-014) are resolved.

**Learned**
- Check the exact file before applying a migration: the file on disk (3,579 bytes) differed in size from the delivered V3 (2,818 bytes). The resulting schema matched the design, but an applied migration cannot be edited, so compare size or checksum first.
- Read the error class first: "connection refused" meant the database was down, not that the credentials were wrong.

**Next**
- Apply and verify V4, then write V5 (`audit_logs`).
- Add a revision note to `er-diagram.md` (see the revision block below).
- Switch the runtime JDK to 21 (TS-011).
- Create the JPA entities for the verified tables.

## 2026-11-22 (update): Flyway V4 applied, V5 prepared

**Done**
- Flyway V4 (`bookings`, `payments`) applied and verified: `flyway_schema_history` lists versions 1 to 4, all `success = t`.
- `\d bookings` confirms the planned rules: partial unique index on `(vehicle_id, slot_date)` over `PENDING_PAYMENT` and `CONFIRMED` (TS-005 fix), partial unique index for one active booking per lead, composite foreign key from `(vehicle_id, vendor_id)` to `vehicles`, and CHECK constraints for status, amount, payment deadline and status timestamps.
- `\d payments` confirms unique `gateway_order_id` and `gateway_payment_id`, unique `(booking_id, idempotency_key)`, and a partial unique index allowing one `SUCCESS` payment per booking.
- Flyway V5 (`audit_logs`) written: append-only table with triggers that reject UPDATE, DELETE and TRUNCATE. Syntax checked with the PostgreSQL parser; not yet applied.

**Blocked**
- None.

**Learned**
- A composite foreign key can enforce a cross-table rule (a vehicle belongs to the booking's vendor) without application code, but the referenced pair must have its own unique constraint.
- Partial unique indexes express "at most one active row" rules, such as one active booking per vehicle and day.

**Next**
- Apply and verify V5, including the append-only test.
- Create the JPA entities for the verified tables.
- Switch the runtime JDK to 21 (TS-011).

## 2026-11-22 (update 2): Flyway V5 applied, database schema complete

**Done**
- Flyway V5 (`audit_logs`) applied and verified: versions 1 to 5, all `success = t`.
- `\d audit_logs` shows the actor CHECK constraint and the triggers that reject UPDATE, DELETE and TRUNCATE.
- Append-only test passed: an update failed with `audit_logs is append-only: UPDATE is not allowed`, and the test insert was rolled back (`count(*)` returned 0).
- The MVP database schema is complete: `cities`, `users`, `vehicle_types`, `vendors`, `vehicles`, `leads`, `lead_allocations`, `bookings`, `payments`, `audit_logs`, plus Flyway's history table.

**Blocked**
- None.

**Learned**
- `psql -c` runs a multi-statement string as one transaction, so a failing statement rolls back the earlier ones. This allows testing constraints without leaving data behind.
- An immutable table cannot be cleaned up afterwards, so tests against it must be rolled back.

**Next**
- Switch the runtime JDK to 21 (TS-011).
- Create the JPA entities and repositories, starting with `cities` and `users`.

## 2026-11-23: Runtime JDK switched to Java 21

**Done**
- Switched the runtime JDK from 27 to Java 21 (Microsoft OpenJDK 21.0.13): set the project SDK in IntelliJ's Project Structure and the JDK in the `BackendApplication` Run configuration.
- Verified from the startup log: `Starting BackendApplication using Java 21.0.13.0.2`.
- Re-verified the database on the new JDK: `Successfully validated 5 migrations`, `Current version of schema "public": 5`, `No migration necessary`. The application started in about 3 seconds.

**Blocked**
- None. TS-011 is resolved.

**Learned**
- The JDK is chosen in three separate places: the project SDK, the Run configuration, and the terminal default (`JAVA_HOME`). The first two changed what IntelliJ runs; the terminal default is separate.
- A target of `release 21` in the build does not decide which JDK actually runs the application. The runtime JDK must be checked in the startup log.

**Next**
- Start the first Java code: the `City` entity and repository, verified by a test against the real database.

## 2026-11-24: First module built, the city module

**Done**
- Built the `city` module in the layered structure from the HLD: controller, service (interface and implementation), repository, entity, DTOs (records), mapper and module exceptions.
- Added shared error handling in `common/exception`: an `ApiException` base class, the `ApiError` response record and a `GlobalExceptionHandler`, so every error uses the format from the API spec.
- Added three endpoints: `GET /api/v1/cities`, `GET /api/v1/cities/{id}`, `POST /api/v1/admin/cities`.
- Wrote a unit test for the service (Mockito, no database) and an integration test for the repository (real PostgreSQL, each test rolled back). The build succeeded.
- Verified through HTTP with `curl`: `201` with a `Location` header, `200` list, `409 CITY_ALREADY_EXISTS`, `400 VALIDATION_ERROR` (`name: must not be blank`) and `404 NOT_FOUND`.
- Starting the application with the new entity also made Hibernate validate `City` against the `cities` table (`ddl-auto=validate`).

**Blocked**
- None.

**Learned**
- A rolled-back insert still consumes an identity value, so the first real row got id 6 after the repository tests had used ids 1 to 5.
- The early "already exists" check is only a friendly message. The unique constraint in the database is the real guarantee, so the service also converts a unique-violation into the same duplicate error.
- A catch-all exception handler can turn Spring's own 404 and 405 responses into 500 errors, so it was left out for now.

**Next**
- Day 11: the vehicle type reference data and the rule-based vehicle matching service (volume and weight to MINI, TEMPO or TRUCK), with unit tests.
- Add controller tests with MockMvc once the Spring Boot 4 test package names are confirmed.