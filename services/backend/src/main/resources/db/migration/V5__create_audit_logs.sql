-- V5: audit log (append-only record of allocation, booking and payment decisions)

CREATE TABLE audit_logs (
                            id              BIGSERIAL    PRIMARY KEY,
                            entity_type     VARCHAR(50)  NOT NULL,
                            entity_id       BIGINT       NOT NULL,
                            action          VARCHAR(50)  NOT NULL,
                            actor_type      VARCHAR(10)  NOT NULL DEFAULT 'SYSTEM',
                            actor_id        BIGINT,
                            correlation_id  VARCHAR(64),
                            details         JSONB        NOT NULL DEFAULT '{}'::jsonb,
                            created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- A user action must name the user; a system action must not.
                            CONSTRAINT chk_audit_actor CHECK (
                                (actor_type = 'USER'   AND actor_id IS NOT NULL) OR
                                (actor_type = 'SYSTEM' AND actor_id IS NULL)
                                )
);

-- No foreign keys on purpose: the log must never be blocked by, or tied to, changes in business rows.

CREATE INDEX idx_audit_entity  ON audit_logs (entity_type, entity_id, created_at);
CREATE INDEX idx_audit_created ON audit_logs (created_at);
CREATE INDEX idx_audit_actor   ON audit_logs (actor_id) WHERE actor_id IS NOT NULL;

-- Append-only: rows can be inserted but never changed, deleted or truncated.
CREATE FUNCTION audit_logs_block_changes() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs is append-only: % is not allowed', TG_OP;
END;
$$;

CREATE TRIGGER trg_audit_logs_no_update_delete
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION audit_logs_block_changes();

CREATE TRIGGER trg_audit_logs_no_truncate
    BEFORE TRUNCATE ON audit_logs
    FOR EACH STATEMENT EXECUTE FUNCTION audit_logs_block_changes();