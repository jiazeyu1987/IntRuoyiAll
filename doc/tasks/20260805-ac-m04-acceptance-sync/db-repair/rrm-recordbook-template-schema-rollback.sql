-- Rollback for AC-M04 / RRM local recordbook template schema repair.
-- Restores the exact entry_schema_json captured before
-- rrm-recordbook-template-schema-apply.sql.

START TRANSACTION;

DROP PROCEDURE IF EXISTS codex_acm04_rrm_recordbook_template_schema_rollback;

DELIMITER //
CREATE PROCEDURE codex_acm04_rrm_recordbook_template_schema_rollback()
BEGIN
    DECLARE target_count INT DEFAULT 0;

    SELECT COUNT(*)
      INTO target_count
      FROM mes_pro_edhr_recordbook rb
      JOIN mes_pro_edhr_recordbook_template tpl
        ON tpl.id = rb.template_id
       AND tpl.deleted = b'0'
     WHERE rb.id = 980011
       AND rb.recordbook_code = 'RRM-20260801-PP-MO-001-PRODUCTION-RB'
       AND rb.deleted = b'0'
       AND tpl.id = 980010
       AND tpl.template_code = 'RRM-20260801-PRODUCTION-RECORD-TPL';

    IF target_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM recordbook/template rollback target is not unique';
    END IF;

    UPDATE mes_pro_edhr_recordbook_template
       SET entry_schema_json = '[{"key":"fieldValues","label":"Field Values","type":"text","required":null,"min":null,"max":null,"options":null},{"key":"defects","label":"Defects","type":"text","required":null,"min":null,"max":null,"options":null},{"key":"productionOrder","label":"Production Order","type":"text","required":null,"min":null,"max":null,"options":null},{"key":"process","label":"Process","type":"text","required":null,"min":null,"max":null,"options":null},{"key":"employee","label":"Employee","type":"text","required":null,"min":null,"max":null,"options":null}]',
           updater = 'codex-acm04-rrm-recordbook-schema-rollback',
           update_time = NOW()
     WHERE id = 980010
       AND template_code = 'RRM-20260801-PRODUCTION-RECORD-TPL'
       AND deleted = b'0';

    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RRM recordbook template rollback did not affect exactly one row';
    END IF;
END//
DELIMITER ;

CALL codex_acm04_rrm_recordbook_template_schema_rollback();

DROP PROCEDURE IF EXISTS codex_acm04_rrm_recordbook_template_schema_rollback;

COMMIT;
