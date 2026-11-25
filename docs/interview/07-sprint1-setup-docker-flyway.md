# Interview Note 07: Sprint 1 Setup (Spring Boot, Docker, Config, Flyway)

**Date:** 2026-11-11 to 2026-11-22
**Related files:** `services/backend/`, `docker-compose.yml`, `.env.example`, `application.properties`, `db/migration/V1` to `V5`, `docs/troubleshooting-log.md`

## 30-second explanation

"After the design phase I set up the backend foundation. I created a Spring Boot 4.1 project on Java 21 and ran Postgres 16 and Redis 7 with Docker Compose, with healthchecks and pinned image versions. Secrets live in a `.env` file that is not committed, and the app reads them through Spring's config import. The schema is created only by Flyway migrations, and Hibernate is set to validate rather than create tables. I proved the setup with the actuator health endpoint, which reports the database and Redis as UP, and with Flyway applying all five schema migrations (V1 to V5). I also kept a troubleshooting log of every problem I hit."

## What I did

- Generated the project with Spring Initializr: Maven, Java 21 target, Spring Boot 4.1.2, package `com.mulashift`.
- Chose dependencies for the MVP: Web, JPA, PostgreSQL, Flyway, Validation, Lombok, Actuator, Redis. Left Spring Security out until the authentication step.
- Wrote `docker-compose.yml` for Postgres 16 and Redis 7 with healthchecks and named volumes.
- Kept credentials in `.env` (ignored by Git) and a `.env.example` template in the repo.
- Configured the datasource and Redis, set `spring.jpa.hibernate.ddl-auto=validate`, and exposed health through Actuator.
- Wrote Flyway V1 to V5 (`cities`, `users`, `vehicle_types`, `vendors`, `vehicles`, `leads`, `lead_allocations`, `bookings`, `payments`, `audit_logs`) with CHECK, UNIQUE, partial unique index and foreign key constraints.
- Documented each problem in `docs/troubleshooting-log.md`.

## Why I did it this way (decisions)

| Decision | Reason |
|---|---|
| Spring Boot 4.1.2 | The 3.x line has left free support, and 4.1 is the current stable line. I avoided RC and SNAPSHOT versions. |
| Java 21 as the target | It is an LTS release, so tools and libraries are most stable on it. |
| Image versions pinned (`postgres:16`, `redis:7`) | `latest` can change and break the setup unexpectedly. |
| Healthchecks in Compose | The app and tests can wait until the database is really ready, not just started. |
| Named volumes | Data survives container restarts. |
| `.env` plus `.env.example` | Secrets stay out of Git, while new developers still see which variables are needed. |
| `ddl-auto=validate` and Flyway | The schema is versioned in the repo and reproducible; Hibernate only checks that code and schema match. |
| CHECK and UNIQUE constraints in SQL | The database enforces rules even if the application has a bug. |
| Security dependency left out for now | It locks every endpoint, so I add it when I build authentication. |

## Alternatives I considered

- Gradle instead of Maven. Either works; Maven is what I know, so I kept the learning curve low.
- Letting Hibernate create tables (`ddl-auto=update`). Rejected: no version history and risky in production.
- Installing Postgres and Redis directly on the machine. Rejected: Docker Compose makes the setup reproducible with one command.
- Putting secrets in `application.properties`. Rejected: they would end up in Git.
- Starting with Spring Boot 3.5 because most tutorials use it. Rejected: it is out of free support.

## Trade-offs and limitations

- The runtime JDK on my machine is currently 27 while the code targets 21 (open item TS-011).
- Newer versions mean fewer ready-made tutorials, so debugging sometimes needs the official docs.
- A `.env` file is fine for local development; real deployments need a proper secret store.
- Flyway migrations are append-only: a mistake needs a new migration instead of editing an old one.
- Placeholder values in `vehicle_types` (`max_kg`) must be confirmed before launch.

## Problems I hit and how I solved them

Full details are in `docs/troubleshooting-log.md`. The best stories for an interview:

**1. The silent configuration failure (TS-010).**
The app failed with `password authentication failed for user "${POSTGRES_USER}"`. The user name was literal text, which meant the `.env` file was never loaded. I found that IntelliJ started the app in the repo root, while my relative path `../../.env` assumed the `services/backend` folder. The `optional:` prefix hid the missing file, so there was no error about it. I fixed it by listing both possible locations. Lesson: relative paths depend on the working directory, and `optional` turns a loud error into a silent one.

