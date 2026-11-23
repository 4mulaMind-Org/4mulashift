# Project Status: 4mulaShift

**Last updated:** 2026-11-15
**Phase:** Phase 1 (MVP), Sprint 1 in progress
**Current step:** Sprint 1, Day 6 (Flyway V3: leads and lead_allocations)

## One-line summary

Relocation platform. MVP: moving leads in one city, fair allocation to top 3 vendors, row-locked booking, idempotent payment.

## Stack (in use)

Java 21 target (runtime JDK on the machine is 27, to be switched), Spring Boot 4.1.2, Maven, PostgreSQL 16, Redis 7, Flyway, Docker Compose, Lombok, Actuator. Planned: JUnit 5 and Testcontainers, Postman, Spring Security (later).

## Repo

https://github.com/4mulaMind-Org/4mulashift
Local path: `~/Desktop/workSpace/project/4mulashift`
Backend: `services/backend` (package `com.mulashift`)
Migrations: `services/backend/src/main/resources/db/migration/`

## Progress

| Step | Status |
|---|---|
| 0. Repo, folders, README, .gitignore | Done |
| 1. Scope fixed (MVP) | Done |
| 2. PRD v0.1 | Done (launch city still to decide) |
| 3. User stories (16) | Done |
| 4. ER diagram, HLD, API spec, ADR 0001 to 0003 | Done (apply TS-005 index fix to `er-diagram.md`) |
| Postman collection skeleton | Not started |
| Sprint 1 Day 1: Spring Boot project | Done |
| Sprint 1 Day 2: Docker Compose (Postgres, Redis) | Done, both healthy |
| Sprint 1 Day 3: connect app to Postgres and Redis | Done, health UP |
| Sprint 1 Day 4: Flyway V1 (cities, users) | Done |
| Sprint 1 Day 5: Flyway V2 (vehicle_types, vendors, vehicles) | Done, verified in startup log (schema version 2) |
| Sprint 1 Day 6: Flyway V3 (leads, lead_allocations) | File ready, not yet applied |
| Flyway V4 (bookings, payments), V5 (audit_logs) | Not started |
| Entities and repositories | Not started |
| Authentication (Spring Security, JWT) | Not started |
| Allocation engine, booking with row lock, concurrency test | Not started |
| Idempotency filter, payment | Not started |

## Decisions so far

- MVP is moving leads only, one city.
- Modular monolith with layers: controller, service, repository, entity, DTO.
- PostgreSQL for booking, payment and locking; Redis for idempotency keys.
- Strategy pattern for allocation scoring (`ScoringStrategy`).
- System picks the first free matching vehicle on accept (changeable).
- Pending bookings hold the slot for a payment window of 15 minutes (assumption).
- Spring Boot 4.1.x because 3.x is out of free support; pinned Docker image versions; schema only through Flyway.
- Idempotency key on `leads` is unique per customer `(customer_id, idempotency_key)`, not globally (refines the ER diagram; apply the same idea to `bookings` and `payments`).
- "First accept wins" is enforced by a partial unique index on ACCEPTED allocations per lead.

## Open items

- TS-011: switch the runtime JDK to 21.
- TS-005: update the partial unique index in `er-diagram.md` (cover `PENDING_PAYMENT` and `CONFIRMED`) and use it in the bookings migration (V4).
- Update `er-diagram.md` for the per-customer idempotency key and the new allocation indexes.
- Confirm placeholder `max_kg` values in `vehicle_types`.
- PRD section 11 open questions (fairness formula, accept window, fewer than 3 vendors, offer expiry, refunds, commission) and the launch city.
- GitHub Projects board not created yet.
- Design-phase dates in `dev-log.md` are approximate; check with `git log --date=short`.

## Known issues and tips

- Terminal `git push` can time out; push from IntelliJ (Cmd + Shift + K).
- Stop the app before restarting. Check `lsof -i :8080` for a leftover process.
- Never edit an applied migration; add a new version.
- `.env` is ignored by Git; `.env.example` is the template. `spring.config.import` lists both `../../.env` and `./.env`.
- Copying text out of the chat loses markdown symbols; download the file cards instead.

## Documentation index

- `docs/PRD.md`, `docs/user-stories.md`
- `docs/02-design/`: `er-diagram.md`, `hld.md`, `api-spec.md`
- `docs/03-adr/`: ADR 0001 to 0003
- `docs/interview/`: notes 00 to 07 (07 has daily entries)
- `docs/dev-log.md`, `docs/troubleshooting-log.md`, `docs/PROJECT_STATUS.md`

## Working agreement

Every step produces: the work, a dev-log entry, an interview note entry, and a troubleshooting-log entry for any problem. Daily time budget is 30 minutes because of GATE preparation: one small task per day.

## How to resume in a new chat

Paste this file and say which step you are on. The assistant has no memory between chats.

## Status updates

Baseline above is as of 2026-11-15. Each update below lists only what changed. Where an update differs from the baseline, the newest update wins.

### 2026-11-22

**Progress**
- Sprint 1 Day 6 (Flyway V3: `leads`, `lead_allocations`): done and verified (schema version 3).
- Sprint 1 Day 7 (Flyway V4: `bookings`, `payments`): file ready, not yet applied.
- ER diagram revision added to `er-diagram.md`.

**Decisions**
- At most one active booking per lead and one active booking per vehicle and day (partial unique indexes over `PENDING_PAYMENT` and `CONFIRMED`).
- A booking's vehicle must belong to the booking's vendor (composite foreign key).
- Idempotency keys are scoped to the caller (per customer on leads, per vendor on bookings, per booking on payments).

**Open items**
- Closed: TS-005 (booking index design), the `er-diagram.md` update.
- Opened and closed since the baseline: TS-013, TS-014 (see `troubleshooting-log.md`).
- Still open: TS-011 (switch the runtime JDK to 21), placeholder `max_kg` values, PRD section 11 questions and launch city, GitHub Projects board, design-phase dates in the dev log.

**Next**
- Apply and verify V4, write V5 (`audit_logs`), then the JPA entities.

> Baseline as of 2026-11-15. See "Status updates" at the bottom for later changes.

### 2026-11-23

**Progress**
- Runtime JDK switched to Java 21 and verified from the startup log. All five Flyway migrations validate on the new JDK.

**Decisions**
- No new decisions.

**Open items**
- Closed: TS-011.
- Still open: placeholder `max_kg` values, PRD section 11 questions and launch city, GitHub Projects board, design-phase dates in the dev log.

**Next**
- First Java code: the `City` entity and repository, verified by a test against the real database.