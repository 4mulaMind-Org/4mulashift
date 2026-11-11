# API Specification (MVP)

**Status:** Draft v0.1 | **Related:** `docs/user-stories.md`, `docs/02-design/hld.md`, `docs/02-design/er-diagram.md`

This is the human-readable endpoint list. Later it becomes an OpenAPI file (`api-spec.yaml`) and a Postman collection.

## 1. Conventions

| Item | Rule |
|---|---|
| Base path | `/api/v1` |
| Format | JSON, UTF-8 |
| Auth | `Authorization: Bearer <JWT>` on everything except register, login and the payment webhook |
| Roles | `CUSTOMER`, `VENDOR`, `ADMIN` |
| Time | ISO-8601 in UTC |
| Money | Decimal amount plus `currency: "INR"` |
| IDs | Numeric in the MVP |
| Pagination | `?page=0&size=20`, response has `items`, `page`, `size`, `totalItems` |
| Idempotency | `Idempotency-Key: <uuid>` header on write endpoints marked **IDEMP** |

### Idempotency behaviour

| Situation | Response |
|---|---|
| New key | Processed normally |
| Same key, same body, already done | Stored response returned, header `Idempotent-Replayed: true` |
| Same key, different body | `422` with code `IDEMPOTENCY_KEY_REUSED` |
| Same key, first request still running | `409` with code `REQUEST_IN_PROGRESS` |

### Error format (all errors)

```json
{
  "timestamp": "2026-11-07T10:15:30Z",
  "status": 409,
  "code": "ALREADY_BOOKED",
  "message": "This vehicle is no longer available for the selected date.",
  "path": "/api/v1/offers/42/accept",
  "correlationId": "b3f1c2d4-...",
  "details": []
}
```

Common codes: `VALIDATION_ERROR` (400), `UNAUTHORIZED` (401), `FORBIDDEN` (403), `NOT_FOUND` (404), `ALREADY_BOOKED` (409), `OFFER_EXPIRED` (409), `REQUEST_IN_PROGRESS` (409), `IDEMPOTENCY_KEY_REUSED` (422), `RATE_LIMITED` (429).

## 2. Endpoints

### Auth and users (module `user`)

| Method | Path | Role | Purpose | Story |
|---|---|---|---|---|
| POST | `/auth/register` | public | Register customer or vendor | US-15 |
| POST | `/auth/login` | public | Returns JWT | US-15 |
| GET | `/users/me` | any | Current user | US-15 |

### Vendors and vehicles (module `vendor`)

| Method | Path | Role | Purpose | Story |
|---|---|---|---|---|
| POST | `/vendors/profile` | VENDOR | Submit business and KYC details, status becomes `PENDING` | US-10 |
| GET | `/vendors/me` | VENDOR | My profile, quota, status | US-11 |
| POST | `/vendors/me/vehicles` | VENDOR | Add a vehicle | US-10 |
| GET | `/vendors/me/vehicles` | VENDOR | List my vehicles | US-11 |
| GET | `/admin/vendors?status=PENDING` | ADMIN | Vendors waiting for approval | US-10 |
| POST | `/admin/vendors/{id}/approve` | ADMIN | Status to `ACTIVE` | US-10 |
| POST | `/admin/vendors/{id}/suspend` | ADMIN | Status to `SUSPENDED` | US-10 |

### Leads (module `lead`)

| Method | Path | Role | Purpose | Story |
|---|---|---|---|---|
| POST | `/leads` **IDEMP** | CUSTOMER | Create a moving request; triggers vehicle matching and allocation | US-01, 02, 03, 04 |
| GET | `/leads/{id}` | CUSTOMER (own) | Lead status and offers summary | US-06 |
| GET | `/leads` | CUSTOMER | My leads | US-06 |
| POST | `/leads/{id}/cancel` | CUSTOMER (own) | Cancel before confirmation | |

### Offers (module `allocation`)

| Method | Path | Role | Purpose | Story |
|---|---|---|---|---|
| GET | `/vendors/me/offers?status=OFFERED` | VENDOR | My pending offers | US-07, 11 |
| POST | `/offers/{id}/accept` **IDEMP** | VENDOR | Accept; creates the booking | US-07, 12 |
| POST | `/offers/{id}/reject` | VENDOR | Reject | US-07 |

### Bookings (module `booking`)

| Method | Path | Role | Purpose | Story |
|---|---|---|---|---|
| GET | `/bookings/{id}` | CUSTOMER or VENDOR (own) | Booking details | US-06 |
| GET | `/bookings` | CUSTOMER or VENDOR | My bookings | US-11 |
| POST | `/bookings/{id}/cancel` | CUSTOMER or VENDOR (own) | Cancel; vendor cancellation increases `cancellation_count` | |

### Payments (module `payment`)

