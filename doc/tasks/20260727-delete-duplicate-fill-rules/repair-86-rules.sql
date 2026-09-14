DROP PROCEDURE IF EXISTS codex_repair_fill_rules_20260727;

DELIMITER $$

CREATE PROCEDURE codex_repair_fill_rules_20260727()
BEGIN
    DECLARE v_target_count INT DEFAULT 0;
    DECLARE v_target_min_id BIGINT DEFAULT 0;
    DECLARE v_target_max_id BIGINT DEFAULT 0;
    DECLARE v_target_checksum CHAR(64);
    DECLARE v_carrier_count INT DEFAULT 0;
    DECLARE v_carrier_id BIGINT DEFAULT 0;
    DECLARE v_source_count INT DEFAULT 0;
    DECLARE v_updated_count INT DEFAULT 0;
    DECLARE v_deleted_count INT DEFAULT 0;
    DECLARE v_remaining_count INT DEFAULT 0;
    DECLARE v_formal_count INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SELECT COUNT(*),
           COALESCE(MIN(id), 0),
           COALESCE(MAX(id), 0),
           SHA2(
               GROUP_CONCAT(
                   CONCAT_WS(
                       '|',
                       id,
                       route_process_id,
                       HEX(batch_record_report_id),
                       HEX(rule_type),
                       HEX(scope_key),
                       HEX(signature_cell_key),
                       COALESCE(HEX(signature_role), 'NULL'),
                       HEX(candidate_source_type),
                       HEX(candidate_source_ids),
                       HEX(completion_policy),
                       due_minutes,
                       HEX(enabled),
                       COALESCE(HEX(CAST(fillable_scope_json AS CHAR CHARACTER SET utf8mb4)), 'NULL'),
                       COALESCE(HEX(remark), 'NULL'),
                       COALESCE(HEX(creator), 'NULL'),
                       DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s'),
                       COALESCE(HEX(updater), 'NULL'),
                       DATE_FORMAT(update_time, '%Y-%m-%d %H:%i:%s'),
                       HEX(deleted),
                       tenant_id,
                       COALESCE(batch_record_definition_id, 'NULL'),
                       COALESCE(batch_record_version_id, 'NULL')
                   )
                   ORDER BY id
                   SEPARATOR ';'
               ),
               256
           )
      INTO v_target_count, v_target_min_id, v_target_max_id, v_target_checksum
      FROM mes_pro_edhr_process_form_permission_rule
     WHERE tenant_id = 1
       AND route_process_id = 0
       AND batch_record_report_id = '1d05410f1d3140c5b8aa6786887ae69c'
       AND batch_record_version_id = 130
       AND rule_type = 'FILL'
       AND enabled = b'1'
       AND deleted = b'0';

    IF v_target_count <> 87
       OR v_target_min_id <> 3217
       OR v_target_max_id <> 3303
       OR v_target_checksum <> 'ffc016241194cdd3dea3bd14375f788f5cfd7ba2630af514d6a17a28013b54f8' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Target rules changed after snapshot; repair aborted';
    END IF;

    SELECT COUNT(*), COALESCE(MIN(id), 0)
      INTO v_carrier_count, v_carrier_id
      FROM mes_pro_edhr_process_form_permission_rule
     WHERE tenant_id = 1
       AND route_process_id = 0
       AND batch_record_report_id = '1d05410f1d3140c5b8aa6786887ae69c'
       AND batch_record_version_id = 130
       AND rule_type = 'FILL'
       AND scope_key = 'CODX_VFC_ASSIST_1'
       AND enabled = b'1'
       AND deleted = b'0';

    IF v_carrier_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Retained carrier is not unique; repair aborted';
    END IF;

    SELECT COUNT(*)
      INTO v_source_count
      FROM mes_pro_edhr_process_form_permission_rule
     WHERE id = 558
       AND tenant_id = 1
       AND route_process_id = 0
       AND batch_record_definition_id = 47
       AND batch_record_version_id = 130
       AND rule_type = 'FILL'
       AND scope_key = 'ALL'
       AND signature_cell_key = ''
       AND candidate_source_type = 'ROLE'
       AND candidate_source_ids = '910405'
       AND enabled = b'1'
       AND deleted = b'0';

    IF v_source_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Formal source rule 558 no longer matches; repair aborted';
    END IF;

    UPDATE mes_pro_edhr_process_form_permission_rule AS target
    JOIN mes_pro_edhr_process_form_permission_rule AS source
      ON source.id = 558
       SET target.scope_key = source.scope_key,
           target.signature_cell_key = source.signature_cell_key,
           target.signature_role = source.signature_role,
           target.candidate_source_type = source.candidate_source_type,
           target.candidate_source_ids = source.candidate_source_ids,
           target.completion_policy = source.completion_policy,
           target.due_minutes = source.due_minutes,
           target.enabled = source.enabled,
           target.fillable_scope_json = source.fillable_scope_json,
           target.remark = source.remark,
           target.updater = '1',
           target.update_time = CURRENT_TIMESTAMP,
           target.batch_record_definition_id = source.batch_record_definition_id
     WHERE target.id = v_carrier_id
       AND target.tenant_id = 1
       AND target.route_process_id = 0
       AND target.batch_record_report_id = '1d05410f1d3140c5b8aa6786887ae69c'
       AND target.batch_record_version_id = 130
       AND target.rule_type = 'FILL'
       AND target.scope_key = 'CODX_VFC_ASSIST_1'
       AND target.enabled = b'1'
       AND target.deleted = b'0';

    SET v_updated_count = ROW_COUNT();

    IF v_updated_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Retained rule update count was not 1; repair aborted';
    END IF;

    DELETE FROM mes_pro_edhr_process_form_permission_rule
     WHERE tenant_id = 1
       AND route_process_id = 0
       AND batch_record_report_id = '1d05410f1d3140c5b8aa6786887ae69c'
       AND batch_record_version_id = 130
       AND rule_type = 'FILL'
       AND enabled = b'1'
       AND deleted = b'0'
       AND id <> v_carrier_id;

    SET v_deleted_count = ROW_COUNT();

    IF v_deleted_count <> 86 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Deleted rule count was not 86; repair aborted';
    END IF;

    SELECT COUNT(*)
      INTO v_remaining_count
      FROM mes_pro_edhr_process_form_permission_rule
     WHERE tenant_id = 1
       AND route_process_id = 0
       AND batch_record_report_id = '1d05410f1d3140c5b8aa6786887ae69c'
       AND batch_record_version_id = 130
       AND rule_type = 'FILL'
       AND enabled = b'1'
       AND deleted = b'0';

    SELECT COUNT(*)
      INTO v_formal_count
      FROM mes_pro_edhr_process_form_permission_rule
     WHERE id = v_carrier_id
       AND tenant_id = 1
       AND route_process_id = 0
       AND batch_record_report_id = '1d05410f1d3140c5b8aa6786887ae69c'
       AND batch_record_definition_id = 47
       AND batch_record_version_id = 130
       AND rule_type = 'FILL'
       AND scope_key = 'ALL'
       AND signature_cell_key = ''
       AND signature_role IS NULL
       AND candidate_source_type = 'ROLE'
       AND candidate_source_ids = '910405'
       AND completion_policy = 'ANY_ONE'
       AND due_minutes = 2147483647
       AND fillable_scope_json IS NULL
       AND enabled = b'1'
       AND deleted = b'0';

    IF v_remaining_count <> 1 OR v_formal_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Post-repair rule verification failed; repair aborted';
    END IF;

    COMMIT;

    SELECT v_carrier_id AS retained_rule_id,
           v_updated_count AS updated_rows,
           v_deleted_count AS deleted_rows,
           v_remaining_count AS remaining_rows;
END$$

DELIMITER ;

SET SESSION TRANSACTION ISOLATION LEVEL SERIALIZABLE;
SET SESSION group_concat_max_len = 1000000;
CALL codex_repair_fill_rules_20260727();
