-- release-target-preflight: migrationId=20261119_edhr_navigation_names; allowedEnvironments=test,backup,prod
WITH renames(component,parent_id,old_name,name) AS (
 SELECT 'mes/pro/batchrecordformlist/index',991512,'表单中心','表单管理'
 UNION ALL
 SELECT 'form-center/template/index',991512,'表单模板','模板管理'
 UNION ALL
 SELECT 'mes/pro/edhr-form/FormPage',991512,'表单','表单配置'
 UNION ALL
 SELECT 'mes/pro/edhr-batch/BatchRecordTestPage',991512,'批记录测试','记录测试'
 UNION ALL
 SELECT 'form-center/simple-parser/index',991512,'简易解析','生产编制'
 UNION ALL
 SELECT 'mes/pro/processpool/QaRegulationPage',991513,'QA检验规程','检验规程'
 UNION ALL
 SELECT 'mes/pro/processpool/CommonRegulationPage',991513,'通用检验规程','通用规程'
 UNION ALL
 SELECT 'form-center/simple-pqc-parser/index',991513,'简易PQC解析','检验编制'
 UNION ALL
 SELECT 'form-center/simple-common-parser/index',991513,'简易通用解析','通用编制'
 UNION ALL
 SELECT 'mes/pro/craft/index',991514,'工艺库','工艺管理'
 UNION ALL
 SELECT 'mes/pro/edhr-config/index',991515,'配置','记录配置'
 UNION ALL
 SELECT 'mes/pro/processpool/ProductionLeaderWorkbenchPage',991516,'生产组长','生产班长'
 UNION ALL
 SELECT 'mes/pro/edhr-batch/BatchProductionFillPage',991516,'一线生产','生产作业'
 UNION ALL
 SELECT 'mes/pro/edhr-batch/BatchPqcFillPage',991516,'一线PQC','过程检验'
 UNION ALL
 SELECT 'mes/pro/processpool/PqcLeaderWorkbenchPage',991516,'PQC组长','检验组长'
 UNION ALL
 SELECT 'mes/dv/subject/index',5300,'点检保养项目','维保项目'
 UNION ALL
 SELECT 'mes/dv/checkplan/index',5300,'点检保养方案','维保方案'
 UNION ALL
 SELECT 'mes/md/workstation/index',5300,'工作站设置','工作站'
 UNION ALL
 SELECT 'mes/qc/indicator/index',5300,'检测项设置','检验项目'
)
SELECT CASE
 WHEN NOT EXISTS(SELECT 1 FROM system_menu WHERE id=900220 AND parent_id=0 AND path='/mes/edhr-batch-processing' AND type=1 AND deleted=b'0') THEN 'TARGET_PREFLIGHT_BLOCKED:20261119_edhr_navigation_names:ROOT_CONFLICT'
 WHEN EXISTS(SELECT p.component FROM renames p LEFT JOIN system_menu m ON BINARY m.component=BINARY p.component AND m.deleted=b'0' GROUP BY p.component,p.parent_id,p.old_name,p.name HAVING COUNT(m.id)<>1 OR SUM(m.type=2 AND m.parent_id=p.parent_id AND (BINARY m.name=BINARY p.old_name OR BINARY m.name=BINARY p.name OR (p.component='mes/pro/edhr-form/FormPage' AND BINARY m.name=BINARY '独立表单')))<>1) THEN 'TARGET_PREFLIGHT_BLOCKED:20261119_edhr_navigation_names:MENU_IDENTITY_CONFLICT'
 WHEN NOT EXISTS(SELECT 1 FROM system_menu WHERE parent_id=991516 AND type=2 AND component='mes/pro/edhr-nonconformance/NonconformanceReviewPage' AND name='不合格评审' AND deleted=b'0') THEN 'TARGET_PREFLIGHT_BLOCKED:20261119_edhr_navigation_names:NONCONFORMANCE_LABEL_CHANGED'
 ELSE 'TARGET_PREFLIGHT_PASS:20261119_edhr_navigation_names' END;
