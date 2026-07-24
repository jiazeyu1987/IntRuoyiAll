-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- 将 eDHR Word 导入自动补齐工序编码从长前缀 EDHR_PROC_ 迁移为短前缀 ER。
-- 安全门禁：如果目标 ER 编码已存在，先停止并人工处理冲突，避免唯一性/引用歧义。

SET @collision_count := (
    SELECT COUNT(*)
    FROM mes_pro_process target
    WHERE target.deleted = b'0'
      AND target.code LIKE 'ER%'
      AND EXISTS (
          SELECT 1
          FROM mes_pro_process source
          WHERE source.deleted = b'0'
            AND source.code LIKE 'EDHR\\_PROC\\_%'
            AND CONCAT('ER', SUBSTRING(source.code, 11)) = target.code
            AND source.tenant_id = target.tenant_id
      )
);

UPDATE mes_pro_process
SET code = CONCAT('ER', SUBSTRING(code, 11)),
    update_time = NOW()
WHERE @collision_count = 0
  AND deleted = b'0'
  AND code LIKE 'EDHR\\_PROC\\_%';
