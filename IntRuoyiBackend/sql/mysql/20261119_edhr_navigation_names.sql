-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_strip_edhr_child_menu_prefixes,20261026_craft_library_process_management,20261026_edhr_device_resource_menus,20261026_split_inspection_regulation_menus,20261027_simple_production_parser_menu,20261028_simple_pqc_parser_menu,20261031_simple_common_parser_menu,20261027_rename_batch_execution_market_release; type=menu; riskLevel=low
-- Navigation labels only. Menu IDs, routes, visibility, permissions, role and tenant-package grants remain unchanged.
-- 不合格评审 is explicitly retained, including its existing five-character label.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS rename_edhr_navigation_names;
DELIMITER $$
CREATE PROCEDURE rename_edhr_navigation_names()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  DROP TEMPORARY TABLE IF EXISTS tmp_edhr_navigation_names;
  CREATE TEMPORARY TABLE tmp_edhr_navigation_names (
    component VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY,
    parent_id BIGINT NOT NULL,
    old_name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
  );
  INSERT INTO tmp_edhr_navigation_names VALUES
    ('mes/pro/batchrecordformlist/index',991512,'表单中心','表单管理'),
    ('form-center/template/index',991512,'表单模板','模板管理'),
    ('mes/pro/edhr-form/FormPage',991512,'表单','表单配置'),
    ('mes/pro/edhr-batch/BatchRecordTestPage',991512,'批记录测试','记录测试'),
    ('form-center/simple-parser/index',991512,'简易解析','生产编制'),
    ('mes/pro/processpool/QaRegulationPage',991513,'QA检验规程','检验规程'),
    ('mes/pro/processpool/CommonRegulationPage',991513,'通用检验规程','通用规程'),
    ('form-center/simple-pqc-parser/index',991513,'简易PQC解析','检验编制'),
    ('form-center/simple-common-parser/index',991513,'简易通用解析','通用编制'),
    ('mes/pro/craft/index',991514,'工艺库','工艺管理'),
    ('mes/pro/edhr-config/index',991515,'配置','记录配置'),
    ('mes/pro/processpool/ProductionLeaderWorkbenchPage',991516,'生产组长','生产班长'),
    ('mes/pro/edhr-batch/BatchProductionFillPage',991516,'一线生产','生产作业'),
    ('mes/pro/edhr-batch/BatchPqcFillPage',991516,'一线PQC','过程检验'),
    ('mes/pro/processpool/PqcLeaderWorkbenchPage',991516,'PQC组长','检验组长'),
    ('mes/dv/subject/index',5300,'点检保养项目','维保项目'),
    ('mes/dv/checkplan/index',5300,'点检保养方案','维保方案'),
    ('mes/md/workstation/index',5300,'工作站设置','工作站'),
    ('mes/qc/indicator/index',5300,'检测项设置','检验项目');
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND parent_id=0
      AND path='/mes/edhr-batch-processing' AND type=1 AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing eDHR navigation root';
  END IF;
  IF EXISTS (
    SELECT p.component FROM tmp_edhr_navigation_names p
    LEFT JOIN system_menu m ON BINARY m.component=BINARY p.component AND m.deleted=b'0'
    GROUP BY p.component,p.parent_id,p.old_name,p.name
    HAVING COUNT(m.id)<>1 OR SUM(m.type=2 AND m.parent_id=p.parent_id
      AND (BINARY m.name=BINARY p.old_name OR BINARY m.name=BINARY p.name))<>1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Menu missing, duplicated, moved or renamed unexpectedly';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id=991516 AND type=2
      AND component='mes/pro/edhr-nonconformance/NonconformanceReviewPage'
      AND name='不合格评审' AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Nonconformance review label must remain unchanged';
  END IF;
  UPDATE system_menu m JOIN tmp_edhr_navigation_names p ON BINARY m.component=BINARY p.component
    SET m.name=p.name,m.updater='edhr-menu-name',m.update_time=NOW()
  WHERE m.deleted=b'0' AND m.type=2 AND m.parent_id=p.parent_id AND BINARY m.name<>BINARY p.name;
  IF EXISTS (SELECT 1 FROM tmp_edhr_navigation_names p JOIN system_menu m
      ON BINARY m.component=BINARY p.component AND m.deleted=b'0'
      WHERE BINARY m.name<>BINARY p.name OR CHAR_LENGTH(m.name)>4) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Navigation label postcondition failed';
  END IF;
  COMMIT;
  DROP TEMPORARY TABLE tmp_edhr_navigation_names;
END$$
DELIMITER ;
CALL rename_edhr_navigation_names();
DROP PROCEDURE rename_edhr_navigation_names;
