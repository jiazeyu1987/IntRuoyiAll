-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260812_mes_route_version_snapshot_identity; type=schema; riskLevel=medium
-- Prospective policy: preserve legacy snapshots without backfill or global NOT NULL.
-- Require identity for new versions, changed snapshot/identity, or publication.
-- Canonical SHA-256 equality and business identities remain checked by the Java
-- identity writer, publication service and exact-version snapshot resolver.

DROP TRIGGER IF EXISTS mes_route_snapshot_identity_insert;
DROP TRIGGER IF EXISTS mes_route_snapshot_identity_update;
DELIMITER $$
CREATE TRIGGER mes_route_snapshot_identity_insert
BEFORE INSERT ON mes_pro_route_version FOR EACH ROW
BEGIN
  IF NEW.route_snapshot_json IS NULL OR JSON_VALID(NEW.route_snapshot_json) <> 1
     OR NEW.route_snapshot_sha256 IS NULL
     OR NOT REGEXP_LIKE(NEW.route_snapshot_sha256, '^[0-9a-f]{64}$', 'c')
     OR NEW.route_snapshot_format_version IS NULL
     OR BINARY NEW.route_snapshot_format_version <> BINARY 'MES_ROUTE_SNAPSHOT_CANONICAL_V1' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'new route snapshot requires valid JSON and canonical identity';
  END IF;
END$$
CREATE TRIGGER mes_route_snapshot_identity_update
BEFORE UPDATE ON mes_pro_route_version FOR EACH ROW
BEGIN
  IF NOT (BINARY NEW.route_snapshot_json <=> BINARY OLD.route_snapshot_json)
     OR NOT (BINARY NEW.route_snapshot_sha256 <=> BINARY OLD.route_snapshot_sha256)
     OR NOT (BINARY NEW.route_snapshot_format_version <=> BINARY OLD.route_snapshot_format_version)
     OR (NEW.lifecycle_status = 'ACTIVE'
         AND (NOT (NEW.lifecycle_status <=> OLD.lifecycle_status)
              OR NOT (NEW.active <=> OLD.active))) THEN
    IF NEW.route_snapshot_json IS NULL OR JSON_VALID(NEW.route_snapshot_json) <> 1
       OR NEW.route_snapshot_sha256 IS NULL
       OR NOT REGEXP_LIKE(NEW.route_snapshot_sha256, '^[0-9a-f]{64}$', 'c')
       OR NEW.route_snapshot_format_version IS NULL
       OR BINARY NEW.route_snapshot_format_version <> BINARY 'MES_ROUTE_SNAPSHOT_CANONICAL_V1' THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'changed route snapshot requires valid JSON and canonical identity';
    END IF;
  END IF;
END$$
DELIMITER ;
