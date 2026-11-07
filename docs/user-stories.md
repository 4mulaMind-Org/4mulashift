# User Stories: 4mulaShift MVP

**Status:** Draft v0.1 | **Related:** `docs/PRD.md` (features F1 to F14)

Priority: **M** = Must, **S** = Should. Each story has acceptance criteria in Given / When / Then form. Items marked *(assumption)* are not yet confirmed and are listed in the PRD open questions.

---

## Epic A: Customer

### US-01 Submit a moving request (F1, M)
As a customer, I want to submit a moving request so that I can get a verified mover.

- **Given** I am logged in, **when** I submit pickup, drop, date and item details, **then** a lead is created with status `NEW`.
- **Given** a required field is missing or the date is in the past, **when** I submit, **then** I get a clear validation error and no lead is created.

### US-02 Get the right vehicle automatically (F2, M)
As a customer, I want the system to pick the vehicle size so that I do not pay for a bigger truck than I need.

- **Given** a request with volume and weight, **when** the lead is created, **then** the vehicle type (mini, tempo or truck) is chosen from the volume matrix.
- **Given** the load exceeds the largest vehicle, **when** the lead is created, **then** the customer is told the load needs more than one vehicle or manual review.

### US-03 Be allocated to exactly 3 vendors (F3, M)
As a customer, I want my request to go to a small set of good vendors so that I do not get spam calls.

- **Given** at least 3 eligible vendors in the city, **when** the lead is created, **then** exactly 3 distinct offers are created with status `OFFERED`.
- **Given** fewer than 3 eligible vendors, **when** the lead is created, **then** offers go to the available vendors and the lead is flagged for admin attention *(assumption)*.
- **Given** any outcome, **then** my phone number is not exposed to vendors before confirmation (see US-13).

### US-04 Retry safely without duplicates (F6, M)
As a customer, I want a retried request to be safe so that I am never booked or charged twice.

- **Given** I send a request with idempotency key K, **when** I send the same request with K again within 24 hours, **then** the stored response is returned and no new record is created.
- **Given** two identical requests with the same key arrive at the same time, **then** only one is processed.

### US-05 Pay once for a confirmed booking (F7, M)
As a customer, I want to pay securely so that my booking is confirmed once.

- **Given** a booking is ready for payment, **when** I pay, **then** the payment is recorded and the booking becomes `CONFIRMED`.
- **Given** the payment gateway callback arrives twice, **then** the second callback is ignored.
- **Given** a webhook with an invalid signature, **then** it is rejected and logged.

### US-06 See my booking status in real time (F13, S)
As a customer, I want live status updates so that I know when a vendor confirms.

- **Given** a vendor accepts, **when** the booking is confirmed, **then** my screen updates without a manual refresh.

---

## Epic B: Vendor

### US-07 Receive and respond to offers (F4, M)
As a vendor, I want to accept or reject offers so that I only take jobs I can do.

- **Given** I have an `OFFERED` allocation, **when** I accept, **then** my allocation becomes `ACCEPTED`.
- **Given** another vendor has already been confirmed, **when** I try to accept, **then** I see "already booked" and nothing changes.
- **Given** I reject, **then** the allocation becomes `REJECTED` and my cancellation count is not affected.

### US-08 Offers expire (F4, M)
As the platform, I want offers to expire so that requests do not hang.

- **Given** an offer is not answered within the offer window (10 minutes *(assumption)*), **then** it becomes `EXPIRED`.
- **Given** all 3 offers are rejected or expired, **then** the lead is flagged for admin reallocation *(assumption)*.

### US-09 Fair allocation (F3, M)
As a vendor, I want leads allocated fairly so that small vendors also get work.

- **Given** vendors with different inactivity, quota, rating and cancellation history, **when** the engine scores them, **then** the top 3 by score are selected using the configured strategy.
- **Given** a vendor has no quota left this month, **then** they are not eligible.
- **Given** the same inputs, **then** the same ranking is produced (deterministic).

### US-10 Onboarding and verification (F9, M)
As a vendor, I want to register and get verified so that I can receive leads.

- **Given** I submit my business and KYC details, **then** my account is `PENDING`.
- **Given** an admin approves me, **then** I become `ACTIVE` and eligible for allocation.
- **Given** I am not `ACTIVE`, **then** I never receive offers.

### US-11 Vendor dashboard (F11, S)
As a vendor, I want to see my offers, bookings and quota so that I can plan my work.

- **Given** I am logged in, **then** I can see pending offers, accepted bookings and remaining monthly quota.

---

## Epic C: Booking safety (platform)

### US-12 Prevent double booking (F5, M)
As the platform, I want a vehicle to be bookable by only one customer at a time so that no booking is ever oversold.

- **Given** 10 concurrent confirm requests for the same vehicle and slot, **then** exactly 1 succeeds and 9 are rejected gracefully.
- **Given** a failure during confirmation, **then** the transaction rolls back and the vehicle stays available.
- Proof: automated Testcontainers integration test.

### US-13 Protect customer privacy (F14, S)
As a customer, I want my phone number hidden until I am confirmed so that I am not contacted by unconfirmed vendors.

- **Given** an offer is `OFFERED` or `ACCEPTED` but not `CONFIRMED`, **then** the vendor sees no phone number.
- **Given** the booking is `CONFIRMED`, **then** the confirmed vendor can see it.

### US-14 Audit every decision (F8, M)
As an admin, I want every allocation and booking decision recorded so that disputes can be resolved.

- **Given** an allocation, offer response, confirmation or cancellation, **then** an audit record is written with who, what, when and the inputs used (for allocation, the scores).

---

## Epic D: Admin and access

### US-15 Role-based access (F10, M)
As the platform, I want roles so that users only see what they should.

- **Given** a customer token, **when** calling a vendor or admin endpoint, **then** the response is 403.
- **Given** no token, **when** calling a protected endpoint, **then** the response is 401.

### US-16 Fairness dashboard (F12, S)
As an admin, I want to see allocation and fairness metrics so that I can monitor the platform.

- **Given** I am an admin, **then** I can see orders per vendor, acceptance rate, time to confirm and the share of orders going to the bottom 50% of vendors by volume.

---

## Traceability

| Feature | Stories |
|---|---|
| F1 | US-01 |
| F2 | US-02 |
| F3 | US-03, US-09 |
| F4 | US-07, US-08 |
| F5 | US-12 |
| F6 | US-04 |
| F7 | US-05 |
| F8 | US-14 |
| F9 | US-10 |
| F10 | US-15 |
| F11 | US-11 |
| F12 | US-16 |
| F13 | US-06 |
| F14 | US-13 |

## Definition of Ready (before a story enters a sprint)

- Acceptance criteria are written and testable.
- Dependencies and open questions are listed.
- It fits in one sprint.