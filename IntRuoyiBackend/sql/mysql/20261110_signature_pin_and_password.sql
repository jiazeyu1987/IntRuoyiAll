-- release-migration: allowedEnvironments=test; dependsOn=20261011_edhr_approval_runtime_v2; type=schema; riskLevel=medium
-- Execute once before deploying the application. Fail on existing columns or missing prerequisite triggers.
-- All existing and future identities start with PIN signing (initial PIN 888888).
-- Existing login/signature passwords and historical signature/audit evidence are preserved.
SET NAMES utf8mb4;
DELIMITER $$
CREATE PROCEDURE preflight_signature_pin_20261110()
BEGIN
 IF (SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE()
     AND ((TRIGGER_NAME='system_users_signature_identity_update' AND EVENT_OBJECT_TABLE='system_users')
       OR (TRIGGER_NAME='mes_employee_signature_generation_update' AND EVENT_OBJECT_TABLE='mes_pro_process_pool_team_employee_profile'))
     AND ACTION_TIMING='BEFORE' AND EVENT_MANIPULATION='UPDATE')<>2 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Signature V2 credential triggers are required';
 END IF;
 IF EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
     AND TABLE_NAME IN ('system_users','mes_pro_process_pool_team_employee_profile')
     AND COLUMN_NAME IN ('signature_pin_hash','signature_credential_type')) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PIN migration already or partially applied';
 END IF;
END$$
DELIMITER ;
CALL preflight_signature_pin_20261110();
DROP PROCEDURE preflight_signature_pin_20261110;
ALTER TABLE system_users
 ADD COLUMN signature_pin_hash VARCHAR(100) NOT NULL DEFAULT '$2b$12$UE14YTiZJRbU4rUww99ppOLbGLYRyizUEkb53EYhuMvvr/sum8ErG',
 ADD COLUMN signature_credential_type VARCHAR(8) NOT NULL DEFAULT 'PASSWORD',
 ADD CONSTRAINT chk_sys_signature_type CHECK (signature_credential_type IN ('PIN','PASSWORD'));
ALTER TABLE mes_pro_process_pool_team_employee_profile
 ADD COLUMN signature_pin_hash VARCHAR(100) NOT NULL DEFAULT '$2b$12$UE14YTiZJRbU4rUww99ppOLbGLYRyizUEkb53EYhuMvvr/sum8ErG',
 ADD COLUMN signature_credential_type VARCHAR(8) NOT NULL DEFAULT 'PASSWORD',
 ADD CONSTRAINT chk_mes_signature_type CHECK (signature_credential_type IN ('PIN','PASSWORD'));
-- Installing the revised triggers makes PIN/method changes invalidate in-flight authenticated signers.
DROP TRIGGER system_users_signature_identity_update;
DROP TRIGGER mes_employee_signature_generation_update;
DELIMITER $$
CREATE TRIGGER system_users_signature_identity_update
BEFORE UPDATE ON system_users FOR EACH ROW
BEGIN
    IF NOT (NEW.signature_credential_generation <=> OLD.signature_credential_generation) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Signature identity generation is immutable';
    END IF;
    IF NOT (NEW.signature_credential_version <=> OLD.signature_credential_version) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Signature credential version is database owned';
    END IF;
    IF NOT (NEW.signature_pin_hash <=> OLD.signature_pin_hash)
       OR NOT (NEW.signature_credential_type <=> OLD.signature_credential_type)
       OR NOT (NEW.password <=> OLD.password)
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
CREATE TRIGGER mes_employee_signature_generation_update BEFORE UPDATE ON mes_pro_process_pool_team_employee_profile
FOR EACH ROW BEGIN
 SET NEW.signature_credential_generation=OLD.signature_credential_generation;
 IF NOT(NEW.signature_pin_hash <=> OLD.signature_pin_hash)
 OR NOT(NEW.signature_credential_type <=> OLD.signature_credential_type)
 OR NOT(NEW.signature_password_hash <=> OLD.signature_password_hash)
 OR NOT(NEW.signature_credential_status <=> OLD.signature_credential_status)
 OR NOT(NEW.enabled <=> OLD.enabled) OR NOT(NEW.disabled_at <=> OLD.disabled_at)
 OR NOT(NEW.system_user_id <=> OLD.system_user_id)
 OR NOT(NEW.signature_locked_until <=> OLD.signature_locked_until) THEN
  IF OLD.signature_credential_version IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='signature credential backfill required'; END IF;
  SET NEW.signature_credential_version=OLD.signature_credential_version+1;
 ELSE SET NEW.signature_credential_version=OLD.signature_credential_version;
 END IF;
END$$
DELIMITER ;
-- Switch existing identities through the new trigger to invalidate authenticated requests issued before migration.
START TRANSACTION;
UPDATE system_users SET signature_credential_type='PIN', update_time=update_time;
UPDATE mes_pro_process_pool_team_employee_profile SET signature_credential_type='PIN', update_time=update_time;
COMMIT;
ALTER TABLE system_users ALTER COLUMN signature_credential_type SET DEFAULT 'PIN';
ALTER TABLE mes_pro_process_pool_team_employee_profile ALTER COLUMN signature_credential_type SET DEFAULT 'PIN';
