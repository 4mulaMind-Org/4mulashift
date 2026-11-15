CREATE TABLE vehicle_types (
                               vehicle_type  VARCHAR(20) PRIMARY KEY,
                               max_cft       INT NOT NULL CHECK (max_cft > 0),
                               max_kg        INT NOT NULL CHECK (max_kg > 0)
);

-- Volume matrix from the PRD. max_kg values are placeholders, confirm before launch.
INSERT INTO vehicle_types (vehicle_type, max_cft, max_kg) VALUES
                                                              ('MINI',   50,   500),
                                                              ('TEMPO',  200,  1500),
                                                              ('TRUCK',  800,  7000);

CREATE TABLE vendors (
                         id                  BIGSERIAL     PRIMARY KEY,
                         user_id             BIGINT        NOT NULL UNIQUE REFERENCES users (id),
                         city_id             BIGINT        NOT NULL REFERENCES cities (id),
                         business_name       VARCHAR(200)  NOT NULL,
                         status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
                         rating              NUMERIC(2,1)  NOT NULL DEFAULT 0,
                         monthly_quota       INT           NOT NULL DEFAULT 0,
                         orders_this_month   INT           NOT NULL DEFAULT 0,
                         cancellation_count  INT           NOT NULL DEFAULT 0,
                         last_order_at       TIMESTAMPTZ,
                         gst_number          VARCHAR(20),
                         kyc_status          VARCHAR(20)   NOT NULL DEFAULT 'NOT_SUBMITTED',
                         CONSTRAINT chk_vendors_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED')),
                         CONSTRAINT chk_vendors_kyc    CHECK (kyc_status IN ('NOT_SUBMITTED', 'PENDING', 'VERIFIED', 'REJECTED')),
                         CONSTRAINT chk_vendors_rating CHECK (rating BETWEEN 0 AND 5),
                         CONSTRAINT chk_vendors_counts CHECK (monthly_quota >= 0 AND orders_this_month >= 0 AND cancellation_count >= 0)
);

CREATE INDEX idx_vendors_city_status ON vendors (city_id, status);

CREATE TABLE vehicles (
                          id               BIGSERIAL    PRIMARY KEY,
                          vendor_id        BIGINT       NOT NULL REFERENCES vendors (id),
                          vehicle_type     VARCHAR(20)  NOT NULL REFERENCES vehicle_types (vehicle_type),
                          registration_no  VARCHAR(20)  NOT NULL UNIQUE,
                          active           BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_vehicles_vendor ON vehicles (vendor_id);