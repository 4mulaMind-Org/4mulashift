-- V4: bookings and payments (moving flow)

-- Target of the composite foreign key below, so a booking's vehicle must belong to the booking's vendor.
ALTER TABLE vehicles ADD CONSTRAINT uq_vehicles_id_vendor UNIQUE (id, vendor_id);

CREATE TABLE bookings (
                          id               BIGSERIAL     PRIMARY KEY,
                          lead_id          BIGINT        NOT NULL REFERENCES leads (id),
                          vendor_id        BIGINT        NOT NULL REFERENCES vendors (id),
                          vehicle_id       BIGINT        NOT NULL,
                          customer_id      BIGINT        NOT NULL REFERENCES users (id),
                          slot_date        DATE          NOT NULL,
                          status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING_PAYMENT',
                          amount           NUMERIC(12,2) NOT NULL,
                          idempotency_key  VARCHAR(100)  NOT NULL,
                          version          BIGINT        NOT NULL DEFAULT 0,
                          created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
                          payment_due_by   TIMESTAMPTZ   NOT NULL,
                          confirmed_at     TIMESTAMPTZ,
                          cancelled_at     TIMESTAMPTZ,
                          CONSTRAINT fk_bookings_vehicle_vendor FOREIGN KEY (vehicle_id, vendor_id) REFERENCES vehicles (id, vendor_id),
                          CONSTRAINT chk_bookings_status    CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED')),
                          CONSTRAINT chk_bookings_amount    CHECK (amount > 0),
                          CONSTRAINT chk_bookings_due       CHECK (payment_due_by > created_at),
                          CONSTRAINT chk_bookings_confirmed CHECK (status <> 'CONFIRMED' OR confirmed_at IS NOT NULL),
                          CONSTRAINT chk_bookings_cancelled CHECK (status <> 'CANCELLED' OR cancelled_at IS NOT NULL),
    -- The key is sent by the vendor who accepts the offer, so it is unique per vendor.
                          CONSTRAINT uq_bookings_vendor_idem UNIQUE (vendor_id, idempotency_key)
);

-- Double-booking protection (corrects TS-005): an active booking is PENDING_PAYMENT or CONFIRMED.
-- A vehicle can have only one active booking per day. Cancelled bookings release the slot.
CREATE UNIQUE INDEX uq_bookings_active_vehicle_slot
    ON bookings (vehicle_id, slot_date)
    WHERE status IN ('PENDING_PAYMENT', 'CONFIRMED');

-- A lead has at most one active booking. A cancelled (for example unpaid) booking allows the lead to be booked again.
CREATE UNIQUE INDEX uq_bookings_active_lead
    ON bookings (lead_id)
    WHERE status IN ('PENDING_PAYMENT', 'CONFIRMED');

CREATE INDEX idx_bookings_vendor_status ON bookings (vendor_id, status);
CREATE INDEX idx_bookings_customer      ON bookings (customer_id);

-- Speeds up the job that cancels bookings which were not paid in time.
CREATE INDEX idx_bookings_payment_due ON bookings (payment_due_by) WHERE status = 'PENDING_PAYMENT';

CREATE TABLE payments (
                          id                  BIGSERIAL     PRIMARY KEY,
                          booking_id          BIGINT        NOT NULL REFERENCES bookings (id),
                          gateway_order_id    VARCHAR(100)  NOT NULL,
                          gateway_payment_id  VARCHAR(100),
                          amount              NUMERIC(12,2) NOT NULL,
                          currency            VARCHAR(3)    NOT NULL DEFAULT 'INR',
                          status              VARCHAR(20)   NOT NULL DEFAULT 'CREATED',
                          idempotency_key     VARCHAR(100)  NOT NULL,
                          created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
                          paid_at             TIMESTAMPTZ,
                          CONSTRAINT chk_payments_status CHECK (status IN ('CREATED', 'SUCCESS', 'FAILED')),
                          CONSTRAINT chk_payments_amount CHECK (amount > 0),
                          CONSTRAINT chk_payments_paid   CHECK (status <> 'SUCCESS' OR (paid_at IS NOT NULL AND gateway_payment_id IS NOT NULL)),
                          CONSTRAINT uq_payments_gateway_order   UNIQUE (gateway_order_id),
    -- A repeated gateway callback cannot create a second payment row.
                          CONSTRAINT uq_payments_gateway_payment UNIQUE (gateway_payment_id),
    -- The key is sent by the customer who pays, so it is unique per booking.
                          CONSTRAINT uq_payments_booking_idem    UNIQUE (booking_id, idempotency_key)
);

-- A booking can have at most one successful payment.
CREATE UNIQUE INDEX uq_payments_one_success_per_booking ON payments (booking_id) WHERE status = 'SUCCESS';