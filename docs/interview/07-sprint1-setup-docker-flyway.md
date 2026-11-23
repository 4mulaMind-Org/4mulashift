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