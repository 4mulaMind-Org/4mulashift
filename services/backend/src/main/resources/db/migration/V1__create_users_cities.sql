CREATE TABLE cities (
                        id      BIGSERIAL    PRIMARY KEY,
                        name    VARCHAR(100) NOT NULL,
                        state   VARCHAR(100) NOT NULL,
                        active  BOOLEAN      NOT NULL DEFAULT TRUE,
                        CONSTRAINT uq_cities_name_state UNIQUE (name, state)
);

CREATE TABLE users (
                       id             BIGSERIAL    PRIMARY KEY,
                       name           VARCHAR(150) NOT NULL,
                       email          VARCHAR(255) NOT NULL,
                       phone          VARCHAR(20)  NOT NULL,
                       password_hash  VARCHAR(255) NOT NULL,
                       role           VARCHAR(20)  NOT NULL,
                       created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       CONSTRAINT chk_users_role CHECK (role IN ('CUSTOMER', 'VENDOR', 'ADMIN'))
);

CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));