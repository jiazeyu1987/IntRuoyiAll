-- release-target-preflight: migrationId=20261119_unique_navigation_names; allowedEnvironments=test,backup,prod
WITH renames(id,parent_id,type,path,component,old_name,name) AS (
 SELECT 2092, 2084, 2, 'statistics', 'mp/statistics/index', '数据统计', '运营统计'
 UNION ALL
 SELECT 2119, 2084, 2, 'menu', 'mp/menu/index', '菜单管理', '微信菜单'
 UNION ALL
 SELECT 2526, 2397, 2, 'product', 'crm/product/index', '产品管理', '销售产品'
 UNION ALL
 SELECT 2920, 2760, 2, 'tool', 'ai/model/tool/index.vue', '工具管理', '模型工具'
 UNION ALL
 SELECT 4002, 4001, 2, 'product', 'iot/product/product/index', '产品管理', '物联产品'
 UNION ALL
 SELECT 4008, 4001, 2, 'device', 'iot/device/device/index', '设备管理', '物联设备'
 UNION ALL
 SELECT 4014, 4001, 2, 'product-category', 'iot/product/category/index', '产品分类', '物联分类'
 UNION ALL
 SELECT 5101, 5100, 1, 'md', '', '基础数据', '制造档案'
 UNION ALL
 SELECT 5140, 5101, 2, 'client', 'mes/md/client/index', '客户管理', '客户档案'
 UNION ALL
 SELECT 5150, 5101, 2, 'vendor', 'mes/md/vendor/index', '供应商管理', '供方档案'
 UNION ALL
 SELECT 5798, 5780, 2, 'item-receipt', 'mes/wm/itemreceipt/index', '采购入库', '物料入库'
 UNION ALL
 SELECT 5830, 5780, 2, 'return-vendor', 'mes/wm/returnvendor/index', '采购退货', '物料退货'
 UNION ALL
 SELECT 5847, 5780, 2, 'return-sales', 'mes/wm/returnsales/index', '销售退货', '产品退货'
 UNION ALL
 SELECT 5950, 5780, 1, 'stock-taking', '', '库存盘点', '仓储盘点'
 UNION ALL
 SELECT 5970, 5780, 2, 'product-sales', 'mes/wm/productsales/index', '销售出库', '产品出库'
 UNION ALL
 SELECT 6010, 2563, 2, 'kingdee-config', 'erp/config/index', '配置管理', '金蝶配置'
 UNION ALL
 SELECT 6020, 2563, 1, 'production', '', '生产管理', '生产业务'
 UNION ALL
 SELECT 6401, 6400, 1, 'md', '', '基础数据', '仓储档案'
 UNION ALL
 SELECT 6814, 6800, 2, 'controlled-file/approval-tasks', 'dcc/controlled-file/approval-tasks/index', '受控浏览', '文控审批'
 UNION ALL
 SELECT 900411, 900218, 2, 'signature-records', 'signature-governance/index', '签名记录', '签名治理'
 UNION ALL
 SELECT 900438, 991516, 2, '/mes/pro/feedback/edhr-batch-pqc-fill', 'mes/pro/edhr-batch/BatchPqcFillPage', '过程检验', '检验填报'
 UNION ALL
 SELECT 980102, 980100, 2, 'product', 'showroom-admin/index', '产品管理', '展品管理'
 UNION ALL
 SELECT 980104, 980100, 2, 'approval', 'showroom-admin/index', '审批中心', '展厅审批'
 UNION ALL
 SELECT 991001, 991000, 1, 'base', '', '基础配置', '供应配置'
 UNION ALL
 SELECT 991002, 991001, 2, 'code-rule', 'srm/code-rule/index', '编码规则', '供应编码'
 UNION ALL
 SELECT 991010, 991000, 1, 'supplier', '', '供应商管理', '供方管理'
 UNION ALL
 SELECT 991515, 900220, 1, '/mes/edhr-process-routes', '', '工艺流程', '工艺路线'
 UNION ALL
 SELECT 991517, 900220, 1, '/mes/edhr-form-trace', '', '表单追溯', '追溯管理'
 UNION ALL
 SELECT 605071201, 991512, 2, '/mdm/form-center/template', 'form-center/template/index', '模板管理', '表单模板'
),
edhr_renames(component,parent_id,old_name,name) AS (
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
 WHEN EXISTS(SELECT 1 FROM renames p LEFT JOIN system_menu m ON m.id=p.id AND m.deleted=b'0' LEFT JOIN edhr_renames e ON BINARY e.component=BINARY m.component WHERE m.id IS NULL OR m.parent_id<>p.parent_id OR m.type<>p.type OR NOT(BINARY m.path <=> BINARY p.path) OR NOT(BINARY m.component <=> BINARY p.component) OR NOT(BINARY m.name=BINARY p.old_name OR BINARY m.name=BINARY p.name OR BINARY m.name=BINARY e.old_name)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261119_unique_navigation_names:MENU_IDENTITY_CONFLICT'
 WHEN (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=2 AND component='mes/pro/edhr-nonconformance/NonconformanceReviewPage' AND BINARY name=BINARY '不合格评审')<>1 THEN 'TARGET_PREFLIGHT_BLOCKED:20261119_unique_navigation_names:NONCONFORMANCE_LABEL_CHANGED'
 WHEN EXISTS(SELECT CASE WHEN u.id IS NOT NULL THEN u.name WHEN e.component IS NOT NULL THEN e.name WHEN m.component='dcc/controlled-file/basic-data/project-code/index' THEN '项目代码' WHEN m.component='dcc/controlled-file/basic-data/product-catalog/index' THEN '产品目录' WHEN m.component='dcc/controlled-file/basic-data/file-type-taxonomy/index' THEN '文件分类' ELSE m.name END final_name FROM system_menu m LEFT JOIN renames u ON u.id=m.id LEFT JOIN edhr_renames e ON BINARY e.component=BINARY m.component WHERE m.deleted=b'0' AND m.type IN (1,2) GROUP BY final_name HAVING COUNT(*)>1) THEN 'TARGET_PREFLIGHT_BLOCKED:20261119_unique_navigation_names:DUPLICATE_FINAL_LABEL'
 ELSE 'TARGET_PREFLIGHT_PASS:20261119_unique_navigation_names' END;
