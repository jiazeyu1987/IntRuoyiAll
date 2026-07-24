-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- 生产工单编码统一为 ERP BillNo。
-- code 是唯一工单编码；order_source_code 不再保存 ERP 工单号或与 code 完全相同的重复值。
-- 本机授权执行范围：tenant_id=1。其它租户需要执行前显式修改该变量并重新预览。
SET @mes_work_order_code_erp_target_tenant_id := 1;

-- 一、预览需要迁移的历史错位数据。
-- 仅处理 eDHR 可选择的未冻结、未取消工单：
-- 1. 本地自动生成的 KDMO-* 工单编码；
-- 2. order_source_code 看起来是 ERP 生产工单号；
-- 3. 同租户下目标 ERP code 尚未被其他未删除工单占用；
-- 4. 本次迁移候选集中 ERP code 不重复，避免产生重复工单编码。
DROP TEMPORARY TABLE IF EXISTS tmp_mes_work_order_code_erp_fix;

CREATE TEMPORARY TABLE tmp_mes_work_order_code_erp_fix AS
SELECT
  wo.id,
  wo.tenant_id,
  wo.code AS old_code,
  wo.order_source_code AS erp_code
FROM mes_pro_work_order wo
WHERE wo.deleted = b'0'
  AND wo.tenant_id = @mes_work_order_code_erp_target_tenant_id
  AND wo.temporary_frozen = b'0'
  AND wo.status <> 3
  AND wo.code IS NOT NULL
  AND wo.order_source_code IS NOT NULL
  AND wo.code <> wo.order_source_code
  AND wo.code LIKE 'KDMO-%'
  AND wo.order_source_code REGEXP '^[0-9A-Za-z_-]*MO[0-9A-Za-z_-]*$'
  AND NOT EXISTS (
    SELECT 1
    FROM mes_pro_work_order duplicated
    WHERE duplicated.deleted = b'0'
      AND duplicated.tenant_id = wo.tenant_id
      AND duplicated.id <> wo.id
      AND duplicated.code = wo.order_source_code
  )
  AND NOT EXISTS (
    SELECT 1
    FROM mes_pro_work_order sibling
    WHERE sibling.deleted = b'0'
      AND sibling.tenant_id = wo.tenant_id
      AND sibling.id <> wo.id
      AND sibling.temporary_frozen = b'0'
      AND sibling.status <> 3
      AND sibling.code LIKE 'KDMO-%'
      AND sibling.order_source_code = wo.order_source_code
  );

-- 执行前请先导出本查询结果，作为精确回滚依据。
SELECT id, tenant_id, old_code, erp_code
FROM tmp_mes_work_order_code_erp_fix
ORDER BY tenant_id, id;

-- 二、同步已有 eDHR 快照编码，保证复盘、归档和执行列表继续展示统一后的 ERP code。
UPDATE mes_pro_batch_record_execution execution
JOIN tmp_mes_work_order_code_erp_fix fix ON fix.id = execution.work_order_id
SET execution.work_order_code = fix.erp_code,
    execution.update_time = NOW()
WHERE execution.deleted = b'0'
  AND execution.work_order_code COLLATE utf8mb4_unicode_ci = fix.old_code;

UPDATE mes_pro_edhr_batch_execution batch_execution
JOIN tmp_mes_work_order_code_erp_fix fix ON fix.id = batch_execution.work_order_id
SET batch_execution.work_order_code = fix.erp_code,
    batch_execution.update_time = NOW()
WHERE batch_execution.deleted = b'0'
  AND batch_execution.work_order_code COLLATE utf8mb4_unicode_ci = fix.old_code;

-- 三、将生产工单 code 修正为 ERP 工单号，并清空重复承载 ERP 工单号的 order_source_code。
UPDATE mes_pro_work_order wo
JOIN tmp_mes_work_order_code_erp_fix fix ON fix.id = wo.id
SET wo.code = fix.erp_code,
    wo.order_source_code = NULL,
    wo.update_time = NOW()
WHERE wo.deleted = b'0'
  AND wo.code = fix.old_code
  AND wo.order_source_code = fix.erp_code;

-- 四、清理 code 与 order_source_code 完全相同的重复值。
UPDATE mes_pro_work_order
SET order_source_code = NULL,
    update_time = NOW()
WHERE deleted = b'0'
  AND tenant_id = @mes_work_order_code_erp_target_tenant_id
  AND code IS NOT NULL
  AND order_source_code IS NOT NULL
  AND order_source_code = code;

-- 五、验证。两个查询均应返回 0。
SELECT COUNT(*) AS remaining_selectable_local_auto_code_with_erp_source_count
FROM mes_pro_work_order
WHERE deleted = b'0'
  AND tenant_id = @mes_work_order_code_erp_target_tenant_id
  AND temporary_frozen = b'0'
  AND status <> 3
  AND code LIKE 'KDMO-%'
  AND order_source_code REGEXP '^[0-9A-Za-z_-]*MO[0-9A-Za-z_-]*$';

SELECT COUNT(*) AS duplicated_order_source_code_count
FROM mes_pro_work_order
WHERE deleted = b'0'
  AND tenant_id = @mes_work_order_code_erp_target_tenant_id
  AND code IS NOT NULL
  AND order_source_code IS NOT NULL
  AND order_source_code = code;
