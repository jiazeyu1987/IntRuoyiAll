-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_data_sync_directory; type=menu; riskLevel=medium
-- Six navigation directories only. Preserve all existing leaf identities, visibility and business behavior.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS migrate_edhr_lifecycle_directories;
DELIMITER $$
CREATE PROCEDURE migrate_edhr_lifecycle_directories()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  DROP TEMPORARY TABLE IF EXISTS tmp_edhr_lifecycle_groups;
  CREATE TEMPORARY TABLE tmp_edhr_lifecycle_groups (
    id BIGINT PRIMARY KEY,name VARCHAR(50) CHARACTER SET utf8mb4,path VARCHAR(200) CHARACTER SET utf8mb4,sort INT);
  INSERT INTO tmp_edhr_lifecycle_groups VALUES
    (991512,'生产表单','/mes/edhr-production-forms',1),
    (991513,'检验表单','/mes/edhr-inspection-forms',2),
    (991514,'工序设置','/mes/edhr-process-settings',3),
    (991515,'工艺流程','/mes/edhr-process-routes',4),
    (991516,'生产执行','/mes/edhr-production-execution',5),
    (991517,'表单追溯','/mes/edhr-form-trace',6);
  DROP TEMPORARY TABLE IF EXISTS tmp_edhr_lifecycle_pages;
  CREATE TEMPORARY TABLE tmp_edhr_lifecycle_pages (
    id BIGINT PRIMARY KEY,parent_id BIGINT,name VARCHAR(50) CHARACTER SET utf8mb4,
    path VARCHAR(200) CHARACTER SET utf8mb4,component VARCHAR(255) CHARACTER SET utf8mb4,sort INT);
  INSERT INTO tmp_edhr_lifecycle_pages VALUES
    (900365,991512,'表单中心','/mes/pro/batch-record-form-list','mes/pro/batchrecordformlist/index',10),
    (605071201,991512,'表单模板','/mdm/form-center/template','form-center/template/index',20),
    (900272,991512,'eDHR独立表单','/mes/pro/feedback/edhr-form','mes/pro/edhr-form/FormPage',30),
    (900440,991512,'批记录测试','/mes/pro/feedback/edhr-batch-test','mes/pro/edhr-batch/BatchRecordTestPage',40),
    (900434,991513,'QA','/mes/pro/process-pool/qa-regulation','mes/pro/processpool/QaRegulationPage',10),
    (5110,991514,'物料产品分类','/mes/md/item-type','mes/md/item/type/index',10),
    (5130,991514,'计量单位','/mes/md/unit-measure','mes/md/unitmeasure/index',20),
    (5120,991514,'物料产品管理','/mes/md/item','mes/md/item/index',30),
    (5710,991514,'工序设置','/mes/pro/process','mes/pro/process/index',40),
    (5718,991514,'MES工序','/mes/pro/mes-process','mes/pro/mes-process/index',50),
    (990210,991515,'DCC项目代码','/mes/md/dcc-project-code','dcc/controlled-file/basic-data/project-code/index',10),
    (5720,991515,'工艺流程','/mes/pro/route','mes/pro/route/index',20),
    (991501,991515,'eDHR配置','/mes/pro/edhr-config','mes/pro/edhr-config/index',30),
    (900436,991516,'生产组长','/mes/pro/process-pool/production-leader','mes/pro/processpool/ProductionLeaderWorkbenchPage',10),
    (900437,991516,'一线生产','/mes/pro/feedback/edhr-batch-production-fill','mes/pro/edhr-batch/BatchProductionFillPage',20),
    (900438,991516,'一线PQC','/mes/pro/feedback/edhr-batch-pqc-fill','mes/pro/edhr-batch/BatchPqcFillPage',30),
    (900435,991516,'PQC组长','/mes/pro/process-pool/pqc-leader','mes/pro/processpool/PqcLeaderWorkbenchPage',40),
    (605071315,991516,'PQC生产放行','/mes/production-release/pqc','mes/pro/production-release/PqcProductionReleasePage',50),
    (900033,991516,'批次执行','/mes/pro/feedback/edhr-batch-execution','mes/pro/edhr-batch/BatchExecutionListPage',60),
    (900230,991516,'eDHR工作任务','/mes/pro/feedback/edhr-work-task','mes/pro/edhr-work-task/WorkTaskBoardPage',70),
    (9008300,991516,'eDHR不合格评审','/mes/pro/feedback/edhr-nonconformance-review','mes/pro/edhr-nonconformance/NonconformanceReviewPage',80),
    (900432,991517,'表单日志','/mes/pro/feedback/edhr-form-fill-log','mes/pro/edhr/FormFillLogPage',10),
    (900025,991517,'表单追溯','/mes/pro/feedback/edhr-form-trace','mes/pro/edhr/FormTracePage',20),
    (991510,991517,'eDHR历史追溯','/mes/pro/feedback/edhr-batch-history','mes/pro/edhr-batch/BatchRecordHistoryPage',30);
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND parent_id=0 AND type=1
      AND path='/mes/edhr-batch-processing' AND status=0 AND deleted=b'0')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991511 AND parent_id=900220
      AND path='/mes/edhr-data-sync' AND type=1 AND sort=-1 AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing eDHR root or preceding data sync directory';
  END IF;
  IF EXISTS (SELECT 1 FROM tmp_edhr_lifecycle_pages p LEFT JOIN system_menu m ON m.id=p.id
      WHERE m.id IS NULL OR m.deleted<>b'0' OR m.status<>0 OR m.type<>2
      OR BINARY m.name<>BINARY p.name OR BINARY m.path<>BINARY p.path OR BINARY m.component<>BINARY p.component
      OR m.parent_id NOT IN (900220,p.parent_id)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Lifecycle page missing or identity changed';
  END IF;
  IF EXISTS (SELECT 1 FROM tmp_edhr_lifecycle_groups g JOIN system_menu m ON m.id=g.id
      WHERE NOT (m.deleted=b'0' AND m.status=0 AND m.type=1 AND m.parent_id=900220
        AND BINARY m.name=BINARY g.name AND BINARY m.path=BINARY g.path
        AND m.permission='' AND m.component='' AND m.always_show=b'1' AND m.sort=g.sort)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Lifecycle directory ID occupied';
  END IF;
  IF EXISTS (SELECT 1 FROM tmp_edhr_lifecycle_groups g JOIN system_menu m ON BINARY m.path=BINARY g.path
      WHERE m.deleted=b'0' AND m.id<>g.id) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Lifecycle directory identity occupied';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu_ids';
  END IF;
  INSERT INTO system_menu
    (id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,
     visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT g.id,g.name,'',1,g.sort,900220,g.path,'ep:folder','','',0,b'1',b'1',b'1',
    'edhr-lifecycle',NOW(),'edhr-lifecycle',NOW(),b'0'
  FROM tmp_edhr_lifecycle_groups g WHERE NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.id=g.id);
  UPDATE system_menu m JOIN tmp_edhr_lifecycle_pages p ON m.id=p.id
    SET m.parent_id=p.parent_id,m.sort=p.sort,m.updater='edhr-lifecycle',m.update_time=NOW();
  DROP TEMPORARY TABLE IF EXISTS tmp_edhr_lifecycle_descendants;
  CREATE TEMPORARY TABLE tmp_edhr_lifecycle_descendants (id BIGINT,parent_id BIGINT,PRIMARY KEY(id,parent_id));
  INSERT INTO tmp_edhr_lifecycle_descendants
  WITH RECURSIVE descendants AS (
    SELECT id,parent_id FROM tmp_edhr_lifecycle_pages
    UNION ALL
    SELECT m.id,d.parent_id FROM system_menu m JOIN descendants d ON m.parent_id=d.id WHERE m.deleted=b'0'
  ) SELECT DISTINCT id,parent_id FROM descendants;
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT s.role_id,d.parent_id,'edhr-lifecycle',NOW(),'edhr-lifecycle',NOW(),b'0',s.tenant_id
  FROM system_role_menu s JOIN tmp_edhr_lifecycle_descendants d ON d.id=s.menu_id
  WHERE s.deleted=b'0' AND NOT EXISTS (SELECT 1 FROM system_role_menu t WHERE t.deleted=b'0'
    AND t.role_id=s.role_id AND t.tenant_id=s.tenant_id AND t.menu_id=d.parent_id);
  -- Append only the necessary parents to each existing child-holder package, in group order.
  UPDATE system_tenant_package p
  SET menu_ids=JSON_ARRAY_APPEND(CAST(p.menu_ids AS JSON),'$',991512),updater='edhr-lifecycle',update_time=NOW()
  WHERE p.deleted=b'0' AND NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST('991512' AS JSON),'$')
    AND EXISTS (SELECT 1 FROM tmp_edhr_lifecycle_descendants d WHERE d.parent_id=991512
      AND JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST(d.id AS JSON),'$'));
  UPDATE system_tenant_package p
  SET menu_ids=JSON_ARRAY_APPEND(CAST(p.menu_ids AS JSON),'$',991513),updater='edhr-lifecycle',update_time=NOW()
  WHERE p.deleted=b'0' AND NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST('991513' AS JSON),'$')
    AND EXISTS (SELECT 1 FROM tmp_edhr_lifecycle_descendants d WHERE d.parent_id=991513
      AND JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST(d.id AS JSON),'$'));
  UPDATE system_tenant_package p
  SET menu_ids=JSON_ARRAY_APPEND(CAST(p.menu_ids AS JSON),'$',991514),updater='edhr-lifecycle',update_time=NOW()
  WHERE p.deleted=b'0' AND NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST('991514' AS JSON),'$')
    AND EXISTS (SELECT 1 FROM tmp_edhr_lifecycle_descendants d WHERE d.parent_id=991514
      AND JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST(d.id AS JSON),'$'));
  UPDATE system_tenant_package p
  SET menu_ids=JSON_ARRAY_APPEND(CAST(p.menu_ids AS JSON),'$',991515),updater='edhr-lifecycle',update_time=NOW()
  WHERE p.deleted=b'0' AND NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST('991515' AS JSON),'$')
    AND EXISTS (SELECT 1 FROM tmp_edhr_lifecycle_descendants d WHERE d.parent_id=991515
      AND JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST(d.id AS JSON),'$'));
  UPDATE system_tenant_package p
  SET menu_ids=JSON_ARRAY_APPEND(CAST(p.menu_ids AS JSON),'$',991516),updater='edhr-lifecycle',update_time=NOW()
  WHERE p.deleted=b'0' AND NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST('991516' AS JSON),'$')
    AND EXISTS (SELECT 1 FROM tmp_edhr_lifecycle_descendants d WHERE d.parent_id=991516
      AND JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST(d.id AS JSON),'$'));
  UPDATE system_tenant_package p
  SET menu_ids=JSON_ARRAY_APPEND(CAST(p.menu_ids AS JSON),'$',991517),updater='edhr-lifecycle',update_time=NOW()
  WHERE p.deleted=b'0' AND NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST('991517' AS JSON),'$')
    AND EXISTS (SELECT 1 FROM tmp_edhr_lifecycle_descendants d WHERE d.parent_id=991517
      AND JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST(d.id AS JSON),'$'));
  IF (SELECT COUNT(*) FROM system_menu m JOIN tmp_edhr_lifecycle_pages p ON m.id=p.id
      WHERE m.parent_id=p.parent_id AND m.sort=p.sort)<>24 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Lifecycle grouping postcondition failed';
  END IF;
  COMMIT;
  DROP TEMPORARY TABLE tmp_edhr_lifecycle_descendants;
  DROP TEMPORARY TABLE tmp_edhr_lifecycle_pages;
  DROP TEMPORARY TABLE tmp_edhr_lifecycle_groups;
END$$
DELIMITER ;
CALL migrate_edhr_lifecycle_directories();
DROP PROCEDURE migrate_edhr_lifecycle_directories;
