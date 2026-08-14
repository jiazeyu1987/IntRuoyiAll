-- RRM M6 local-only E2E fixture for tenant 1 / 芋道源码.
-- Purpose: bind a formal PQC EMPLOYEE scope so /pqc/personnel can return a leader and employee.
-- No passwords, tokens, or remote environment data are stored in this file.

DROP PROCEDURE IF EXISTS seed_rrm_m6_pqc_employee_scope;
DELIMITER $$
CREATE PROCEDURE seed_rrm_m6_pqc_employee_scope()
BEGIN
    DECLARE v_tenant_count INT DEFAULT 0;
    DECLARE v_leader_count INT DEFAULT 0;
    DECLARE v_employee_count INT DEFAULT 0;
    DECLARE v_scope_count_before INT DEFAULT 0;
    DECLARE v_scope_count_after INT DEFAULT 0;

    SELECT COUNT(*) INTO v_tenant_count
    FROM system_tenant
    WHERE id = 1 AND deleted = b'0' AND status = 0;

    SELECT COUNT(*) INTO v_leader_count
    FROM system_users
    WHERE id = 512 AND username = 'huzonggang' AND tenant_id = 1 AND deleted = b'0' AND status = 0;

    SELECT COUNT(*) INTO v_employee_count
    FROM system_users
    WHERE id = 659 AND username = 'shangmengying' AND tenant_id = 1 AND deleted = b'0' AND status = 0;

    IF v_tenant_count <> 1 OR v_leader_count <> 1 OR v_employee_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM M6 PQC scope seed failed: tenant or users are not the authorized local fixtures';
    END IF;

    SELECT COUNT(*) INTO v_scope_count_before
    FROM mes_pro_process_pool_team_leader_scope
    WHERE tenant_id = 1
      AND leader_user_id = 512
      AND leader_type = 'PQC'
      AND scope_type = 'EMPLOYEE'
      AND employee_user_id = 659
      AND deleted = b'0';

    IF v_scope_count_before = 0 THEN
        INSERT INTO mes_pro_process_pool_team_leader_scope
            (leader_user_id, leader_type, scope_type, employee_user_id, process_id, workstation_id,
             production_line_id, equipment_id, work_order_id, enabled, remark, creator, updater, tenant_id)
        VALUES
            (512, 'PQC', 'EMPLOYEE', 659, NULL, NULL, NULL, NULL, NULL, b'1',
             'RRM M6 local E2E fixture: PQC leader huzonggang to employee shangmengying for pressure pump V21 actual employee switch',
             'codex-rrm-m6', 'codex-rrm-m6', 1);
    END IF;

    SELECT COUNT(*) INTO v_scope_count_after
    FROM mes_pro_process_pool_team_leader_scope
    WHERE tenant_id = 1
      AND leader_user_id = 512
      AND leader_type = 'PQC'
      AND scope_type = 'EMPLOYEE'
      AND employee_user_id = 659
      AND enabled = b'1'
      AND deleted = b'0';

    IF v_scope_count_after <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM M6 PQC scope seed failed: final enabled scope count is not exactly 1';
    END IF;
END$$
DELIMITER ;

START TRANSACTION;
CALL seed_rrm_m6_pqc_employee_scope();
COMMIT;
DROP PROCEDURE IF EXISTS seed_rrm_m6_pqc_employee_scope;

SELECT id, tenant_id, leader_user_id, leader_type, scope_type, employee_user_id, enabled, remark
FROM mes_pro_process_pool_team_leader_scope
WHERE tenant_id = 1
  AND leader_user_id = 512
  AND leader_type = 'PQC'
  AND scope_type = 'EMPLOYEE'
  AND employee_user_id = 659
  AND deleted = b'0';
