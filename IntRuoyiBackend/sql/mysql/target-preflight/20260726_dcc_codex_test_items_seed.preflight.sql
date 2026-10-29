-- release-target-preflight: migrationId=20260726_dcc_codex_test_items_seed; allowedEnvironments=test,backup,prod
WITH seed_names AS (SELECT '智能文控受控文件上传审批发布闭环' AS name UNION ALL SELECT '智能文控受控文件修订版本链闭环' AS name UNION ALL SELECT '智能文控作废审批与受控浏览收敛' AS name UNION ALL SELECT '智能文控受控浏览下载水印与访问日志' AS name UNION ALL SELECT '智能文控分发培训闭环' AS name UNION ALL SELECT '智能文控项目代码识别分配闭环' AS name)
SELECT CASE WHEN
(SELECT COUNT(*) FROM system_tenant WHERE id=1 AND deleted=b'0')=1
  AND NOT EXISTS (
    SELECT c.tenant_id,c.name FROM system_codex_test_case c JOIN seed_names s ON s.name=c.name
    WHERE c.tenant_id=1 AND c.deleted=b'0' GROUP BY c.tenant_id,c.name HAVING COUNT(*)>1
  )
  AND NOT EXISTS (
    SELECT 1 FROM system_codex_test_case c JOIN seed_names s ON s.name=c.name
    WHERE c.tenant_id=1 AND c.deleted=b'0' AND (c.project IS NULL OR c.project<>'文控')
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
THEN 'TARGET_PREFLIGHT_PASS:20260726_dcc_codex_test_items_seed'
ELSE 'TARGET_PREFLIGHT_BLOCKED:review-required-before-data-migration' END;
