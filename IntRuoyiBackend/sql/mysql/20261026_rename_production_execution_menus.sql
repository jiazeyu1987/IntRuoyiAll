-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_lifecycle_directories; type=menu; riskLevel=low
-- Navigation labels only. Preserve paths, components, permissions, role grants
-- and tenant-package grants; business terminology within pages is unchanged.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS rename_production_execution_menus;
DELIMITER $$
CREATE PROCEDURE rename_production_execution_menus()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF (SELECT COUNT(*) FROM system_menu WHERE parent_id=991516 AND type=2 AND deleted=b'0' AND (
      (id=605071316 AND path='/mes/pro/production-release/pqc'
        AND component='mes/pro/production-release/PqcProductionReleasePage'
        AND name IN ('PQC生产放行','生产放行'))
      OR (id=900230 AND path='/mes/pro/feedback/edhr-work-task'
        AND component='mes/pro/edhr-work-task/WorkTaskBoardPage'
        AND name IN ('eDHR工作任务','工作任务'))
      OR (id=9008300 AND path='/mes/pro/feedback/edhr-nonconformance-review'
        AND component='mes/pro/edhr-nonconformance/NonconformanceReviewPage'
        AND name IN ('eDHR不合格评审','不合格评审'))
    ))<>3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed production execution menus';
  END IF;
  UPDATE system_menu SET name=CASE id
      WHEN 605071316 THEN '生产放行'
      WHEN 900230 THEN '工作任务'
      WHEN 9008300 THEN '不合格评审'
    END, updater='rename-execution-menus', update_time=NOW()
    WHERE id IN (605071316,900230,9008300);
  COMMIT;
END$$
DELIMITER ;
CALL rename_production_execution_menus();
DROP PROCEDURE rename_production_execution_menus;
