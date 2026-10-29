-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261008_profile_business_config_menus,20260831_mes_pqc_production_release_menu,20260829_mes_form_center_unified_import_menu; type=menu; riskLevel=medium
-- Promote the existing eDHR directory and order its existing pages by the batch-record lifecycle.
-- Preserve names, menu IDs, components, permissions, button grants and absolute page URLs.
-- History uses the existing BatchRecordHistoryPage; no production/release business behavior changes.
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS migrate_edhr_root_menu_business_order;
DELIMITER $$
CREATE PROCEDURE migrate_edhr_root_menu_business_order()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  DROP TEMPORARY TABLE IF EXISTS tmp_edhr_menu_order;
  CREATE TEMPORARY TABLE tmp_edhr_menu_order (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    component VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    parent_id BIGINT NOT NULL,
    old_path VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    path VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    sort INT NOT NULL
  );
  INSERT INTO tmp_edhr_menu_order VALUES
    (6013, '金蝶同步运行', 'erp/sync/index', 2563, 'kingdee-sync', '/erp/kingdee-sync', 10),
    (991502, '自动同步配置', 'erp/sync/config/index', 2563, 'sync/config', '/erp/sync/config', 20),
    (2565, '产品信息', 'erp/product/product/index', 5101, 'erp-product', '/mes/md/erp-product', 30),
    (2590, '产品库存', 'erp/stock/stock/index', 5101, 'erp-stock', '/mes/md/erp-stock', 40),
    (6023, '物料清单', 'erp/production/bom-list/index', 6020, 'bom-list', '/erp/production/bom-list', 50),
    (6021, '生产用料清单', 'erp/production/material-list/index', 6020, 'material-list', '/erp/production/material-list', 60),
    (6032, '生产领料单列表', 'erp/production/pick-list/index', 6020, 'pick-list', '/erp/production/pick-list', 70),
    (605071318, '生产补料单列表', 'erp/production/replenishment-list/index', 6020, 'replenishment-list', '/erp/production/replenishment-list', 80),
    (6025, '即时库存', 'erp/production/inventory-list/index', 6020, 'inventory-list', '/erp/production/inventory-list', 90),
    (5110, '物料产品分类', 'mes/md/item/type/index', 5101, 'item-type', '/mes/md/item-type', 100),
    (5130, '计量单位', 'mes/md/unitmeasure/index', 5101, 'unit-measure', '/mes/md/unit-measure', 110),
    (5120, '物料产品管理', 'mes/md/item/index', 5101, 'item', '/mes/md/item', 120),
    (990210, 'DCC项目代码', 'dcc/controlled-file/basic-data/project-code/index', 5101, 'dcc-project-code', '/mes/md/dcc-project-code', 130),
    (900365, '表单中心', 'mes/pro/batchrecordformlist/index', 900220, '/mes/pro/batch-record-form-list', '/mes/pro/batch-record-form-list', 140),
    (605071201, '表单模板', 'form-center/template/index', 990200, 'form-center/template', '/mdm/form-center/template', 150),
    (900272, 'eDHR独立表单', 'mes/pro/edhr-form/FormPage', 900220, '/mes/pro/feedback/edhr-form', '/mes/pro/feedback/edhr-form', 160),
    (5710, '工序设置', 'mes/pro/process/index', 5700, 'process', '/mes/pro/process', 170),
    (5718, 'MES工序', 'mes/pro/mes-process/index', 5700, 'mes-process', '/mes/pro/mes-process', 180),
    (5720, '工艺流程', 'mes/pro/route/index', 5700, 'route', '/mes/pro/route', 190),
    (900434, 'QA', 'mes/pro/processpool/QaRegulationPage', 900220, '/mes/pro/process-pool/qa-regulation', '/mes/pro/process-pool/qa-regulation', 200),
    (991501, 'eDHR配置', 'mes/pro/edhr-config/index', 5100, 'pro/edhr-config', '/mes/pro/edhr-config', 210),
    (900440, '批记录测试', 'mes/pro/edhr-batch/BatchRecordTestPage', 900220, '/mes/pro/feedback/edhr-batch-test', '/mes/pro/feedback/edhr-batch-test', 220),
    (5530, '生产工单', 'mes/pro/workorder/index', 5700, 'work-order', '/mes/pro/work-order', 230),
    (900436, '生产组长', 'mes/pro/processpool/ProductionLeaderWorkbenchPage', 900220, '/mes/pro/process-pool/production-leader', '/mes/pro/process-pool/production-leader', 240),
    (900437, '一线生产', 'mes/pro/edhr-batch/BatchProductionFillPage', 900220, '/mes/pro/feedback/edhr-batch-production-fill', '/mes/pro/feedback/edhr-batch-production-fill', 250),
    (900438, '一线PQC', 'mes/pro/edhr-batch/BatchPqcFillPage', 900220, '/mes/pro/feedback/edhr-batch-pqc-fill', '/mes/pro/feedback/edhr-batch-pqc-fill', 260),
    (900435, 'PQC组长', 'mes/pro/processpool/PqcLeaderWorkbenchPage', 900220, '/mes/pro/process-pool/pqc-leader', '/mes/pro/process-pool/pqc-leader', 270),
    (605071315, 'PQC生产放行', 'mes/pro/production-release/PqcProductionReleasePage', 900220, '/mes/production-release/pqc', '/mes/production-release/pqc', 280),
    (900033, '批次执行', 'mes/pro/edhr-batch/BatchExecutionListPage', 900220, '/mes/pro/feedback/edhr-batch-execution', '/mes/pro/feedback/edhr-batch-execution', 290),
    (900230, 'eDHR工作任务', 'mes/pro/edhr-work-task/WorkTaskBoardPage', 900220, '/mes/pro/feedback/edhr-work-task', '/mes/pro/feedback/edhr-work-task', 300),
    (9008300, 'eDHR不合格评审', 'mes/pro/edhr-nonconformance/NonconformanceReviewPage', 5700, 'feedback/edhr-nonconformance-review', '/mes/pro/feedback/edhr-nonconformance-review', 310),
    (900432, '表单日志', 'mes/pro/edhr/FormFillLogPage', 900220, '/mes/pro/feedback/edhr-form-fill-log', '/mes/pro/feedback/edhr-form-fill-log', 320),
    (900025, '表单追溯', 'mes/pro/edhr/FormTracePage', 900220, '/mes/pro/feedback/edhr-form-trace', '/mes/pro/feedback/edhr-form-trace', 330);

  START TRANSACTION;
  IF NOT EXISTS (
    SELECT 1 FROM system_menu WHERE id=900220 AND name='eDHR批记录' AND type=1
      AND deleted=b'0' AND status=0
      AND ((parent_id=5100 AND path='edhr-batch-processing')
        OR (parent_id=0 AND path='/mes/edhr-batch-processing'))
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed eDHR directory 900220';
  END IF;
  IF EXISTS (
    SELECT 1 FROM tmp_edhr_menu_order plan LEFT JOIN system_menu menu ON menu.id=plan.id
    WHERE menu.id IS NULL OR menu.deleted<>b'0' OR menu.status<>0 OR menu.type<>2
      OR BINARY menu.name<>BINARY plan.name OR BINARY menu.component<>BINARY plan.component
      OR NOT ((menu.parent_id=plan.parent_id AND BINARY menu.path=BINARY plan.old_path)
        OR (menu.parent_id=900220 AND BINARY menu.path=BINARY plan.path))
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed eDHR source page; refusing partial menu relocation';
  END IF;
  IF EXISTS (
    SELECT 1 FROM system_menu menu JOIN tmp_edhr_menu_order plan ON BINARY menu.path=BINARY plan.path
    WHERE menu.deleted=b'0' AND menu.type<>3 AND menu.id<>plan.id
  ) OR EXISTS (
    SELECT 1 FROM system_menu WHERE deleted=b'0' AND path='/mes/edhr-batch-processing' AND id<>900220
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='eDHR target route conflicts with another menu';
  END IF;
  IF EXISTS (
    SELECT 1 FROM system_menu WHERE id=991510
      AND NOT (deleted=b'0' AND name='eDHR历史追溯' AND parent_id=900220 AND type=2
        AND path='/mes/pro/feedback/edhr-batch-history'
        AND component='mes/pro/edhr-batch/BatchRecordHistoryPage'
        AND component_name='MesProEdhrBatchHistory'
        AND permission='mes:pro-edhr-batch-execution:query')
  ) OR EXISTS (
    SELECT 1 FROM system_menu WHERE deleted=b'0' AND id<>991510
      AND (path='/mes/pro/feedback/edhr-batch-history' OR component_name='MesProEdhrBatchHistory')
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='eDHR history menu identity is already occupied';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu_ids; cannot preserve menu scope';
  END IF;

  UPDATE system_menu SET parent_id=0, path='/mes/edhr-batch-processing', sort=319,
    updater='edhr-menu-order', update_time=NOW() WHERE id=900220;
  UPDATE system_menu menu JOIN tmp_edhr_menu_order plan ON menu.id=plan.id
    SET menu.parent_id=900220, menu.path=plan.path, menu.sort=plan.sort,
        menu.visible=b'1', menu.updater='edhr-menu-order', menu.update_time=NOW();
  INSERT INTO system_menu
    (id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,
     visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT 991510,'eDHR历史追溯','mes:pro-edhr-batch-execution:query',2,340,900220,
    '/mes/pro/feedback/edhr-batch-history','ep:clock','mes/pro/edhr-batch/BatchRecordHistoryPage',
    'MesProEdhrBatchHistory',0,b'1',b'0',b'1','edhr-menu-order',NOW(),'edhr-menu-order',NOW(),b'0'
  WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=991510);

  -- Only roles already allowed to see the batch execution list receive its history entry.
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,991510,'edhr-menu-order',NOW(),'edhr-menu-order',NOW(),b'0',source.tenant_id
  FROM system_role_menu source WHERE source.deleted=b'0' AND source.menu_id=900033
    AND NOT EXISTS (SELECT 1 FROM system_role_menu target WHERE target.deleted=b'0'
      AND target.role_id=source.role_id AND target.tenant_id=source.tenant_id AND target.menu_id=991510);

  DROP TEMPORARY TABLE IF EXISTS tmp_edhr_menu_descendants;
  CREATE TEMPORARY TABLE tmp_edhr_menu_descendants (id BIGINT PRIMARY KEY);
  INSERT INTO tmp_edhr_menu_descendants
  WITH RECURSIVE descendants AS (
    SELECT id FROM system_menu WHERE parent_id=900220 AND deleted=b'0'
    UNION ALL
    SELECT child.id FROM system_menu child JOIN descendants parent ON child.parent_id=parent.id
      WHERE child.deleted=b'0'
  ) SELECT DISTINCT id FROM descendants;
  -- Preserve navigation for every role owning a moved page/button; no leaf permission is broadened.
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,900220,'edhr-menu-order',NOW(),'edhr-menu-order',NOW(),b'0',source.tenant_id
  FROM system_role_menu source JOIN tmp_edhr_menu_descendants child ON child.id=source.menu_id
  WHERE source.deleted=b'0' AND NOT EXISTS (SELECT 1 FROM system_role_menu target
    WHERE target.deleted=b'0' AND target.role_id=source.role_id
      AND target.tenant_id=source.tenant_id AND target.menu_id=900220);

  UPDATE system_tenant_package package
  SET menu_ids=JSON_ARRAY_APPEND(CAST(package.menu_ids AS JSON),'$',991510),
      updater='edhr-menu-order',update_time=NOW()
  WHERE package.deleted=b'0'
    AND JSON_CONTAINS(CAST(package.menu_ids AS JSON),CAST('900033' AS JSON),'$')
    AND NOT JSON_CONTAINS(CAST(package.menu_ids AS JSON),CAST('991510' AS JSON),'$');
  UPDATE system_tenant_package package
  SET menu_ids=JSON_ARRAY_APPEND(CAST(package.menu_ids AS JSON),'$',900220),
      updater='edhr-menu-order',update_time=NOW()
  WHERE package.deleted=b'0'
    AND NOT JSON_CONTAINS(CAST(package.menu_ids AS JSON),CAST('900220' AS JSON),'$')
    AND EXISTS (SELECT 1 FROM tmp_edhr_menu_descendants child
      WHERE JSON_CONTAINS(CAST(package.menu_ids AS JSON),CAST(child.id AS JSON),'$'));

  IF (SELECT COUNT(*) FROM system_menu menu JOIN tmp_edhr_menu_order plan ON menu.id=plan.id
      WHERE menu.parent_id=900220 AND menu.sort=plan.sort AND BINARY menu.path=BINARY plan.path
        AND BINARY menu.name=BINARY plan.name AND menu.visible=b'1' AND menu.deleted=b'0')<>33 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='eDHR menu order postcondition failed';
  END IF;
  COMMIT;
  DROP TEMPORARY TABLE tmp_edhr_menu_descendants;
  DROP TEMPORARY TABLE tmp_edhr_menu_order;
END$$
DELIMITER ;
CALL migrate_edhr_root_menu_business_order();
DROP PROCEDURE migrate_edhr_root_menu_business_order;
