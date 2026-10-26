-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_rename_production_execution_menus,20261026_simplify_form_trace_menus; type=menu; riskLevel=low
-- Rename descendant navigation labels only, including hidden entries.
-- Preserve root name, route identities, business permissions, role grants and tenant packages.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS strip_edhr_child_menu_prefixes;
DELIMITER $$
CREATE PROCEDURE strip_edhr_child_menu_prefixes()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    DROP TEMPORARY TABLE IF EXISTS tmp_edhr_prefix_children;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND type=1
      AND path='/mes/edhr-batch-processing' AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed eDHR batch record root menu';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_edhr_prefix_children;
  CREATE TEMPORARY TABLE tmp_edhr_prefix_children (id BIGINT PRIMARY KEY);
  INSERT INTO tmp_edhr_prefix_children
  WITH RECURSIVE descendants AS (
    SELECT id FROM system_menu WHERE parent_id=900220 AND deleted=b'0'
    UNION ALL
    SELECT m.id FROM system_menu m JOIN descendants d ON m.parent_id=d.id
      WHERE m.deleted=b'0'
  ) SELECT id FROM descendants;
  IF EXISTS (SELECT 1 FROM system_menu m JOIN tmp_edhr_prefix_children d ON d.id=m.id
      WHERE m.type IN (1,2) AND LOWER(LEFT(m.name,4))='edhr'
        AND CHAR_LENGTH(TRIM(SUBSTRING(m.name,5)))=0) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Removing eDHR prefix would leave an empty menu label';
  END IF;
  UPDATE system_menu m JOIN tmp_edhr_prefix_children d ON d.id=m.id
    SET m.name=TRIM(SUBSTRING(m.name,5)),m.updater='strip-edhr-prefix',m.update_time=NOW()
    WHERE m.type IN (1,2) AND LOWER(LEFT(m.name,4))='edhr';
  COMMIT;
  DROP TEMPORARY TABLE tmp_edhr_prefix_children;
END$$
DELIMITER ;
CALL strip_edhr_child_menu_prefixes();
DROP PROCEDURE strip_edhr_child_menu_prefixes;
