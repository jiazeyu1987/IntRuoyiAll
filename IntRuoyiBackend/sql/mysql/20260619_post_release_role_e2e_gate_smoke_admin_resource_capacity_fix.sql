-- release-migration: allowedEnvironments=test,backup; dependsOn=20260619_post_release_role_e2e_gate_smoke_z_erp_job_permission_fix; type=permission; riskLevel=medium
-- Deduplicate admin tenant A03388 machinery-process capacities so smart-scheduling smoke can preview without resource conflicts.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS apply_pr_role_e2e_smoke_admin_capacity_fix;

DELIMITER $$

CREATE PROCEDURE apply_pr_role_e2e_smoke_admin_capacity_fix()
BEGIN
  DECLARE outer_keep_count INT DEFAULT 0;
  DECLARE inner_keep_count INT DEFAULT 0;
  DECLARE remaining_conflict_count INT DEFAULT 0;

  SELECT COUNT(*) INTO outer_keep_count
  FROM `mes_dv_machinery_process`
  WHERE `tenant_id` = 1
    AND `machinery_id` = 47
    AND `process_id` = 900370
    AND `standard_hourly_capacity` = 25.714286
    AND `deleted` = b'0';

  IF outer_keep_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'post release admin resource capacity fix requires one active keep row for machinery 47 process 900370 at 25.714286';
  END IF;

  SELECT COUNT(*) INTO inner_keep_count
  FROM `mes_dv_machinery_process`
  WHERE `tenant_id` = 1
    AND `machinery_id` = 47
    AND `process_id` = 900371
    AND `standard_hourly_capacity` = 40.000000
    AND `deleted` = b'0';

  IF inner_keep_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'post release admin resource capacity fix requires one active keep row for machinery 47 process 900371 at 40.000000';
  END IF;

  UPDATE `mes_dv_machinery_process`
  SET `deleted` = b'1',
      `updater` = 'post-release-role-e2e-gate',
      `update_time` = NOW()
  WHERE `tenant_id` = 1
    AND `machinery_id` = 47
    AND `process_id` = 900370
    AND `standard_hourly_capacity` = 61.904762
    AND `deleted` = b'0';

  UPDATE `mes_dv_machinery_process`
  SET `deleted` = b'1',
      `updater` = 'post-release-role-e2e-gate',
      `update_time` = NOW()
  WHERE `tenant_id` = 1
    AND `machinery_id` = 47
    AND `process_id` = 900371
    AND `standard_hourly_capacity` = 80.000000
    AND `deleted` = b'0';

  SELECT COUNT(*) INTO remaining_conflict_count
  FROM (
    SELECT `process_id`
    FROM `mes_dv_machinery_process`
    WHERE `tenant_id` = 1
      AND `machinery_id` = 47
      AND `process_id` IN (900370, 900371)
      AND `deleted` = b'0'
    GROUP BY `process_id`
    HAVING COUNT(DISTINCT `standard_hourly_capacity`) > 1
  ) AS `conflicts`;

  IF remaining_conflict_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'post release admin resource capacity fix still has active A03388 machinery-process conflicts';
  END IF;

  SELECT COUNT(*) INTO outer_keep_count
  FROM `mes_dv_machinery_process`
  WHERE `tenant_id` = 1
    AND `machinery_id` = 47
    AND `process_id` = 900370
    AND `standard_hourly_capacity` = 25.714286
    AND `deleted` = b'0';

  SELECT COUNT(*) INTO inner_keep_count
  FROM `mes_dv_machinery_process`
  WHERE `tenant_id` = 1
    AND `machinery_id` = 47
    AND `process_id` = 900371
    AND `standard_hourly_capacity` = 40.000000
    AND `deleted` = b'0';

  IF outer_keep_count <> 1 OR inner_keep_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'post release admin resource capacity fix did not preserve the expected A03388 keep rows';
  END IF;
END$$

DELIMITER ;

CALL apply_pr_role_e2e_smoke_admin_capacity_fix();

DROP PROCEDURE IF EXISTS apply_pr_role_e2e_smoke_admin_capacity_fix;
