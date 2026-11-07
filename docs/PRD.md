# 4mulaShift: Product Requirements Document (MVP)

| | |
|---|---|
| **Status** | Draft v0.1 |
| **Owner** | Abdul Qadir |
| **Last updated** | 2026-11-07 |
| **Scope** | Phase 1 (MVP): Moving leads, one city |

## 1. Overview

4mulaShift is a relocation platform that turns a moving request into a fair, safe and automated booking. In the MVP, a customer submits a moving request, the platform offers it to exactly 3 matching vendors using a fairness score, and the first vendor to accept gets a confirmed booking that cannot be double-booked or double-charged.

## 2. Problem

We focus on three problems for the MVP:

1. **Broker spam and vendor monopoly.** A customer's phone number is sold to many brokers, and demand concentrates with a few large vendors. Small vendors get no fair share.
2. **Double booking under concurrency.** Two bookings for the same vehicle or slot can both succeed when the system only checks availability in application code.
3. **Double payment on retry.** A network failure followed by a retry can create two bookings or two debits.

Deferred to later phases: wrong vehicle sizing (AI volume estimation), unwanted goods (marketplace), fragmented relocation lifecycle (PG, flat, services).

## 3. Users

| User | Goal |
|---|---|
| **Customer** | Get a reliable mover quickly, without spam calls, and pay once. |
| **Vendor** (packers and movers) | Receive fair, relevant leads and accept or reject them quickly. |
| **Admin** | Onboard and verify vendors, monitor allocations, bookings and fairness. |

## 4. Goals

- G1. Convert a moving request into exactly 3 vendor offers within seconds.
- G2. Guarantee a vehicle or slot can be booked by only one customer at a time.
- G3. Guarantee a retried request never creates a duplicate booking or payment.
- G4. Distribute leads fairly across vendors, and make that measurable.
- G5. Keep a complete audit trail of every allocation and booking decision.

## 5. Non-goals (MVP)

- PG, flat, marketplace, cleaning, painting, insurance and storage leads
- AI or ML features (image-based volume, dynamic pricing, forecasting)
- Multiple cities, multiple languages, international support
- Mobile apps (a responsive web UI is enough)
- Microservices (we build a modular monolith)
- Live GPS tracking, escrow, vendor financing

## 6. Success metrics

| Metric | Target |
|---|---|
| Concurrency safety | With 10 concurrent confirm requests for the same vehicle, exactly 1 succeeds (automated test) |
| Idempotency | Repeating the same request with the same key returns the same result, with no duplicate record (automated test) |
| Allocation latency | A request is allocated to 3 vendors in under 2 seconds (p95) |
| API latency | p95 under 300 ms for booking endpoints |
| Vendor acceptance rate | Tracked per vendor and overall (baseline in pilot) |
| Time to confirm | Median time from request to confirmed booking (baseline in pilot) |
| Fairness | Share of orders going to the bottom 50% of vendors by volume (baseline in pilot) |

## 7. Features

### Must have

| ID | Feature | Notes |
|---|---|---|
| F1 | Customer submits a moving request | Pickup, drop, date, item list or size, contact details |
| F2 | Vehicle matching by volume and weight | Rule-based matrix: mini, tempo, truck |
| F3 | Fair allocation to top 3 vendors | Score uses inactivity, remaining quota, rating and cancellations |
| F4 | Vendor accepts or rejects an offer | First accept wins, others are closed |
| F5 | Booking confirmation with row-level locking | PostgreSQL `SELECT ... FOR UPDATE` inside a transaction |
| F6 | Idempotent booking and payment | Idempotency key stored in Redis with a 24-hour TTL |
| F7 | Payment in test mode | Razorpay test mode, webhook signature verification |
| F8 | Audit log | Every allocation and booking decision recorded |
| F9 | Vendor onboarding and verification | Basic KYC fields, admin approval |
| F10 | Authentication and roles | Customer, vendor, admin |

### Should have

| ID | Feature |
|---|---|
| F11 | Vendor dashboard: offers, accepted bookings, quota |
| F12 | Admin dashboard: allocations, bookings, fairness metric |
| F13 | Real-time confirmation updates (WebSocket) |
| F14 | Customer phone number hidden from vendors until booking is confirmed |

### Later

PG and flat leads, marketplace, home services, AI volume estimation, dynamic pricing, multi-city, partner API, live tracking.

## 8. Core flow

1. Customer submits a request with an idempotency key.
2. The system checks the key. A duplicate returns the stored response.
3. A lead is created (status `NEW`), and the vehicle type is derived from volume and weight.
4. The allocation service scores eligible vendors in the city and creates 3 offers (status `OFFERED`).
5. Vendors accept or reject. The first accept moves the lead to `ACCEPTED`.
6. Booking confirmation locks the vehicle row. If it is still free, the booking becomes `CONFIRMED`. If not, the request is rejected gracefully.
7. Payment is processed with the same idempotency guarantee.
8. Customer and vendor are notified, and the audit log is written.

Lead status flow: `NEW` → `OFFERED` → `ACCEPTED` → `CONFIRMED` (or `CANCELLED` / `EXPIRED` at any stage).

## 9. Allocation scoring (initial version)

```
score = (days_since_last_order * 10)
      + (remaining_monthly_quota * 5)
      + (rating * 2)
      - (cancellation_count * 20)
```

The strategy sits behind a `ScoringStrategy` interface, so other lead types can add their own scoring later without changing the allocation service.

## 10. Non-functional requirements

- **Consistency:** all booking state changes run in ACID transactions.
- **Performance:** p95 under 300 ms for booking APIs.
- **Availability:** 99% during the pilot.
- **Security:** role-based access, encrypted PII, no card data stored, webhook signature checks, rate limiting.
- **Privacy:** follow the DPDP Act 2023 (consent, purpose limitation, deletion on request).
- **Observability:** structured logs with correlation IDs, health checks, error tracking.
- **Testing:** unit tests, Testcontainers integration tests for locking and idempotency, load test before pilot.

## 11. Assumptions and open questions

**Assumptions**
- Launch city: to be decided.
- 10 to 20 verified vendors are onboarded before the pilot.
- Payment uses Razorpay in test mode until the pilot.

**Open questions**
- Does the inactivity term (`days_since_last_order * 10`) let a low-rated vendor outrank a reliable one? Needs a cap or a minimum rating filter.
- First-to-accept still favours fast responders. Do we need an accept window (for example, 5 minutes) before choosing among accepters?
- What happens when fewer than 3 vendors are eligible?
- What is the offer expiry time, and what happens when all 3 vendors reject?
- Cancellation and refund rules for customers and vendors.
- Commission model and when it is charged.

## 12. Milestones

| Sprint | Outcome |
|---|---|
| 1 | Project setup, Docker Compose (Postgres, Redis), Flyway migrations, entities, auth |
| 2 | Vendor and lead modules, scoring strategy, allocation service |
| 3 | Booking, row locking, concurrency test |
| 4 | Idempotency, payment (test mode) |
| 5 | Dashboards, Postman collection, CI pipeline |
| 6 | Testing, bug fixing, deployment, pilot |