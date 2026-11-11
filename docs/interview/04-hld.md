# Interview Note 04: High-Level Design (HLD)

**Date:** (fill in)
**Related files:** `docs/02-design/hld.md`

## 30-second explanation

"I designed 4mulaShift as a modular monolith: one Spring Boot application split into modules for user, vendor, lead, allocation, booking, payment and audit. Each module owns its tables and talks to others only through service interfaces, so it can be extracted into a service later. The booking module is the critical path: it takes a row lock on the vehicle inside a transaction, so concurrent requests cannot double-book. An idempotency filter in front of the API, backed by Redis and database unique keys, stops duplicate requests and payments."

## What I did

- Defined the architecture style, modules, responsibilities and which tables each module owns.
- Set dependency rules between modules and drew them.
- Drew two sequence diagrams: request to allocation, and accept to confirmed booking.
- Listed cross-cutting concerns (idempotency, security, audit, observability, migrations).
- Chose the technology stack and a Docker Compose deployment for the MVP.
- Mapped every important requirement to the part of the design that handles it.

## Why I did it this way (decisions)

| Decision | Reason |
|---|---|
| Modular monolith | One developer, one transaction boundary for booking. Microservices would add network failures and distributed transactions without benefit. |
| Modules own their tables | Clear boundaries make later extraction possible. |
| Modules call service interfaces, not each other's repositories | Prevents hidden coupling and keeps rules in one place. |
| Audit through application events | Business modules stay independent of the audit module. |
| Idempotency as a filter | One place handles retries for every write endpoint. |
| Two-lane idea (rules first, AI later) | The safety path stays deterministic; AI can later sit behind `ScoringStrategy` with a rule-based fallback. |

## Alternatives I considered

- Microservices from day one. Rejected: too much operational cost for the MVP, and a booking that spans services needs distributed transactions.
- A single layered app with no module boundaries. Rejected: it becomes tangled and hard to split later.
- Locking in Redis instead of PostgreSQL. Rejected: the lock should live next to the data it protects, in the same transaction.
- Event-driven everything with Kafka. Rejected for the MVP: not needed at this volume; kept for later phases.

## Trade-offs and limitations

- A monolith scales as one unit. If allocation becomes heavy, I would extract that module first.
- Module boundaries are only conventions unless tested, so I plan architecture tests (ArchUnit or Spring Modulith).
- Offer expiry needs a scheduled job, which adds a small moving part.
- WebSocket notifications add state to the app server; fine for one instance, harder when scaled out.

## Problems I hit and how I solved them

| Problem | What I did |
|---|---|
| How to keep audit logging without coupling every module to it | Used application events so modules publish and the audit module listens. |
| Where booking confirmation should start | Made `booking` the only module that takes the vehicle row lock, so the rule lives in one place. |
| Open question: who picks the vehicle at confirmation | Left it as an explicit open design question instead of guessing. |

## Questions an interviewer may ask

**Q: Why a monolith and not microservices?**
A: For one developer and one transactional workflow, a modular monolith is faster to build and debug. The module boundaries and table ownership mean I can extract a service later without redesign.

**Q: How do you keep modules from becoming tightly coupled?**
A: Modules call each other only through service interfaces, never through another module's repository, and I enforce it with architecture tests.

**Q: Walk me through a booking.**
A: Customer submits with an idempotency key, the filter checks Redis, the lead is created, allocation scores vendors and creates 3 offers. When a vendor accepts, the booking module starts a transaction, locks the vehicle row, checks for an existing confirmed booking, inserts the booking and commits. Then payment is created with its own idempotency key and both sides are notified.

**Q: What happens if two vendors accept at the same time?**
A: The accept updates the lead only if it is still open, and the booking confirmation takes the row lock, so only one reaches a confirmed booking. The other gets an "already booked" response.

**Q: Where would you add AI later?**
A: Behind the `ScoringStrategy` interface as a separate Python service, with the current rule-based strategy as a fallback, so safety does not depend on the model.

**Q: What would make you split into microservices?**
A: A module needing independent scaling or independent deployment, for example allocation under heavy load, or a team boundary.

## Self-check

1. Name the modules and say which tables each owns.
2. Explain the dependency rules and why audit uses events.
3. Walk through the booking sequence without looking.
4. When would you extract a module into a service?

## Proof and links

- `docs/02-design/hld.md` (system context, module diagram, two sequence diagrams)
- Repo: https://github.com/4mulaMind-Org/4mulashift