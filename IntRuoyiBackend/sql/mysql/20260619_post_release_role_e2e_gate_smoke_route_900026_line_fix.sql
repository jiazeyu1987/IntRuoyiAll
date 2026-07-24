-- release-migration: allowedEnvironments=test,backup; dependsOn=20260619_post_release_role_e2e_gate_smoke_admin_resource_capacity_fix; type=config; riskLevel=medium
-- Bind route 900026 sting-process workstations to AUTO-LINE-01 so post-release smart-scheduling smoke can preview without LINE blockers.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS apply_pr_role_e2e_smoke_route_900026_line_fix;

DELIMITER $$

CREATE PROCEDURE apply_pr_role_e2e_smoke_route_900026_line_fix()
BEGIN
  DECLARE target_workstation_count INT DEFAULT 0;
  DECLARE line_ready_count INT DEFAULT 0;
  DECLARE conflicting_binding_count INT DEFAULT 0;
  DECLARE remaining_null_binding_count INT DEFAULT 0;
  DECLARE bound_count INT DEFAULT 0;

  SELECT COUNT(*) INTO line_ready_count
  FROM `mes_md_production_line`
  WHERE `id` = 900040
    AND `workshop_id` = 900011
    AND `status` = 0
    AND `calendar_plan_id` = 900030
    AND `deleted` = b'0';

  IF line_ready_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'post release route 900026 line fix requires enabled production line 900040 in workshop 900011 with calendar plan 900030';
  END IF;

  SELECT COUNT(*) INTO target_workstation_count
  FROM `mes_md_workstation`
  WHERE `id` IN (900113, 900114, 900115, 900116, 900117, 900118, 900119, 900120, 900121)
    AND `process_id` IN (900379, 900380, 900381, 900382, 900383, 900384, 900385, 900386, 900387)
    AND `workshop_id` = 900011
    AND `deleted` = b'0';

  IF target_workstation_count <> 9 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'post release route 900026 line fix requires nine active sting-process workstations in workshop 900011';
  END IF;

  SELECT COUNT(*) INTO conflicting_binding_count
  FROM `mes_md_workstation`
  WHERE `id` IN (900113, 900114, 900115, 900116, 900117, 900118, 900119, 900120, 900121)
    AND `deleted` = b'0'
    AND `production_line_id` IS NOT NULL
    AND `production_line_id` <> 900040;

  IF conflicting_binding_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'post release route 900026 line fix found unexpected existing production line bindings';
  END IF;

  UPDATE `mes_md_workstation`
  SET `production_line_id` = 900040,
      `updater` = 'post-release-role-e2e-gate',
      `update_time` = NOW()
  WHERE `id` IN (900113, 900114, 900115, 900116, 900117, 900118, 900119, 900120, 900121)
    AND `deleted` = b'0'
    AND `production_line_id` IS NULL;

  SELECT COUNT(*) INTO remaining_null_binding_count
  FROM `mes_md_workstation`
  WHERE `id` IN (900113, 900114, 900115, 900116, 900117, 900118, 900119, 900120, 900121)
    AND `deleted` = b'0'
    AND `production_line_id` IS NULL;

  IF remaining_null_binding_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'post release route 900026 line fix left null production line bindings';
  END IF;

  SELECT COUNT(*) INTO bound_count
  FROM `mes_md_workstation`
  WHERE `id` IN (900113, 900114, 900115, 900116, 900117, 900118, 900119, 900120, 900121)
    AND `deleted` = b'0'
    AND `production_line_id` = 900040;

  IF bound_count <> 9 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'post release route 900026 line fix did not bind all target workstations to production line 900040';
  END IF;
END$$

DELIMITER ;

CALL apply_pr_role_e2e_smoke_route_900026_line_fix();

DROP PROCEDURE IF EXISTS apply_pr_role_e2e_smoke_route_900026_line_fix;
