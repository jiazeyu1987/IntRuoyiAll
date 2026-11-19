-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261119_edhr_navigation_names,20261119_mdm_dcc_menu_names; type=menu; riskLevel=low
-- Deduplicate existing directory/page labels, including hidden entries. See docs/menu-name-deduplication.md.
-- Preserve IDs, parentage, paths, components, permissions, visibility, roles and tenant-package grants.
-- Every changed label has at most four characters; keep 不合格评审 unchanged.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS deduplicate_navigation_names;
DELIMITER $$
CREATE PROCEDURE deduplicate_navigation_names()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  DROP TEMPORARY TABLE IF EXISTS tmp_navigation_names;
  CREATE TEMPORARY TABLE tmp_navigation_names (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT NOT NULL,
    type INT NOT NULL,
    path VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    component VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    old_name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL UNIQUE
  );
  INSERT INTO tmp_navigation_names VALUES
    (2092, 2084, 2, 'statistics', 'mp/statistics/index', '数据统计', '运营统计'),
    (2119, 2084, 2, 'menu', 'mp/menu/index', '菜单管理', '微信菜单'),
    (2526, 2397, 2, 'product', 'crm/product/index', '产品管理', '销售产品'),
    (2920, 2760, 2, 'tool', 'ai/model/tool/index.vue', '工具管理', '模型工具'),
    (4002, 4001, 2, 'product', 'iot/product/product/index', '产品管理', '物联产品'),
    (4008, 4001, 2, 'device', 'iot/device/device/index', '设备管理', '物联设备'),
    (4014, 4001, 2, 'product-category', 'iot/product/category/index', '产品分类', '物联分类'),
    (5101, 5100, 1, 'md', '', '基础数据', '制造档案'),
    (5140, 5101, 2, 'client', 'mes/md/client/index', '客户管理', '客户档案'),
    (5150, 5101, 2, 'vendor', 'mes/md/vendor/index', '供应商管理', '供方档案'),
    (5798, 5780, 2, 'item-receipt', 'mes/wm/itemreceipt/index', '采购入库', '物料入库'),
    (5830, 5780, 2, 'return-vendor', 'mes/wm/returnvendor/index', '采购退货', '物料退货'),
    (5847, 5780, 2, 'return-sales', 'mes/wm/returnsales/index', '销售退货', '产品退货'),
    (5950, 5780, 1, 'stock-taking', '', '库存盘点', '仓储盘点'),
    (5970, 5780, 2, 'product-sales', 'mes/wm/productsales/index', '销售出库', '产品出库'),
    (6010, 2563, 2, 'kingdee-config', 'erp/config/index', '配置管理', '金蝶配置'),
    (6020, 2563, 1, 'production', '', '生产管理', '生产业务'),
    (6401, 6400, 1, 'md', '', '基础数据', '仓储档案'),
    (6814, 6800, 2, 'controlled-file/approval-tasks', 'dcc/controlled-file/approval-tasks/index', '受控浏览', '文控审批'),
    (900438, 991516, 2, '/mes/pro/feedback/edhr-batch-pqc-fill', 'mes/pro/edhr-batch/BatchPqcFillPage', '过程检验', '检验填报'),
    (980102, 980100, 2, 'product', 'showroom-admin/index', '产品管理', '展品管理'),
    (980104, 980100, 2, 'approval', 'showroom-admin/index', '审批中心', '展厅审批'),
    (991001, 991000, 1, 'base', '', '基础配置', '供应配置'),
    (991002, 991001, 2, 'code-rule', 'srm/code-rule/index', '编码规则', '供应编码'),
    (991010, 991000, 1, 'supplier', '', '供应商管理', '供方管理'),
    (991515, 900220, 1, '/mes/edhr-process-routes', '', '工艺流程', '工艺路线'),
    (991517, 900220, 1, '/mes/edhr-form-trace', '', '表单追溯', '追溯管理'),
    (605071201, 991512, 2, '/mdm/form-center/template', 'form-center/template/index', '模板管理', '表单模板');
  START TRANSACTION;
  IF EXISTS (
    SELECT 1 FROM tmp_navigation_names p
    LEFT JOIN system_menu m ON m.id=p.id AND m.deleted=b'0'
    WHERE m.id IS NULL OR m.parent_id<>p.parent_id OR m.type<>p.type
      OR NOT (BINARY m.path <=> BINARY p.path)
      OR NOT (BINARY m.component <=> BINARY p.component)
      OR NOT (BINARY m.name=BINARY p.old_name OR BINARY m.name=BINARY p.name)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Navigation menu missing, moved or changed unexpectedly';
  END IF;
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=2
      AND component='mes/pro/edhr-nonconformance/NonconformanceReviewPage'
      AND BINARY name=BINARY '不合格评审')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Nonconformance review label must remain unchanged';
  END IF;
  UPDATE system_menu m JOIN tmp_navigation_names p ON m.id=p.id
    SET m.name=p.name,m.updater='menu-name-dedup',m.update_time=NOW()
  WHERE m.deleted=b'0' AND BINARY m.name<>BINARY p.name;
  IF EXISTS (SELECT 1 FROM tmp_navigation_names p JOIN system_menu m ON m.id=p.id
      WHERE BINARY m.name<>BINARY p.name OR CHAR_LENGTH(m.name)>4) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Navigation label postcondition failed';
  END IF;
  IF EXISTS (SELECT name FROM system_menu WHERE deleted=b'0' AND type IN (1,2)
      GROUP BY name HAVING COUNT(*)>1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate directory/page label remains after rename';
  END IF;
  COMMIT;
  DROP TEMPORARY TABLE tmp_navigation_names;
END$$
DELIMITER ;
CALL deduplicate_navigation_names();
DROP PROCEDURE deduplicate_navigation_names;
