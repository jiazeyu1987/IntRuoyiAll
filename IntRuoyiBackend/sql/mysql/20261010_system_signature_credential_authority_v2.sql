-- DRAFT ONLY: task 20261006-approval-center-review-preflight. Never executed by this task.
-- Deployment requires approval, the formal tenant inventory, and current schema/trigger verification.
-- Based on ruoyi-vue-pro.sql, 20260908_system_signature_password_t1.sql and
-- 20260830_system_user_lifecycle_deactivation.sql. This migration intentionally fails on rerun.
ALTER TABLE system_users
    ADD COLUMN signature_credential_generation CHAR(36) NULL,
    ADD COLUMN signature_credential_version BIGINT NULL;

-- Only mutable identity metadata is initialized. No historical signature/audit payload is rewritten.
UPDATE system_users SET signature_credential_generation = UUID(), signature_credential_version = 1;
ALTER TABLE system_users
    MODIFY COLUMN signature_credential_generation CHAR(36) NOT NULL,
    MODIFY COLUMN signature_credential_version BIGINT NOT NULL;

CREATE TABLE system_approval_authority_revision (
    tenant_id BIGINT NOT NULL,
    revision BIGINT NOT NULL,
    updated_at DATETIME(0) NOT NULL,
    PRIMARY KEY (tenant_id),
    CONSTRAINT chk_approval_authority_tenant CHECK (tenant_id > 0),
    CONSTRAINT chk_approval_authority_revision CHECK (revision > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Enumerate the formal tenant table; no request-driven insertion and no implicit tenant zero.
INSERT INTO system_approval_authority_revision (tenant_id, revision, updated_at)
SELECT id, 1, UTC_TIMESTAMP(0) FROM system_tenant WHERE id > 0 AND deleted = b'0';

CREATE TABLE system_signature_authentication_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    identity_domain VARCHAR(32) NOT NULL,
    operator_id BIGINT NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    outcome VARCHAR(64) NOT NULL,
    occurred_at DATETIME(0) NOT NULL,
    credential_version BIGINT NOT NULL,
    credential_generation CHAR(36) NOT NULL,
    creator VARCHAR(64) NULL,
    create_time DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) NULL,
    update_time DATETIME(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    KEY idx_signature_authentication_request (tenant_id, request_id, id),
    CONSTRAINT chk_signature_authentication_domain CHECK (identity_domain IN ('SYSTEM_USER', 'MES_EMPLOYEE_PROFILE')),
    CONSTRAINT chk_signature_authentication_identity CHECK (tenant_id > 0 AND actor_id > 0 AND operator_id > 0 AND credential_version > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELIMITER $$
CREATE TRIGGER system_users_signature_identity_insert
BEFORE INSERT ON system_users FOR EACH ROW
BEGIN
    -- The database owns generation, even when an import supplies a previously used primary key.
    SET NEW.signature_credential_generation = UUID();
    SET NEW.signature_credential_version = 1;
END$$
CREATE TRIGGER system_users_signature_identity_update
BEFORE UPDATE ON system_users FOR EACH ROW
BEGIN
    IF NOT (NEW.signature_credential_generation <=> OLD.signature_credential_generation) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Signature identity generation is immutable';
    END IF;
    IF NOT (NEW.signature_credential_version <=> OLD.signature_credential_version) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Signature credential version is database owned';
    END IF;
    IF NOT (NEW.password <=> OLD.password)
       OR NOT (NEW.password_credential_status <=> OLD.password_credential_status)
       OR NOT (NEW.password_update_time <=> OLD.password_update_time)
       OR NOT (NEW.status <=> OLD.status)
       OR NOT (NEW.deleted <=> OLD.deleted)
       OR NOT (NEW.lifecycle_effective_time <=> OLD.lifecycle_effective_time)
       OR NOT (NEW.lifecycle_deactivated_time <=> OLD.lifecycle_deactivated_time)
       OR NOT (NEW.login_locked <=> OLD.login_locked)
       OR NOT (NEW.login_locked_time <=> OLD.login_locked_time) THEN
        IF OLD.signature_credential_version = 9223372036854775807 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Signature credential version exhausted';
        END IF;
        SET NEW.signature_credential_version = OLD.signature_credential_version + 1;
    ELSE
        SET NEW.signature_credential_version = OLD.signature_credential_version;
    END IF;
END$$
CREATE TRIGGER system_signature_authentication_event_no_update
BEFORE UPDATE ON system_signature_authentication_event FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Signature authentication event is immutable';
END$$
CREATE TRIGGER system_signature_authentication_event_no_delete
BEFORE DELETE ON system_signature_authentication_event FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Signature authentication event is immutable';
END$$
DELIMITER ;