**2. The old process holding the port (TS-008).**
The new run failed with "Port 8080 was already in use", yet a health check seemed to work. The reason was that the previous run was still alive and answered the request. I matched the PID in the old startup log with `lsof -i :8080` and stopped it. Lesson: a passing check does not prove the new build is the one running.

**3. The file name with a trailing space (TS-007).**
Docker Compose said no configuration file was found. `ls -la` revealed the name ended with a space. Lesson: check exact names when a tool cannot find a file.

**4. The IDE versus the build (TS-009).**
The terminal build worked, but IntelliJ's Run button was disabled because the Maven project was in a subfolder and was never loaded. I separated "is the build broken" from "is the IDE configured" and fixed only the second.

**5. A gap found in my own design (TS-005).**
While writing the API I noticed that pending bookings did not hold a vehicle slot in my unique index. I fixed the index to cover pending and confirmed bookings and added a payment timeout.

## Questions an interviewer may ask

**Q: Why use Flyway and not let Hibernate create the schema?**
A: Flyway gives a versioned, repeatable history of schema changes that is reviewed in Git. Hibernate's auto-update has no history and can make unsafe changes. With `ddl-auto=validate`, Hibernate only checks that the entities match the schema.

**Q: What happens if you edit a migration that was already applied?**
A: Flyway stores a checksum of each applied migration, so the app refuses to start with a checksum mismatch. Changes go into a new versioned migration.

**Q: Why pin Docker image versions?**
A: So the environment is reproducible. `latest` can change under me and cause failures that are hard to trace.

**Q: What does a healthcheck add in Compose?**
A: It reports when the service is actually ready to accept connections, so dependent services and tests do not start too early.

**Q: How do you keep secrets out of Git?**
A: Secrets go in `.env`, which is in `.gitignore`. The repository only has `.env.example` with variable names and dummy values. In production I would use a secret manager.

**Q: Tell me about a bug you debugged.**
A: Use story 1 above: a silent config failure caused by working directory and `optional:`. Explain the clue (literal `${POSTGRES_USER}` in the error), the cause and the fix.

**Q: Why READ_COMMITTED?**
A: It is the PostgreSQL default and my startup log confirmed it. It is enough for the booking flow because a row lock serialises competing requests, as described in ADR-0001.

**Q: Why not add Spring Security right away?**
A: It secures every endpoint by default, which would block my health checks and early tests. I add it when I implement login and roles.

## Self-check

1. What are the two paths in `spring.config.import`, and why are both needed?
2. What does `ddl-auto=validate` do, and why use it with Flyway?
3. Why can a changed migration file stop the app from starting?
4. Describe one bug from the troubleshooting log: symptom, cause, fix, lesson.
5. What does the actuator health output prove?

## Proof and links

- Actuator health: `db` UP, `redis` UP (Redis 7.4.12), overall `UP`.
- Flyway log: `Successfully applied 1 migration to schema "public", now at version v1`.
- Startup log: `Isolation level: READ_COMMITTED`.
- Flyway on a later start: `Successfully validated 2 migrations`, `Current version of schema "public": 2`, `Schema "public" is up to date. No migration necessary.` (V1 and V2 applied, not re-run).
- App started from the repo root and connected, which confirms the `.env` fix (TS-010).
- Repo: https://github.com/4mulaMind-Org/4mulashift

---

## Daily entries (oldest first; add each new day at the bottom)

Every heading carries the date of the day the work happened: `### Day N (YYYY-MM-DD): title`. An entry is written once, after its proof exists, so it never needs editing.

Entry template:

```
### Day N (YYYY-MM-DD): title

**30-second explanation:** how I would describe this step to an interviewer.

**What I did:**

- ...

**Why (decision):**

- ...

**Problems encountered:** TS-xxx, or "none".

**Sample interview question:** Q: ... A: ...

**Self-check:** 1) ... 2) ...

**Proof:** log line, command output or test result.
```

### Day 5 (2026-11-15): Flyway V2 (vehicle_types, vendors, vehicles)

**30-second explanation:** "I wrote the second migration for vehicle types, vendors and vehicles. The volume matrix lives in a lookup table that vehicles reference by foreign key, and vendor data has CHECK constraints for status, KYC status, rating range and non-negative counters, so bad data is rejected by the database itself."

**What I did:**

- Created `vehicle_types` with three rows (MINI, TEMPO, TRUCK) from the PRD volume matrix. The `max_kg` values are placeholders.
- Created `vendors` with quota, rating, cancellation count and last order time, which the scoring formula needs.
- Created `vehicles` with a unique registration number and a foreign key to the vehicle type.

**Why (decision):**

