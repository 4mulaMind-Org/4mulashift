# Project Status: 4mulaShift

**Last updated:** 2026-11-07
**Phase:** Phase 1 (MVP), pre-development
**Current step:** Step 3, user stories

## One-line summary

Relocation platform. MVP: moving leads in one city, fair allocation to top 3 vendors, row-locked booking, idempotent payment.

## Stack (planned)

Java 21, Spring Boot 3, PostgreSQL 16, Redis 7, Flyway, Docker, JUnit 5 and Testcontainers, Postman. Modular monolith. MongoDB and Python/ML come in Phase 2.

## Repo

https://github.com/4mulaMind-Org/4mulashift
Local path: `~/Desktop/workSpace/project/4mulashift`

## Progress

| Step | Status |
|---|---|
| 0. Repo, folders, README, .gitignore | Done |
| 1. Scope fixed (MVP) | Done |
| 2. PRD v0.1 | Done (needs review and launch city) |
| 3. User stories | Next |
| 4. ER diagram, HLD, API list, ADRs | Not started |
| 5. Sprint 1 (setup, Docker, Flyway, entities, auth) | Not started |
| GitHub Projects board | Not created yet |

## Decisions so far

- MVP = moving leads only, one city.
- Modular monolith with layers: controller, service, repository, entity, DTO.
- PostgreSQL for booking, payment and locking; Redis for idempotency keys.
- Strategy pattern for allocation scoring (`ScoringStrategy`).

## Open questions

See section 11 of `docs/PRD.md` (fairness formula weakness, accept window, fewer than 3 eligible vendors, offer expiry, refunds, commission).

## Known issues

- Terminal `git push` times out; push from IntelliJ (Cmd + Shift + K) works.

## Next actions

1. Review PRD and decide the launch city.
2. Write `docs/user-stories.md` (10 to 15 stories with acceptance criteria).
3. Create the GitHub Projects board.
4. Write the interview note for each finished step in `docs/interview/`.

## How to resume in a new chat

Paste this file and say which step you are on. The assistant has no memory between chats.