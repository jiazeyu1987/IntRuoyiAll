-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260813_dcc_explicit_data_scope_permissions; type=schema; riskLevel=low
-- Keep global assignment candidate latest-version checks bounded by tenant and master.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS add_dcc_project_assignment_candidate_index;
DELIMITER //
CREATE PROCEDURE add_dcc_project_assignment_candidate_index()
BEGIN
  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'dcc_controlled_file'
        AND INDEX_NAME = 'idx_dcc_controlled_file_assignment_latest'
  ) THEN
    CREATE INDEX `idx_dcc_controlled_file_assignment_latest`
      ON `dcc_controlled_file` (`tenant_id`, `master_id`, `deleted`, `id`);
  END IF;
END//
DELIMITER ;

CALL add_dcc_project_assignment_candidate_index();
DROP PROCEDURE IF EXISTS add_dcc_project_assignment_candidate_index;
