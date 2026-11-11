# ADR-0001: Use a PostgreSQL row lock (plus a unique index) to prevent double booking

- **Status:** Accepted
- **Date:** 2026-11-11
- **Related:** `docs/02-design/er-diagram.md`, `docs/02-design/hld.md`, user story US-12

## Context

Two customers (or two vendors accepting at the same moment) can try to book the same vehicle for the same day. A check-then-act in application code ("if free, then book") fails under concurrency, because both requests can read "free" before either writes. A double booking breaks customer trust and is the core problem this platform claims to solve.

## Decision

1. Inside the booking transaction, lock the vehicle row with `SELECT ... FOR UPDATE`.
2. After acquiring the lock, check whether an active booking (`PENDING_PAYMENT` or `CONFIRMED`) exists for that vehicle and date.
3. If none exists, insert the booking and update vendor counters, then commit.
4. Add a **partial unique index** on `bookings (vehicle_id, slot_date)` where status is `PENDING_PAYMENT` or `CONFIRMED`, as a second line of defence.
5. Set a `lock_timeout` (for example 3 seconds) so a stuck lock returns an error instead of hanging.
6. Keep the transaction short: no network calls (payment gateway, notifications) inside it.

Default isolation level `READ COMMITTED` is enough, because the row lock serialises competing requests and the check runs after the lock is acquired.

## Alternatives considered

| Option | Why not chosen |
|---|---|
| Application-level check only | Race condition under concurrency. |
| Optimistic locking (version column) only | Works, but under contention it fails and retries often; a double booking here is costly, so waiting is better than failing. Kept `version` as an extra guard on updates. |
| Redis distributed lock | The lock would live outside the data it protects and outside the transaction; if Redis fails or the lock expires early, safety is lost. |
| `SERIALIZABLE` isolation | Correct but causes serialization failures that need retry logic everywhere, and is heavier than a single targeted lock. |
| Unique index only | Prevents the duplicate but gives a database error instead of a graceful result; used together with the lock, not instead of it. |

## Consequences

**Good**
- Correctness does not depend on application logic alone: the lock serialises requests and the index blocks duplicates even if the lock logic had a bug.
- Easy to prove with a Testcontainers test: 10 concurrent threads, exactly 1 success.

**Costs and risks**
- Requests for the same vehicle queue up; this is acceptable because contention per vehicle is low.
- Lock timeouts must be handled and translated into a clear API response (`409` or `503` with retry advice).
- A pending booking holds the slot until paid or timed out (15 minutes is an assumption), so an abandoned payment blocks the vehicle briefly.
- Only one row (the vehicle) is locked per booking, so deadlocks are avoided; if more rows are ever locked, use a fixed lock order.

## Verification

- Integration test (Testcontainers): 10 concurrent confirms for one vehicle and date, expect exactly 1 success and 9 `ALREADY_BOOKED`.
- Test that cancelling a booking releases the slot for a new booking.