| Method | Path | Role | Purpose | Story |
|---|---|---|---|---|
| POST | `/bookings/{id}/payments` **IDEMP** | CUSTOMER (own) | Create a payment order | US-05 |
| POST | `/payments/webhook` | public, signature verified | Gateway callback; duplicates ignored | US-05 |

### Admin and audit

| Method | Path | Role | Purpose | Story |
|---|---|---|---|---|
| GET | `/admin/leads?status=` | ADMIN | Leads needing attention | US-03, 08 |
| GET | `/admin/leads/{id}/allocations` | ADMIN | Offers with scores and ranks | US-09, 14 |
| GET | `/admin/metrics/fairness` | ADMIN | Orders per vendor, acceptance rate, bottom-50% share | US-16 |
| GET | `/admin/audit-logs?entityType=&entityId=` | ADMIN | Audit trail | US-14 |

### Real-time (WebSocket)

| Path | Purpose | Story |
|---|---|---|
| `/ws` (STOMP) | Subscribe to `/user/queue/updates` for lead and booking status changes | US-06 |

## 3. Key request and response examples

### Create lead

`POST /api/v1/leads` with `Idempotency-Key: 7d9c...`

```json
{
  "cityId": 1,
  "pickupAddress": "Sadar Bazar, Agra",
  "dropAddress": "Sector 62, Noida",
  "moveDate": "2026-12-05",
  "volumeCft": 180,
  "weightKg": 900
}
```

`201 Created`

```json
{
  "id": 101,
  "status": "OFFERED",
  "requiredVehicleType": "TEMPO",
  "offersSent": 3,
  "createdAt": "2026-11-07T10:15:30Z"
}
```

### Vendor views offers (customer phone hidden)

`GET /api/v1/vendors/me/offers?status=OFFERED`

```json
{
  "items": [
    {
      "offerId": 42,
      "leadId": 101,
      "pickupArea": "Agra",
      "dropArea": "Noida",
      "moveDate": "2026-12-05",
      "requiredVehicleType": "TEMPO",
      "expiresAt": "2026-11-07T10:25:30Z",
      "customerPhone": null
    }
  ],
  "page": 0, "size": 20, "totalItems": 1
}
```

### Accept an offer

`POST /api/v1/offers/42/accept` with `Idempotency-Key: 1a2b...` (empty body)

`201 Created`

```json
{
  "bookingId": 77,
  "status": "PENDING_PAYMENT",
  "vehicleId": 9,
  "slotDate": "2026-12-05",
  "paymentDueBy": "2026-11-07T10:40:30Z"
}
```

`409 Conflict` when another vendor or booking already holds the slot:

```json
{ "status": 409, "code": "ALREADY_BOOKED", "message": "This vehicle is no longer available for the selected date." }
```

### Create payment

`POST /api/v1/bookings/77/payments` with `Idempotency-Key: 5e6f...`

```json
{ "paymentId": 31, "gatewayOrderId": "order_test_123", "amount": 8500.00, "currency": "INR", "status": "CREATED" }
```

## 4. State changes caused by endpoints

| Action | Lead | Allocation | Booking | Vendor |
|---|---|---|---|---|
| `POST /leads` | `NEW` then `OFFERED` | 3 rows `OFFERED` | none | none |
| `accept` | `ACCEPTED` | chosen `ACCEPTED`, others closed | `PENDING_PAYMENT` | none |
| payment success webhook | `CONFIRMED` | unchanged | `CONFIRMED` | `orders_this_month` +1, `last_order_at` updated |
| payment window passes | `OFFERED` or admin review *(assumption)* | chosen `EXPIRED` | `CANCELLED`, slot released | none |
| vendor cancels booking | `CANCELLED` or reallocated *(assumption)* | none | `CANCELLED` | `cancellation_count` +1 |

## 5. Design corrections found while writing this API

Writing the API exposed two gaps in earlier documents. Fix them in those files.

**1. The double-booking index must also cover pending bookings.**
In `er-diagram.md` the partial unique index applies only to status `CONFIRMED`. But an accepted offer creates a `PENDING_PAYMENT` booking first. If pending bookings do not hold the slot, two customers could both reach the payment step for the same vehicle and day.

Fix: the partial unique index on `(vehicle_id, slot_date)` applies to status in (`PENDING_PAYMENT`, `CONFIRMED`). A pending booking that is not paid in time (15 minutes *(assumption)*) becomes `CANCELLED`, which releases the slot.

In `hld.md` section 6, read "confirmed booking exists" as "active booking (pending or confirmed) exists".

**2. Who picks the vehicle (HLD open question).**
Decision for the MVP: the system picks the first free vehicle of the required type owned by the accepting vendor, inside the same locked transaction. The vendor does not choose. If none is free, the accept fails with `ALREADY_BOOKED`. This can change later to vendor choice.

## 6. Open API questions

- Payment window length and what happens to the lead after an unpaid booking.
- Whether vendors can see the customer name before confirmation (currently hidden together with the phone number).
- Rate limits per endpoint.
- Versioning policy when `/api/v2` is needed.