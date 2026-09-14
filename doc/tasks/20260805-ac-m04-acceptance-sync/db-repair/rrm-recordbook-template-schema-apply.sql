-- AC-M04 / RRM local recordbook template schema repair.
-- Scope: local Docker MySQL ruoyi-vue-pro only.
-- Goal: align the task-owned RRM production recordbook template with
-- MesProFrontlineFeedbackPayloadSplitter's formal entryContent contract.

START TRANSACTION;

DROP PROCEDURE IF EXISTS codex_acm04_rrm_recordbook_template_schema_apply;

DELIMITER //
CREATE PROCEDURE codex_acm04_rrm_recordbook_template_schema_apply()
BEGIN
    DECLARE target_count INT DEFAULT 0;
    DECLARE missing_count INT DEFAULT 0;

    SELECT COUNT(*)
      INTO target_count
      FROM mes_pro_edhr_recordbook rb
      JOIN mes_pro_edhr_recordbook_template tpl
        ON tpl.id = rb.template_id
       AND tpl.deleted = b'0'
     WHERE rb.id = 980011
       AND rb.recordbook_code = 'RRM-20260801-PP-MO-001-PRODUCTION-RB'
       AND rb.status = 'OPEN'
       AND rb.deleted = b'0'
       AND tpl.id = 980010
       AND tpl.template_code = 'RRM-20260801-PRODUCTION-RECORD-TPL'
       AND tpl.recordbook_type = 'PRODUCTION'
       AND tpl.status = 'ACTIVE';

    IF target_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM recordbook/template target is not unique or not active/open';
    END IF;

    SELECT COUNT(*)
      INTO missing_count
      FROM mes_pro_edhr_recordbook_template tpl
     WHERE tpl.id = 980010
       AND tpl.template_code = 'RRM-20260801-PRODUCTION-RECORD-TPL'
       AND tpl.deleted = b'0'
       AND (
            JSON_SEARCH(tpl.entry_schema_json, 'one', 'equipmentParameters', NULL, '$[*].key') IS NULL
         OR JSON_SEARCH(tpl.entry_schema_json, 'one', 'rawPayload', NULL, '$[*].key') IS NULL
       );

    IF missing_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM recordbook template is not in the expected missing-required-fields state';
    END IF;

    UPDATE mes_pro_edhr_recordbook_template
       SET entry_schema_json = JSON_ARRAY_APPEND(
           entry_schema_json,
           '$',
           JSON_OBJECT('key', 'equipmentParameters', 'label', 'Equipment Parameters', 'type', 'text', 'required', false),
               '$',
               JSON_OBJECT('key', 'rawPayload', 'label', 'Raw Payload', 'type', 'text', 'required', false)
           ),
           updater = 'codex-acm04-rrm-recordbook-schema',
           update_time = NOW()
     WHERE id = 980010
       AND template_code = 'RRM-20260801-PRODUCTION-RECORD-TPL'
       AND deleted = b'0';

    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM recordbook template schema update did not affect exactly one row';
    END IF;

    SELECT COUNT(*)
      INTO missing_count
      FROM mes_pro_edhr_recordbook_template tpl
     WHERE tpl.id = 980010
       AND tpl.template_code = 'RRM-20260801-PRODUCTION-RECORD-TPL'
       AND tpl.deleted = b'0'
       AND (
            JSON_SEARCH(tpl.entry_schema_json, 'one', 'equipmentParameters', NULL, '$[*].key') IS NULL
         OR JSON_SEARCH(tpl.entry_schema_json, 'one', 'rawPayload', NULL, '$[*].key') IS NULL
       );

    IF missing_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM recordbook template schema verification failed after update';
    END IF;
END//
DELIMITER ;

CALL codex_acm04_rrm_recordbook_template_schema_apply();

DROP PROCEDURE IF EXISTS codex_acm04_rrm_recordbook_template_schema_apply;

COMMIT;