- A lookup table instead of an enum: the matrix can change without a code change, and the foreign key blocks invalid types.
- `vendors.user_id` is unique, so one user has one vendor profile.
- Constraints in SQL as the last line of defence, in addition to application validation.

**Problems encountered:** TS-010, the `.env` file was not found when the app ran from the repo root (fixed by listing two paths in `spring.config.import`).

**Sample interview question:** Q: Why validate rating in the database when the code validates it too? A: Code can have bugs or be bypassed by scripts and other services. A CHECK constraint guarantees the rule for every writer.

**Self-check:** 1) Why a lookup table and not an enum for vehicle types? 2) What happened to V1 when V2 was added, and why?

**Proof:** Startup log: `Successfully validated 2 migrations`, `Current version of schema "public": 2`, `No migration necessary`.

### Day 6 (2026-11-21 to 2026-11-22): Flyway V3 (leads, lead_allocations)

**30-second explanation:** "I wrote the third migration for leads and lead allocations. Besides the tables, I put the business rules into the database: a vendor can be offered a lead only once, ranks are unique per lead, and a partial unique index allows at most one ACCEPTED allocation per lead, which enforces 'first accept wins' even if the application has a race condition."

**What I did:**

- Created `leads` with status, vehicle type and a per-customer idempotency key.
- Created `lead_allocations` with score, rank, status and expiry time.
- Added CHECK constraints, unique constraints and a partial unique index.

**Why (decision):**

- The idempotency key is unique per `(customer_id, idempotency_key)`, not globally. Keys are generated by clients, so a global unique rule would let one user's key collide with, block or probe another user's. This refines the ER diagram, which showed a global unique key.
- A partial unique index on `ACCEPTED` keeps "first accept wins" safe under concurrency.
- A partial index on `expires_at` for `OFFERED` rows makes the expiry job cheap.

**Problems encountered:** TS-013 and TS-014 (Postgres container not running; Compose run from the wrong directory). Both resolved.

**Sample interview question:** Q: How does the database stop two vendors from both being accepted for the same lead? A: A partial unique index on `lead_allocations (lead_id) WHERE status = 'ACCEPTED'`. The second update fails with a unique violation, which the service turns into "already booked".

**Self-check:** 1) Why is the idempotency key unique per customer? 2) What does the partial unique index on ACCEPTED protect?

**Proof:** `flyway_schema_history` shows versions 1, 2 and 3 with success `t`; `\dt` lists 8 tables; `\d lead_allocations` shows `uq_alloc_one_accepted_per_lead` (partial unique index on `lead_id` where status is `ACCEPTED`).

### Day 7 (2026-11-22): Flyway V4 (bookings, payments)

**30-second explanation:** "I wrote the fourth migration for bookings and payments, and moved the booking rules into the database. A partial unique index allows only one active booking, pending payment or confirmed, per vehicle and day, which closes a gap I found while writing the API. A composite foreign key guarantees that a booking's vehicle belongs to the booking's vendor, and a unique index allows at most one successful payment per booking."

**What I did:**

- Created `bookings` with status, slot date, amount, payment deadline and a version column.
- Created `payments` with gateway order and payment ids, status and idempotency key.
- Added CHECK constraints so, for example, a confirmed booking must have a confirmation time and a successful payment must have a payment id and time.

**Why (decision):**

- The double-booking index covers `PENDING_PAYMENT` as well as `CONFIRMED` (TS-005), so a booking that is waiting for payment still holds the slot.
- At most one active booking per lead, instead of a plain unique `lead_id`: an unpaid booking can be cancelled and the lead offered again.
- Idempotency keys are scoped to the caller: per vendor for accepting an offer, per booking for creating a payment.
- A unique `gateway_payment_id` means a repeated gateway callback cannot create a second payment.
- Money is stored as `numeric`, never as a floating-point type.

**Problems encountered:** none.

**Sample interview question:** Q: How does the schema stop two customers from paying for the same vehicle on the same day? A: A partial unique index on `(vehicle_id, slot_date)` for bookings in `PENDING_PAYMENT` or `CONFIRMED` makes a second active booking impossible. The service also takes a row lock, so the second request waits and then gets a clean "already booked" response; the index is the backstop if the lock logic ever has a bug.

**Self-check:** 1) Why does the index include `PENDING_PAYMENT`? 2) Why is `lead_id` not simply unique on `bookings`?

