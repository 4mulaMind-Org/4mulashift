# Interview Note 06: Architecture Decision Records (ADRs)

**Date:** (fill in)
**Related files:** `docs/03-adr/0001-...`, `0002-...`, `0003-...`

## 30-second explanation

"I record important technical decisions as short ADRs: the context, the decision, the alternatives I rejected and the consequences. I wrote three for the MVP: a PostgreSQL row lock plus unique index to prevent double booking, a modular monolith instead of microservices, and idempotency using Redis for speed with database constraints as the guarantee. This way I can explain not only what I built but why, and what I would revisit."

## What I did

- Wrote three ADRs in a fixed format: status, context, decision, alternatives, consequences, verification.
- Included the cost and risk of each decision, not only the benefits.
- Tied each decision to a test that will prove it.

## The three decisions in one line each

| ADR | Decision | Key reason |
|---|---|---|
| 0001 | `SELECT ... FOR UPDATE` on the vehicle row plus a partial unique index | Correctness must not depend on application code alone. |
| 0002 | Modular monolith | One developer and one atomic booking transaction; boundaries allow later extraction. |
| 0003 | Redis idempotency keys plus database unique constraints | Redis is fast, the database is the guarantee. |

## Why ADRs at all

| Reason | Detail |
|---|---|
| Memory | In six months I will not remember why I chose something. |
| Review | A reviewer can challenge the reasoning, not just the code. |
| Onboarding | A new developer understands the history quickly. |
| Change | When a decision is revisited, the old one is marked superseded instead of deleted. |

## Alternatives I considered (summary)

- Locking: optimistic locking, Redis lock, `SERIALIZABLE`, unique index only.
- Architecture: microservices, plain layered monolith.
- Idempotency: gateway only, database only, Redis only.

## Trade-offs and limitations

- Row locks make requests for the same vehicle wait; acceptable at low contention, and a `lock_timeout` prevents hanging.
- A pending booking holds a slot until paid or timed out.
- A monolith scales as one unit and boundaries need architecture tests.
- Idempotency adds Redis and a filter; a crash at the wrong moment can leave a key in `PROCESSING` until the TTL, though the database still prevents duplicates.

## Questions an interviewer may ask

**Q: Why pessimistic locking and not optimistic?**
A: A double booking is costly and contention per vehicle is low, so waiting briefly is better than failing and retrying. I still keep a version column as an extra guard on updates.

**Q: Why is `READ COMMITTED` enough for the booking?**
A: The row lock serialises competing transactions, and after the second transaction gets the lock its next query sees the first one's committed data. So the check after the lock is accurate.

**Q: Why not lock in Redis?**
A: The lock should sit with the data and inside the same transaction. A Redis lock can expire or be lost while the database transaction is still running.

**Q: How do you avoid deadlocks?**
A: Each booking locks one row, the vehicle, and keeps the transaction short with no external calls. If more rows were ever locked, I would use a fixed lock order.

**Q: What if Redis goes down?**
A: Requests still work. The database unique constraints on idempotency keys and payment IDs stop duplicates; we only lose the fast replay path.

**Q: What would make you reverse the monolith decision?**
A: A module needing independent scaling or release cycles, or a separate team owning it.

**Q: What happens to a decision that turns out wrong?**
A: I write a new ADR that supersedes the old one and explain what changed.

## Self-check

1. Explain `FOR UPDATE` plus the partial unique index and why both are needed.
2. Why is `READ COMMITTED` enough here?
3. List the four idempotency situations and the response for each.
4. Give two reasons to stay a monolith now and one reason to split later.

## Proof and links

- `docs/03-adr/` (three ADRs)
- Repo: https://github.com/4mulaMind-Org/4mulashift