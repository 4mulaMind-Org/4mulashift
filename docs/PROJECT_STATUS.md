# Project Status: 4mulaShift

**Last updated:** 2026-11-15
**Phase:** Phase 1 (MVP), Sprint 1 in progress
**Current step:** Sprint 1, Day 5 (V2 migration to verify)

## One-line summary

Relocation platform. MVP: moving leads in one city, fair allocation to top 3 vendors, row-locked booking, idempotent payment.

## Stack (in use)

Java 21 target (runtime JDK on the machine is 27, to be switched), Spring Boot 4.1.2, Maven, PostgreSQL 16, Redis 7, Flyway, Docker Compose, Lombok, Actuator. Planned: JUnit 5 and Testcontainers, Postman, Spring Security (later).

## Repo

https://github.com/4mulaMind-Org/4mulashift
Local path: `~/Desktop/workSpace/project/4mulashift`
Backend: `services/backend` (package `com.mulashift`)

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
| Sprint 1 Day 3: connect app to Postgres and Redis | Done, health UP (apply TS-010 config fix) |
| Sprint 1 Day 4: Flyway V1 (cities, users) | Done, applied |
| Sprint 1 Day 5: Flyway V2 (vehicle_types, vendors, vehicles) | File written, not yet verified |
| Flyway V3 (leads, lead_allocations) | Next |
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

## Open items

- TS-010: update `spring.config.import` in `application.properties` to list both `.env` paths, then rerun and confirm V2 applies.
- TS-011: switch the runtime JDK to 21.
- TS-005: update the partial unique index in `er-diagram.md` (cover `PENDING_PAYMENT` and `CONFIRMED`) and use it in the bookings migration.
- Confirm placeholder `max_kg` values in `vehicle_types`.
- PRD section 11 open questions (fairness formula, accept window, fewer than 3 vendors, offer expiry, refunds, commission) and the launch city.

## Known issues and tips

- Terminal `git push` can time out; push from IntelliJ (Cmd + Shift + K).
- Stop the app before restarting. Check `lsof -i :8080` for a leftover process.
- Never edit an applied migration; add a new version.
- `.env` is ignored by Git; `.env.example` is the template.

## Documentation index

- `docs/PRD.md`, `docs/user-stories.md`
- `docs/02-design/`: `er-diagram.md`, `hld.md`, `api-spec.md`
- `docs/03-adr/`: ADR 0001 to 0003
- `docs/interview/`: notes 00 to 07
- `docs/dev-log.md`, `docs/troubleshooting-log.md`, `docs/PROJECT_STATUS.md`

## Working agreement

Every step produces: the work, a dev-log entry, an interview note, and a troubleshooting-log entry for any problem. Daily time budget is 30 minutes because of GATE preparation: one small task per day.

## How to resume in a new chat

Paste this file and say which step you are on. The assistant has no memory between chats.