**Proof:** `flyway_schema_history` shows versions 1 to 4 with success `t`. `\d bookings` shows `uq_bookings_active_vehicle_slot` (partial unique index on `(vehicle_id, slot_date)` where status is `PENDING_PAYMENT` or `CONFIRMED`) and `fk_bookings_vehicle_vendor`. `\d payments` shows `uq_payments_one_success_per_booking`.

### Day 8 (2026-11-22): Flyway V5 (audit_logs)

**30-second explanation:** "I created the audit log as an append-only table. Every allocation, booking and payment decision can be written to it, and database triggers reject any update, delete or truncate, so history cannot be rewritten by application code. It has no foreign keys on purpose, so it is never blocked by changes to business rows, and it stores details as JSON so the shape can evolve."

**What I did:**

- Created `audit_logs` with entity type and id, action, actor, correlation id, JSON details and a timestamp.
- Added a CHECK so a user action must name the user and a system action must not.
- Added triggers that raise an error on UPDATE, DELETE and TRUNCATE.
- Added indexes for lookups by entity, by time and by actor.

**Why (decision):**

- Immutability is enforced in the database, because application code can have bugs and other writers can bypass it.
- JSONB for details, so different event types can record different fields without schema changes.
- A correlation id links an audit row to the request logs for debugging.
- Audit history stays in PostgreSQL for the MVP and can move to a document store or a partitioned table later.

**Problems encountered:** none.

**Sample interview question:** Q: How do you make an audit log tamper-resistant? A: Insert-only access in the application, triggers that reject UPDATE, DELETE and TRUNCATE, and in production a database role that has only INSERT and SELECT on the table. A table owner could still drop the triggers, so the role restriction matters for real protection.

**Self-check:** 1) Why does the audit table have no foreign keys? 2) What does the actor CHECK constraint guarantee? 3) Why is the proof test run as one transaction?

**Proof:** `flyway_schema_history` shows versions 1 to 5, all with success `t`. `\d audit_logs` lists the CHECK constraint `chk_audit_actor` and the triggers `trg_audit_logs_no_update_delete` and `trg_audit_logs_no_truncate`. The test (an insert followed by an update in one transaction) failed with `ERROR: audit_logs is append-only: UPDATE is not allowed`, and `select count(*) from audit_logs` returned 0, so the test insert was rolled back.

### Day 9 (2026-11-23): Runtime JDK aligned to Java 21

**30-second explanation:** "My build targeted Java 21, an LTS release, but my machine's default JDK was 27, so the application was actually running on a different Java version than the one I target. I found it in the startup log, installed 21 was already available, and pointed both the IDE project and the run configuration at it. I then verified the new startup line and re-checked that all five database migrations still validate."

**What I did:**

- Read the Java version from the startup log instead of assuming it from the build file.
- Set the project SDK and the Run configuration JDK to Java 21.
- Restarted the application and confirmed the version and the schema state.

**Why (decision):**

- Run on the LTS version that I target and plan to deploy, so behaviour in development matches production.
- Avoid warnings and library incompatibilities that appear on non-LTS, very new JDKs.

**Problems encountered:** TS-011, resolved.

**Sample interview question:** Q: The code compiles for Java 21, so why does the runtime Java version matter? A: Compiling for a target version only fixes the bytecode level. The JVM that runs it can be newer, and differences in the JVM, libraries and agents can change behaviour or produce warnings. I want development and production to use the same LTS JVM, and I verify it in the startup log.

**Self-check:** 1) Where is the JDK chosen in an IntelliJ plus Maven setup? 2) How do you check which Java actually runs your application?

**Proof:** Startup log: `Starting BackendApplication using Java 21.0.13.0.2`; `Successfully validated 5 migrations`; `Current version of schema "public": 5`; `Started BackendApplication in 2.986 seconds`.

### Day 10 (2026-11-24): First module, the city module (controller, service, repository)

**30-second explanation:** "I built my first module in the layered structure I designed. A controller handles HTTP only, a service interface with an implementation holds the business rules and transactions, a repository talks to the database, and entities never leave the service because the API uses DTOs. A shared exception handler turns every business error into one consistent response format. I tested the service with a fast unit test, the repository against the real database, and the whole chain with real HTTP calls."

**What I did:**

- Created the module with controller, service, repository, entity, dto, mapper and exception packages.
- Added a shared `GlobalExceptionHandler` that returns the common error format with a code.
- Wrote a Mockito unit test for the service and a rolled-back integration test for the repository.
- Verified the endpoints with `curl`: create, list, duplicate, validation error and not found.

**Why (decision):**

