# Interview Note 05: API Design

**Date:** (fill in)
**Related files:** `docs/02-design/api-spec.md`

## 30-second explanation

"I designed the REST API before writing controllers. Every endpoint maps to a user story, is protected by role, and write endpoints that must not run twice take an Idempotency-Key header. A repeated request returns the stored response, the same key with a different body is rejected, and a request still in progress returns a conflict. Errors use one consistent format with a code and a correlation ID. While writing the API I found a gap in my own database design: pending bookings did not hold the vehicle slot, so I fixed the unique index to cover pending and confirmed bookings."

## What I did

- Listed all endpoints by module with method, path, role, purpose and the story it serves.
- Defined conventions: versioned base path, JWT, ISO-8601 time, pagination, error format.
- Specified idempotency behaviour for the four possible situations.
- Wrote request and response examples for the critical flows (create lead, view offers, accept, pay).
- Documented which endpoint changes which state (lead, allocation, booking, vendor).
- Reviewed the design against the ER diagram and HLD, and recorded two corrections.

## Why I did it this way (decisions)

| Decision | Reason |
|---|---|
| Design the API before code | It is the contract between UI, tests and backend; changes are cheapest here. |
| `/api/v1` versioning | Allows breaking changes later without breaking clients. |
| Idempotency-Key header on create lead, accept and payment | These are the endpoints where a retry could duplicate money or bookings. |
| Four defined idempotency outcomes | Clients need predictable behaviour, including the same key with a different body. |
| Accept creates the booking in `PENDING_PAYMENT` | Separates holding the slot from receiving money; payment confirmation comes from the gateway webhook. |
| Customer phone hidden in offers until confirmed | Delivers the anti-spam promise at the API level. |
| One error format with `code` and `correlationId` | Easier client handling and faster debugging in logs. |
| Webhook endpoint public but signature verified | The gateway cannot send a JWT, so authenticity comes from the signature. |

## Alternatives I considered

- Booking created only after payment succeeds. Rejected: two customers could pay for the same slot, and refunds become the normal failure path.
- Idempotency only in the payment gateway. Rejected: the platform itself can double-create leads and bookings.
- GraphQL. Rejected for the MVP: simple resource-style endpoints are enough and easier to test with Postman.
- Vendor chooses the vehicle on accept. Deferred: the system picks the first free matching vehicle, which keeps the vendor flow to one click.

## Trade-offs and limitations

- A pending booking holds a slot for a payment window (15 minutes is an assumption), so an abandoned payment blocks the vehicle for a short time.
- Letting the system pick the vehicle reduces vendor control.
- Offset pagination is simple but can be slow on very large tables.
- Hiding customer name and phone limits what vendors see before they accept; this may reduce acceptance rates.

## Problems I hit and how I solved them

| Problem | What I did |
|---|---|
| Found a hole in my own design: the unique index covered only `CONFIRMED`, but accept creates `PENDING_PAYMENT` first | Extended the partial unique index to pending and confirmed bookings and added a payment timeout that releases the slot. |
| Open question from HLD: who picks the vehicle | Decided the system picks inside the locked transaction, and documented it as changeable. |
| Webhook cannot use JWT | Used signature verification and ignored duplicate gateway payment IDs. |

## Questions an interviewer may ask

**Q: How does your API handle a client retrying a request?**
A: With an Idempotency-Key. The first request is processed and its response stored. A retry with the same key and same body gets the stored response. A different body with the same key gets a 422, and a retry while the first is still running gets a 409.

**Q: Why not make POST idempotent through the data model alone?**
A: I do both. The key is stored in Redis for a fast replay, and unique constraints in the database are the guarantee if Redis is unavailable.

**Q: Why return 409 for "already booked"?**
A: It is a conflict with the current state of the resource, not a client syntax error or a missing resource.

**Q: How do you protect the payment webhook?**
A: Verify the gateway signature on every call, and treat the gateway payment ID as unique so repeated callbacks do nothing.

**Q: What did you change after designing the API?**
A: I found that pending bookings did not block the slot in my schema, which could let two customers reach payment for the same vehicle. I extended the partial unique index and added a payment timeout.

**Q: How would you version the API?**
A: Path versioning (`/api/v1`). Non-breaking changes stay in v1; breaking changes go into v2 while v1 keeps running during migration.

## Self-check

1. What are the four idempotency outcomes?
2. Walk through accept to confirmed booking, naming each status change.
3. What was the gap in the original unique index and how did you fix it?
4. Why is the webhook public, and how is it still safe?

## Proof and links

- `docs/02-design/api-spec.md`
- Repo: https://github.com/4mulaMind-Org/4mulashift