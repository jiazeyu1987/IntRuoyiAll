-- release-target-preflight: migrationId=20261112_mes_feedback_code_rule; allowedEnvironments=test,backup,prod
-- Check missing feedback rule serial capacity before project_id is introduced; migration checks the exact post-schema global-rule scope.
SELECT CASE
 WHEN (EXISTS (SELECT 1 FROM system_tenant t WHERE t.deleted=0
AND NOT EXISTS (SELECT 1 FROM mes_md_auto_code_rule r WHERE r.tenant_id=t.id AND r.code='PRO_FEEDBACK_CODE' AND r.deleted=0)
AND (SELECT COALESCE(MAX(CAST(SUBSTRING(h.code,4) AS UNSIGNED)),0)
 FROM (SELECT tenant_id,code COLLATE utf8mb4_bin code FROM mes_pro_feedback
 UNION ALL SELECT tenant_id,result COLLATE utf8mb4_bin FROM mes_md_auto_code_record) h
 WHERE h.tenant_id=t.id AND h.code REGEXP '^FB-[0-9]+$')>=2147483647)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261112_mes_feedback_code_rule:CONDITION_1'
 ELSE 'TARGET_PREFLIGHT_PASS:20261112_mes_feedback_code_rule' END;
