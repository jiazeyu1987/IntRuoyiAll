-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_lifecycle_directories; type=menu; riskLevel=low
-- Rename existing navigation only; retain menu identity, permissions, roles and tenant packages.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS rename_batch_execution_market_release;
DELIMITER $$
CREATE PROCEDURE rename_batch_execution_market_release()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900033 AND parent_id=991516
      AND type=2 AND deleted=b'0' AND path='/mes/pro/feedback/edhr-batch-execution'
      AND component='mes/pro/edhr-batch/BatchExecutionListPage'
      AND name IN ('批次执行','上市放行')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed batch execution menu';
  END IF;
  UPDATE system_menu SET name='上市放行',updater='rename-market-release',update_time=NOW()
    WHERE id=900033 AND name<>'上市放行';
  COMMIT;
END$$
DELIMITER ;
CALL rename_batch_execution_market_release();
DROP PROCEDURE rename_batch_execution_market_release;