- Service as an interface plus an implementation, so other modules depend on the contract and the implementation can change or be mocked.
- Entities never returned from controllers. DTOs as records keep the API stable when the table changes.
- The duplicate check happens twice: an early check gives a friendly error, and the database unique constraint is the real guarantee, because two requests can pass the early check together.
- A global exception handler keeps controllers free of error handling and guarantees one error format.
- The admin endpoint is open for now and gets the ADMIN role when authentication is built.

**Problems encountered:** none.

**Sample interview question:** Q: Why check for duplicates in code if the database already has a unique constraint? A: The check gives a clear, friendly error in the normal case. But it is a check-then-act, so two concurrent requests can both pass it. The unique constraint is what actually guarantees correctness, and the service converts the resulting database error into the same business error. The same idea applies to the booking flow.

**Self-check:** 1) Why does the controller return DTOs and never entities? 2) What does the service layer own that the controller must not? 3) Why is an early existence check not enough on its own?

**Proof:** The build succeeded. `curl` results: `POST /api/v1/admin/cities` returned `201` with `Location: /api/v1/cities/6`; `GET /api/v1/cities` returned `200` with the city; a repeated create returned `409` with code `CITY_ALREADY_EXISTS`; an empty name returned `400` with code `VALIDATION_ERROR` and detail `name: must not be blank`; `GET /api/v1/cities/9999` returned `404` with code `NOT_FOUND`.

### Day 11 (2026-11-24 to 2026-11-25): Vehicle module and rule-based matching

**30-second explanation:** "I built the vehicle module, which owns the vehicle types and the rule that picks the smallest vehicle that can carry a load. The service sorts the types from smallest to largest in memory and returns the first one where both volume and weight fit; otherwise it raises a business error that the shared handler returns as HTTP 422. The rule itself lives in the entity, the service orchestrates, and the controller only handles HTTP. I proved it with unit tests on the boundaries, tests against the real database and real HTTP calls."

**What I did:**

- Created the module with entity, repository, DTOs, mapper, two services, controller and an exception.
- Implemented the matching rule and the `422 LOAD_EXCEEDS_CAPACITY` response.
- Wrote 11 unit tests (boundaries, weight-driven upgrade, over-capacity, empty configuration, invalid input), a database test and two integration tests.
- Verified the endpoints with `curl`, and wrote a read-only script that checks the project structure.

**Why (decision):**

- The rule checks both volume and weight, because a load can fit by volume and still be too heavy.
- Types are sorted in memory, so the result never depends on database row order.
- The rule lives in the entity (`canCarry`), which keeps the business rule next to its data and easy to unit test.
- `422` rather than `400`: the request is valid, but the business rule cannot be satisfied. A `400` is for invalid input.
- The lead module will call the matching service through its interface only, never through this module's repository.
- Matching is a `POST` with a body, since it is a calculation on input rather than a lookup of a stored resource.

**Problems encountered:** TS-015 to TS-019. The best interview stories: code extracted into a nested folder, and how I verified and rebuilt the project from known-good archives; `git clean` deleting an untracked file; a test colliding with real data; and a verification script that was wrong, not the project.

**Sample interview question:** Q: Why does the matching rule check both volume and weight? A: A load can fit by volume and still exceed the weight limit. For example, 40 cubic feet fits a MINI (limit 50), but at 600 kg it is above the MINI limit of 500 kg, so the correct answer is a TEMPO. Checking only volume would overload the smaller vehicle.

**Sample interview question:** Q: Tell me about a time something went wrong in your project and how you fixed it. A: After a bad extraction, my source tree had a duplicate nested folder and moved files. I did not fix files one by one. I made a backup, rebuilt from the known-good archives, and verified with a file count and a script that checks every file, folder and package name. The script then showed a false failure on macOS because of a `sed` difference. I confirmed the project was fine, fixed the script, and recorded each incident with its lesson.

**Self-check:** 1) Why are the vehicle types sorted in memory rather than relying on database order? 2) Why is the over-capacity response 422 and not 400? 3) What caused TS-016, and what two habits prevent it?

**Proof:** `./mvnw clean test`: `Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`, Java 21.0.13. `curl`: `GET /api/v1/vehicle-types` returned MINI, TEMPO, TRUCK in order; `POST /api/v1/vehicle-matches` with 180 cft and 900 kg returned TEMPO; with 40 cft and 600 kg returned TEMPO; with 900 cft and 1000 kg returned `422` with code `LOAD_EXCEEDS_CAPACITY`; with 0 cft returned `400` with code `VALIDATION_ERROR` and the detail `volumeCft: must be greater than 0`. `verify-structure.sh` ended with `RESULT: all checks passed.`