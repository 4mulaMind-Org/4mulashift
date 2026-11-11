# ADR-0003: Idempotency with Redis for speed and database constraints for guarantee

- **Status:** Accepted
- **Date:** 2026-11-11
- **Related:** `docs/02-design/api-spec.md`, user stories US-04 and US-05

## Context

Networks fail and users retry. A retried request to create a lead, accept an offer or create a payment must not create a second record or a second charge. Payment gateways offer some protection for their own payments, but the platform's own booking creation can still double-fire.

## Decision

1. Write endpoints that must not run twice (create lead, accept offer, create payment) require an `Idempotency-Key` header.
2. A servlet filter handles the key:
    - New key: store `PROCESSING` in Redis using an atomic `SETNX` with a 24-hour TTL, then run the request.
    - On completion: store the response (status and body) and a hash of the request body against the key.
    - Same key, same body: return the stored response with `Idempotent-Replayed: true`.
    - Same key, different body: `422 IDEMPOTENCY_KEY_REUSED`.
    - Same key while still `PROCESSING`: `409 REQUEST_IN_PROGRESS`.
3. The database is the final guarantee: unique constraints on `idempotency_key` in `leads`, `bookings` and `payments`, and a unique `gateway_payment_id` on `payments` for duplicate webhooks.
4. If Redis is unavailable, the request still proceeds and the unique constraints prevent duplicates (a duplicate-key error is converted into the stored result).

## Alternatives considered

| Option | Why not chosen |
|---|---|
| Rely on the payment gateway only | Does not protect booking and lead creation. |
| Database-only idempotency | Correct but slower for the common replay path and puts retry load on the database. |
| Redis-only idempotency | Fast, but if Redis is flushed or fails, duplicates become possible. |
| Client-side prevention (disable the button) | Cannot stop network retries or double submits from other clients. |

## Consequences

**Good**
- Fast replay in the common case, with a hard guarantee in the database.
- Clear, predictable behaviour for clients in all four situations.

**Costs and risks**
- Extra moving part (Redis) and a filter to maintain.
- A crash after the business commit but before the response is stored in Redis leaves the key in `PROCESSING` until its TTL; the database unique constraint still prevents a duplicate and the retry resolves to the existing record.
- Stored responses use memory; the 24-hour TTL limits this.
- Clients must generate unique keys (a UUID per user action) and reuse the same key only for retries.

## Verification

- Test: same request twice with the same key creates exactly one record.
- Test: same key with a different body returns 422.
- Test: two simultaneous identical requests create exactly one record.
- Test: with Redis stopped, a duplicate request still creates only one record.