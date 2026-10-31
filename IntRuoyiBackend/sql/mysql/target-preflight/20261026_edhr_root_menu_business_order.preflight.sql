-- release-target-preflight: migrationId=20261026_edhr_root_menu_business_order; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
WITH tmp_edhr_menu_order (id,name,component,parent_id,old_path,path,sort) AS (
SELECT 6013,'金蝶同步运行','erp/sync/index',2563,'kingdee-sync','/erp/kingdee-sync',10
UNION ALL
SELECT 991502,'自动同步配置','erp/sync/config/index',2563,'sync/config','/erp/sync/config',20
UNION ALL
SELECT 2565,'产品信息','erp/product/product/index',5101,'erp-product','/mes/md/erp-product',30
UNION ALL
SELECT 2590,'产品库存','erp/stock/stock/index',5101,'erp-stock','/mes/md/erp-stock',40
UNION ALL
SELECT 6023,'物料清单','erp/production/bom-list/index',6020,'bom-list','/erp/production/bom-list',50
UNION ALL
SELECT 6021,'生产用料清单','erp/production/material-list/index',6020,'material-list','/erp/production/material-list',60
UNION ALL
SELECT 6032,'生产领料单列表','erp/production/pick-list/index',6020,'pick-list','/erp/production/pick-list',70
UNION ALL
SELECT 605071318,'生产补料单列表','erp/production/replenishment-list/index',6020,'replenishment-list','/erp/production/replenishment-list',80
UNION ALL
SELECT 6025,'即时库存','erp/production/inventory-list/index',6020,'inventory-list','/erp/production/inventory-list',90
UNION ALL
SELECT 5110,'物料产品分类','mes/md/item/type/index',5101,'item-type','/mes/md/item-type',100
UNION ALL
SELECT 5130,'计量单位','mes/md/unitmeasure/index',5101,'unit-measure','/mes/md/unit-measure',110
UNION ALL
SELECT 5120,'物料产品管理','mes/md/item/index',5101,'item','/mes/md/item',120
UNION ALL
SELECT 990210,'DCC项目代码','dcc/controlled-file/basic-data/project-code/index',5101,'dcc-project-code','/mes/md/dcc-project-code',130
UNION ALL
SELECT 900365,'表单中心','mes/pro/batchrecordformlist/index',900220,'/mes/pro/batch-record-form-list','/mes/pro/batch-record-form-list',140
UNION ALL
SELECT 605071201,'表单模板','form-center/template/index',990200,'form-center/template','/mdm/form-center/template',150
UNION ALL
SELECT 900272,'eDHR独立表单','mes/pro/edhr-form/FormPage',900220,'/mes/pro/feedback/edhr-form','/mes/pro/feedback/edhr-form',160
UNION ALL
SELECT 5710,'工序设置','mes/pro/process/index',5700,'process','/mes/pro/process',170
UNION ALL
SELECT 5718,'MES工序','mes/pro/mes-process/index',5700,'mes-process','/mes/pro/mes-process',180
UNION ALL
SELECT 5720,'工艺流程','mes/pro/route/index',5700,'route','/mes/pro/route',190
UNION ALL
SELECT 900434,'QA','mes/pro/processpool/QaRegulationPage',900220,'/mes/pro/process-pool/qa-regulation','/mes/pro/process-pool/qa-regulation',200
UNION ALL
SELECT 991501,'eDHR配置','mes/pro/edhr-config/index',5100,'pro/edhr-config','/mes/pro/edhr-config',210
UNION ALL
SELECT 900440,'批记录测试','mes/pro/edhr-batch/BatchRecordTestPage',900220,'/mes/pro/feedback/edhr-batch-test','/mes/pro/feedback/edhr-batch-test',220
UNION ALL
SELECT 5530,'生产工单','mes/pro/workorder/index',5700,'work-order','/mes/pro/work-order',230
UNION ALL
SELECT 900436,'生产组长','mes/pro/processpool/ProductionLeaderWorkbenchPage',900220,'/mes/pro/process-pool/production-leader','/mes/pro/process-pool/production-leader',240
UNION ALL
SELECT 900437,'一线生产','mes/pro/edhr-batch/BatchProductionFillPage',900220,'/mes/pro/feedback/edhr-batch-production-fill','/mes/pro/feedback/edhr-batch-production-fill',250
UNION ALL
SELECT 900438,'一线PQC','mes/pro/edhr-batch/BatchPqcFillPage',900220,'/mes/pro/feedback/edhr-batch-pqc-fill','/mes/pro/feedback/edhr-batch-pqc-fill',260
UNION ALL
SELECT 900435,'PQC组长','mes/pro/processpool/PqcLeaderWorkbenchPage',900220,'/mes/pro/process-pool/pqc-leader','/mes/pro/process-pool/pqc-leader',270
UNION ALL
SELECT 605071315,'PQC生产放行','mes/pro/production-release/PqcProductionReleasePage',900220,'/mes/production-release/pqc','/mes/production-release/pqc',280
UNION ALL
SELECT 900033,'批次执行','mes/pro/edhr-batch/BatchExecutionListPage',900220,'/mes/pro/feedback/edhr-batch-execution','/mes/pro/feedback/edhr-batch-execution',290
UNION ALL
SELECT 900230,'eDHR工作任务','mes/pro/edhr-work-task/WorkTaskBoardPage',900220,'/mes/pro/feedback/edhr-work-task','/mes/pro/feedback/edhr-work-task',300
UNION ALL
SELECT 9008300,'eDHR不合格评审','mes/pro/edhr-nonconformance/NonconformanceReviewPage',5700,'feedback/edhr-nonconformance-review','/mes/pro/feedback/edhr-nonconformance-review',310
UNION ALL
SELECT 900432,'表单日志','mes/pro/edhr/FormFillLogPage',900220,'/mes/pro/feedback/edhr-form-fill-log','/mes/pro/feedback/edhr-form-fill-log',320
UNION ALL
SELECT 900025,'表单追溯','mes/pro/edhr/FormTracePage',900220,'/mes/pro/feedback/edhr-form-trace','/mes/pro/feedback/edhr-form-trace',330)
SELECT CASE WHEN
  (NOT (NOT EXISTS (
    SELECT 1 FROM system_menu WHERE id=900220 AND name='eDHR批记录' AND type=1
      AND deleted=b'0' AND status=0
      AND ((parent_id=5100 AND path='edhr-batch-processing')
        OR (parent_id=0 AND path='/mes/edhr-batch-processing'))
  )))
  AND (NOT (EXISTS (
    SELECT 1 FROM tmp_edhr_menu_order plan LEFT JOIN system_menu menu ON menu.id=plan.id
    WHERE menu.id IS NULL OR menu.deleted<>b'0' OR menu.status<>0 OR menu.type<>2
      OR BINARY menu.name<>BINARY plan.name OR BINARY menu.component<>BINARY plan.component
      OR NOT ((menu.parent_id=plan.parent_id AND BINARY menu.path=BINARY plan.old_path)
        OR (menu.parent_id=900220 AND BINARY menu.path=BINARY plan.path))
  )))
  AND (NOT (EXISTS (
    SELECT 1 FROM system_menu menu JOIN tmp_edhr_menu_order plan ON BINARY menu.path=BINARY plan.path
    WHERE menu.deleted=b'0' AND menu.type<>3 AND menu.id<>plan.id
  ) OR EXISTS (
    SELECT 1 FROM system_menu WHERE deleted=b'0' AND path='/mes/edhr-batch-processing' AND id<>900220
  )))
  AND (NOT (EXISTS (
    SELECT 1 FROM system_menu WHERE id=991510
      AND NOT (deleted=b'0' AND name='eDHR历史追溯' AND parent_id=900220 AND type=2
        AND path='/mes/pro/feedback/edhr-batch-history'
        AND component='mes/pro/edhr-batch/BatchRecordHistoryPage'
        AND component_name='MesProEdhrBatchHistory'
        AND permission='mes:pro-edhr-batch-execution:query')
  ) OR EXISTS (
    SELECT 1 FROM system_menu WHERE deleted=b'0' AND id<>991510
      AND (path='/mes/pro/feedback/edhr-batch-history' OR component_name='MesProEdhrBatchHistory')
  )))
  AND (NOT (EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY'))))
THEN 'TARGET_PREFLIGHT_PASS:20261026_edhr_root_menu_business_order' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261026_edhr_root_menu_business_order:BASELINE_CONFLICT' END;
