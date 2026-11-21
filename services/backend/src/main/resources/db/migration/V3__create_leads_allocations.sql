-- V3: leads and lead_allocations (moving flow)

CREATE TABLE leads (
                       id                     BIGSERIAL     PRIMARY KEY,
                       customer_id            BIGINT        NOT NULL REFERENCES users (id),
                       city_id                BIGINT        NOT NULL REFERENCES cities (id),
                       lead_type              VARCHAR(30)   NOT NULL DEFAULT 'MOVING',
                       status                 VARCHAR(20)   NOT NULL DEFAULT 'NEW',
                       pickup_address         VARCHAR(500)  NOT NULL,
                       drop_address           VARCHAR(500)  NOT NULL,
                       move_date              DATE          NOT NULL,
                       volume_cft             INT           NOT NULL,
                       weight_kg              INT           NOT NULL,
                       required_vehicle_type  VARCHAR(20)   REFERENCES vehicle_types (vehicle_type),
                       idempotency_key        VARCHAR(100)  NOT NULL,
                       created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
                       CONSTRAINT chk_leads_type   CHECK (lead_type IN ('MOVING')),
                       CONSTRAINT chk_leads_status CHECK (status IN ('NEW', 'OFFERED', 'ACCEPTED', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
                       CONSTRAINT chk_leads_volume CHECK (volume_cft > 0),
                       CONSTRAINT chk_leads_weight CHECK (weight_kg > 0),
    -- A client key only has to be unique per customer, so one user cannot block or probe another user's keys.
                       CONSTRAINT uq_leads_customer_idem UNIQUE (customer_id, idempotency_key)
);

CREATE INDEX idx_leads_city_status ON leads (city_id, status);
CREATE INDEX idx_leads_customer    ON leads (customer_id);

CREATE TABLE lead_allocations (
                                  id             BIGSERIAL     PRIMARY KEY,
                                  lead_id        BIGINT        NOT NULL REFERENCES leads (id),
                                  vendor_id      BIGINT        NOT NULL REFERENCES vendors (id),
                                  score          NUMERIC(10,2) NOT NULL,
                                  rank_position  INT           NOT NULL,
                                  status         VARCHAR(20)   NOT NULL DEFAULT 'OFFERED',
                                  offered_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                  expires_at     TIMESTAMPTZ   NOT NULL,
                                  responded_at   TIMESTAMPTZ,
                                  CONSTRAINT chk_alloc_status CHECK (status IN ('OFFERED', 'ACCEPTED', 'REJECTED', 'EXPIRED')),
                                  CONSTRAINT chk_alloc_rank   CHECK (rank_position >= 1),
                                  CONSTRAINT chk_alloc_expiry CHECK (expires_at > offered_at),
    -- A vendor is offered a lead only once, and ranks are unique within a lead.
                                  CONSTRAINT uq_alloc_lead_vendor UNIQUE (lead_id, vendor_id),
                                  CONSTRAINT uq_alloc_lead_rank   UNIQUE (lead_id, rank_position)
);

-- "First accept wins": at most one ACCEPTED allocation per lead, enforced by the database.
CREATE UNIQUE INDEX uq_alloc_one_accepted_per_lead ON lead_allocations (lead_id) WHERE status = 'ACCEPTED';

CREATE INDEX idx_alloc_vendor_status ON lead_allocations (vendor_id, status);

-- Speeds up the job that expires unanswered offers.
CREATE INDEX idx_alloc_expiry ON lead_allocations (expires_at) WHERE status = 'OFFERED';