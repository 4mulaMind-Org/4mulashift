# Interview Note 03: ER Diagram and Data Model

**Date:** 2026-11-07
**Related files:** `docs/02-design/er-diagram.md`

## 30-second explanation

"I designed the PostgreSQL schema before writing code. It has nine tables: users, vendors, vehicles, leads, lead allocations, bookings, payments, audit logs and a small vehicle-type config table. The important part is that safety is enforced by the database as well as the code: unique constraints on idempotency keys, a partial unique index that blocks two confirmed bookings for the same vehicle and day, and row-level locking in the booking transaction. So even if my application logic had a bug, the database would still refuse a double booking."

## What I did

- Drew the ER diagram in Mermaid so it lives in the repo and renders on GitHub.
- Defined tables, keys, statuses and relationships for the moving flow.
- Added constraints that enforce the business rules (unique allocation per lead and vendor, one booking per lead, one confirmed booking per vehicle per day).
- Planned indexes for the queries I know I will run (eligible vendors, allocations, bookings).
- Planned the Flyway migrations V1 to V5.

## Why I did it this way (decisions)

| Decision | Reason |
|---|---|
| PostgreSQL for all core data | Booking, payment and quota need ACID transactions and row locks. |
| Partial unique index on confirmed bookings | Defence in depth. Locking prevents the race; the index guarantees correctness even if the lock logic is wrong. |
| Idempotency keys unique in the database | Redis is fast but can be flushed or fail; the constraint is the final guarantee. |
| `lead_type` column | One lead table can later serve PG, flat and marketplace leads without redesign. |
| Audit log with no foreign keys | It is append-only history and must never be blocked by or tied to business rows. |
| `numeric` for money | Floating point causes rounding errors in payments. |
| Vendor counters stored on the vendor row | The scoring formula needs them fast; they update in the same transaction as the booking. |

## Alternatives I considered

- MongoDB for everything. Rejected: weak fit for transactions and row locking, which are the core proof of this project.
- Storing allocation as a column on the lead. Rejected: a lead has 3 offers, so a separate table with its own status and score is the correct model.
- Only application-level duplicate checks. Rejected: check-then-act fails under concurrency.
- Separate tables per lead type from day one. Rejected for the MVP: only moving exists, so extra tables would be speculative.

## Trade-offs and limitations

- One booking per vehicle per day is a simplification. Real moves need time windows, which will change the slot model.
- Vendor counters are denormalised for speed, so they must always be updated inside the booking transaction or they drift.
- Audit logs in PostgreSQL will grow; they may move to MongoDB or a partitioned table later.
- Hiding customer phone numbers is done in the API layer, not in the schema.

## Problems I hit and how I solved them

| Problem | What I did |
|---|---|
| Where should the "exactly one booking" guarantee live? | Used both: row lock in code and a partial unique index in the database. |
| How to avoid redesign when PG and flat leads arrive | Added `lead_type` and kept type-specific data out of the core `leads` table. |
| Redis idempotency is not a guarantee by itself | Added unique constraints on idempotency keys as the durable backstop. |

## Questions an interviewer may ask

**Q: How does your schema prevent double booking?**
A: In two ways. The booking service takes `SELECT ... FOR UPDATE` on the vehicle row inside a transaction, so concurrent requests wait. And a partial unique index on `(vehicle_id, slot_date)` for confirmed bookings makes a second confirmed booking impossible at the database level.

**Q: Why a partial unique index?**
A: Cancelled bookings for the same vehicle and day must be allowed. The uniqueness only applies to rows with status `CONFIRMED`.

**Q: What is the difference between pessimistic and optimistic locking, and where do you use each?**
A: Pessimistic locking blocks others until I finish (`FOR UPDATE`); I use it for the booking confirm because conflicts are likely and the cost of a failure is high. Optimistic locking uses a version column and fails on conflict; I keep a `version` on bookings as an extra guard for updates.

**Q: Why store idempotency keys in both Redis and the database?**
A: Redis gives a fast response for retries. The database unique constraint guarantees no duplicates even if Redis is down or flushed.

**Q: Why a separate `lead_allocations` table?**
A: One lead is offered to 3 vendors, and each offer has its own score, rank, status and expiry. That is a one-to-many relationship, so it needs its own table.

**Q: Why numeric and not float for money?**
A: Floats cannot represent many decimal values exactly, which causes rounding errors.

**Q: How would you extend this for PG rentals?**
A: Add `PG` to `lead_type`, create a `pg_lead_details` table, and add a PG scoring strategy. The allocation, booking and payment tables stay the same.

## Self-check

1. Draw the relationship between leads, lead_allocations and bookings from memory.
2. Explain the partial unique index and why it is needed in addition to `FOR UPDATE`.
3. Which constraints protect against duplicate payments?
4. What would you change to support time-window slots?

## Proof and links

- `docs/02-design/er-diagram.md`
- Repo: https://github.com/4mulaMind-Org/4mulashift