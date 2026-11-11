# ADR-0002: Build a modular monolith instead of microservices

- **Status:** Accepted
- **Date:** 2026-11-11
- **Related:** `docs/02-design/hld.md`

## Context

The MVP is built by one developer with limited daily time. The core workflow (accept, lock vehicle, create booking, update vendor counters) must be one atomic database transaction. The long-term vision includes more lead types and AI services, so the code must not become a tangle.

## Decision

Build one Spring Boot application, deployed as one unit, split into modules: `user`, `vendor`, `lead`, `allocation`, `booking`, `payment`, `audit`, `common`.

Rules:
- Each module owns its tables and its code package.
- A module calls another only through that module's service interface, never its repository or tables.
- `audit` is fed by application events.
- No circular dependencies, enforced with architecture tests (ArchUnit or Spring Modulith).

## Alternatives considered

| Option | Why not chosen |
|---|---|
| Microservices from day one | Adds network failures, service discovery, deployment and monitoring overhead; booking would span services and need distributed transactions or sagas. Too costly for one developer. |
| Plain layered monolith with no module boundaries | Faster at first but becomes tangled; hard to extract anything later. |
| Serverless functions | Poor fit for transactional workflows with row locks and long-lived WebSocket connections. |

## Consequences

**Good**
- One deployment, one database transaction for booking, simple debugging and testing.
- Clear boundaries mean a module (for example `allocation`) can be extracted into a service later with its tables.
- New lead types add a strategy and details table, not a new service.

**Costs and risks**
- The whole app scales as one unit.
- Boundaries are only conventions unless tested, so architecture tests are required.
- A bug in one module can crash the whole application.

## When to revisit

Extract a module into a service when it needs independent scaling (for example allocation under heavy load), independent release cycles, or when a separate team owns it.