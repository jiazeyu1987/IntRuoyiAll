-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_dcc_project_code_to_mdm_hide_edhr_config,20260626_dcc_basic_data_global_submenu,20261117_dcc_file_type_taxonomy_menu_restore; type=menu; riskLevel=low
-- Display labels only. Keep menu IDs, URLs, permissions, role grants and tenant packages unchanged.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS rename_mdm_dcc_menu_names;
DELIMITER $$
CREATE PROCEDURE rename_mdm_dcc_menu_names()
BEGIN
  DECLARE root_id BIGINT;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  IF (SELECT COUNT(*) FROM system_menu WHERE parent_id=0 AND path='/mdm'
      AND type=1 AND status=0 AND deleted=b'0')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unique enabled master data root required';
  END IF;
  SELECT id INTO root_id FROM system_menu WHERE parent_id=0 AND path='/mdm'
    AND type=1 AND status=0 AND deleted=b'0';
  IF (SELECT COUNT(*) FROM system_menu WHERE parent_id=root_id AND type=2
      AND status=0 AND deleted=b'0' AND (
        (component='dcc/controlled-file/basic-data/project-code/index' AND name IN ('DCC项目代码','项目代码'))
        OR (component='dcc/controlled-file/basic-data/product-catalog/index' AND name IN ('DCC产品目录','产品目录'))
        OR (component='dcc/controlled-file/basic-data/file-type-taxonomy/index' AND name IN ('DCC文件分类','文件分类'))
      ))<>3
    OR (SELECT COUNT(*) FROM system_menu WHERE parent_id=root_id
      AND type=2 AND status=0 AND deleted=b'0' AND component IN (
        'dcc/controlled-file/basic-data/project-code/index',
        'dcc/controlled-file/basic-data/product-catalog/index',
        'dcc/controlled-file/basic-data/file-type-taxonomy/index'))<>3
    OR (SELECT COUNT(DISTINCT component) FROM system_menu WHERE parent_id=root_id
      AND type=2 AND status=0 AND deleted=b'0' AND component IN (
        'dcc/controlled-file/basic-data/project-code/index',
        'dcc/controlled-file/basic-data/product-catalog/index',
        'dcc/controlled-file/basic-data/file-type-taxonomy/index'))<>3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Master data menu missing, duplicated or renamed unexpectedly';
  END IF;
  UPDATE system_menu SET name=CASE component
      WHEN 'dcc/controlled-file/basic-data/project-code/index' THEN '项目代码'
      WHEN 'dcc/controlled-file/basic-data/product-catalog/index' THEN '产品目录'
      WHEN 'dcc/controlled-file/basic-data/file-type-taxonomy/index' THEN '文件分类'
    END,updater='mdm-menu-name',update_time=NOW()
  WHERE parent_id=root_id AND type=2 AND status=0 AND deleted=b'0'
    AND component IN ('dcc/controlled-file/basic-data/project-code/index',
      'dcc/controlled-file/basic-data/product-catalog/index',
      'dcc/controlled-file/basic-data/file-type-taxonomy/index')
    AND name IN ('DCC项目代码','DCC产品目录','DCC文件分类');
  COMMIT;
END$$
DELIMITER ;
CALL rename_mdm_dcc_menu_names();
DROP PROCEDURE rename_mdm_dcc_menu_names;
