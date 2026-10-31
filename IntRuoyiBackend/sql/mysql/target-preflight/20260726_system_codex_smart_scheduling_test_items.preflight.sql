-- release-target-preflight: migrationId=20260726_system_codex_smart_scheduling_test_items; allowedEnvironments=test,backup,prod
WITH seed_names AS (SELECT '智能排产-全链路冒烟：入池、预览、发布、日历、报工闭环' AS name UNION ALL SELECT '智能排产-只读一致性：工作台、排产工单、排程日历' AS name UNION ALL SELECT '智能排产-可点击安全巡检：危险写入必须显式确认' AS name)
SELECT CASE WHEN
(SELECT COUNT(*) FROM system_tenant WHERE id=1 AND deleted=b'0')=1
  AND NOT EXISTS (
    SELECT c.tenant_id,c.name FROM system_codex_test_case c JOIN seed_names s ON s.name=c.name
    WHERE c.tenant_id=1 AND c.deleted=b'0' GROUP BY c.tenant_id,c.name HAVING COUNT(*)>1
  )
  AND NOT EXISTS (
    SELECT 1 FROM system_codex_test_case c JOIN seed_names s ON s.name=c.name
    WHERE c.tenant_id=1 AND c.deleted=b'0' AND (c.project IS NULL OR c.project<>'智能排产')
  )
  AND NOT EXISTS (
    SELECT p.case_id,p.sort FROM system_codex_test_checkpoint p
    JOIN system_codex_test_case c ON c.id=p.case_id JOIN seed_names s ON s.name=c.name
    WHERE c.tenant_id=1 AND c.deleted=b'0' AND p.deleted=b'0'
    GROUP BY p.case_id,p.sort HAVING COUNT(*)>1
  )
  AND NOT EXISTS (
    SELECT 1 FROM system_codex_test_checkpoint p JOIN system_codex_test_case c ON c.id=p.case_id
    JOIN seed_names s ON s.name=c.name WHERE c.tenant_id=1 AND c.deleted=b'0' AND p.deleted=b'0'
    AND (p.tenant_id IS NULL OR p.tenant_id<>c.tenant_id)
  )
THEN 'TARGET_PREFLIGHT_PASS:20260726_system_codex_smart_scheduling_test_items'
ELSE 'TARGET_PREFLIGHT_BLOCKED:review-required-before-data-migration' END;
