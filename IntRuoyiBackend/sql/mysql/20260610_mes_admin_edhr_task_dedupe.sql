-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
START TRANSACTION;

UPDATE mes_pro_task t
JOIN (
    SELECT process_id, MIN(id) AS keep_id
    FROM mes_pro_task
    WHERE tenant_id = 1
      AND work_order_id = 903245
      AND route_id = 900022
      AND deleted = b'0'
    GROUP BY process_id
    HAVING COUNT(*) > 1
) k ON k.process_id = t.process_id
SET t.deleted = b'1',
    t.updater = 'codex',
    t.update_time = NOW(),
    t.remark = CONCAT(COALESCE(NULLIF(t.remark, ''), ''), CASE WHEN COALESCE(NULLIF(t.remark, ''), '') = '' THEN '' ELSE ' | ' END, 'deduped for admin eDHR E2E')
WHERE t.tenant_id = 1
  AND t.work_order_id = 903245
  AND t.route_id = 900022
  AND t.deleted = b'0'
  AND t.id <> k.keep_id;

COMMIT;
