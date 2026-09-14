-- ACM04 P0 backfill targeted rollback. Prefer full backup restore for complete rollback if later data was not created.
DROP PROCEDURE IF EXISTS acm04_drop_index_if_exists;
DROP PROCEDURE IF EXISTS acm04_drop_column_if_exists;
DELIMITER $$
CREATE PROCEDURE acm04_drop_index_if_exists(IN p_table_name VARCHAR(128), IN p_index_name VARCHAR(128)) BEGIN IF EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table_name AND INDEX_NAME = p_index_name) THEN SET @acm04_sql := CONCAT('ALTER TABLE `', p_table_name, '` DROP INDEX `', p_index_name, '`'); PREPARE stmt FROM @acm04_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt; END IF; END$$
CREATE PROCEDURE acm04_drop_column_if_exists(IN p_table_name VARCHAR(128), IN p_column_name VARCHAR(128)) BEGIN IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table_name AND COLUMN_NAME = p_column_name) THEN SET @acm04_sql := CONCAT('ALTER TABLE `', p_table_name, '` DROP COLUMN `', p_column_name, '`'); PREPARE stmt FROM @acm04_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt; END IF; END$$
DELIMITER ;
CALL acm04_drop_index_if_exists('mes_pro_process_pool_pqc_record','idx_mes_pro_process_pool_pqc_submit_event');
CALL acm04_drop_index_if_exists('mes_pro_process_pool_event','uk_mes_pro_process_pool_event_idem');
CALL acm04_drop_index_if_exists('mes_pro_process_pool_quantity_fragment','idx_mes_pro_process_pool_fragment_submit_event');
CALL acm04_drop_column_if_exists('mes_pro_process_pool_pqc_record','production_submit_event_id');
CALL acm04_drop_column_if_exists('mes_pro_process_pool_quantity_fragment','production_submit_event_id');
CALL acm04_drop_column_if_exists('mes_pro_process_pool_event','recordbook_entry_id');
CALL acm04_drop_column_if_exists('mes_pro_process_pool_event','event_idempotency_key');
START TRANSACTION;
DELETE FROM mes_pro_process_pool_event WHERE id IN (18,19,20,21,22,104,105,106,107,108,109,110,111,112,113,114,115,116,117) AND creator = 'codex-acm04-p0-backfill';
DELETE FROM mes_pro_edhr_recordbook_event WHERE id IN (980020,980021,980022,980023,980024,980025,980026,980027,980028,980029,980030,980031,980032,980033,980034,980035,980036,980037,980038,980039,980040) AND creator = 'codex-acm04-p0-backfill';
DELETE FROM mes_pro_edhr_recordbook_entry WHERE id IN (980020,980021,980022,980023,980024,980025,980026,980027,980028,980029,980030,980031,980032,980033,980034,980035,980036,980037,980038,980039,980040) AND creator = 'codex-acm04-p0-backfill';
DELETE FROM mes_pro_process_pool WHERE id = 15 AND creator = 'codex-acm04-p0-backfill';
UPDATE mes_pro_edhr_recordbook SET entry_count = 0, updater = 'codex-acm04-p0-backfill', update_time = NOW() WHERE id = 980011 AND tenant_id = 1;
UPDATE mes_pro_edhr_recordbook SET entry_count = 4, updater = 'codex-acm04-p0-backfill', update_time = NOW() WHERE id = 980010 AND tenant_id = 122;
COMMIT;
DROP PROCEDURE IF EXISTS acm04_drop_index_if_exists;
DROP PROCEDURE IF EXISTS acm04_drop_column_if_exists;
