-- release-target-preflight: migrationId=20260731_dcc_file_category_match_rule_seed; allowedEnvironments=test,backup,prod
-- Exact original seed category names and match texts; refuse absent/ambiguous categories and existing disabled/duplicate rules that NOT EXISTS would preserve.
WITH seed AS (
 SELECT '过程运行确认（OQ）方案' COLLATE utf8mb4_unicode_ci AS category_name,'OQ方案' COLLATE utf8mb4_0900_ai_ci AS match_text,'CONTAINS' COLLATE utf8mb4_0900_ai_ci AS match_type
 UNION ALL
 SELECT '过程运行确认（OQ）方案' COLLATE utf8mb4_unicode_ci AS category_name,'OQ验证方案' COLLATE utf8mb4_0900_ai_ci AS match_text,'CONTAINS' COLLATE utf8mb4_0900_ai_ci AS match_type
 UNION ALL
 SELECT '过程运行确认（OQ）方案' COLLATE utf8mb4_unicode_ci AS category_name,'OQPQ验证方案' COLLATE utf8mb4_0900_ai_ci AS match_text,'CONTAINS' COLLATE utf8mb4_0900_ai_ci AS match_type
 UNION ALL
 SELECT '过程运行确认（OQ）报告' COLLATE utf8mb4_unicode_ci AS category_name,'OQ报告' COLLATE utf8mb4_0900_ai_ci AS match_text,'CONTAINS' COLLATE utf8mb4_0900_ai_ci AS match_type
 UNION ALL
 SELECT '过程运行确认（OQ）报告' COLLATE utf8mb4_unicode_ci AS category_name,'OQ验证报告' COLLATE utf8mb4_0900_ai_ci AS match_text,'CONTAINS' COLLATE utf8mb4_0900_ai_ci AS match_type
 UNION ALL
 SELECT '过程运行确认（OQ）报告' COLLATE utf8mb4_unicode_ci AS category_name,'OQPQ验证报告' COLLATE utf8mb4_0900_ai_ci AS match_text,'CONTAINS' COLLATE utf8mb4_0900_ai_ci AS match_type
 UNION ALL
 SELECT '过程性能确认（PQ）方案' COLLATE utf8mb4_unicode_ci AS category_name,'PQ方案' COLLATE utf8mb4_0900_ai_ci AS match_text,'CONTAINS' COLLATE utf8mb4_0900_ai_ci AS match_type
 UNION ALL
 SELECT '过程性能确认（PQ）方案' COLLATE utf8mb4_unicode_ci AS category_name,'PQ验证方案' COLLATE utf8mb4_0900_ai_ci AS match_text,'CONTAINS' COLLATE utf8mb4_0900_ai_ci AS match_type
 UNION ALL
 SELECT '过程性能确认（PQ）方案' COLLATE utf8mb4_unicode_ci AS category_name,'性能确认方案PQ' COLLATE utf8mb4_0900_ai_ci AS match_text,'CONTAINS' COLLATE utf8mb4_0900_ai_ci AS match_type
 UNION ALL
 SELECT '过程性能确认（PQ）报告' COLLATE utf8mb4_unicode_ci AS category_name,'PQ报告' COLLATE utf8mb4_0900_ai_ci AS match_text,'CONTAINS' COLLATE utf8mb4_0900_ai_ci AS match_type
 UNION ALL
 SELECT '过程性能确认（PQ）报告' COLLATE utf8mb4_unicode_ci AS category_name,'PQ验证报告' COLLATE utf8mb4_0900_ai_ci AS match_text,'CONTAINS' COLLATE utf8mb4_0900_ai_ci AS match_type
 UNION ALL
 SELECT '过程性能确认（PQ）报告' COLLATE utf8mb4_unicode_ci AS category_name,'性能确认报告PQ' COLLATE utf8mb4_0900_ai_ci AS match_text,'CONTAINS' COLLATE utf8mb4_0900_ai_ci AS match_type
 UNION ALL
 SELECT '零配件图纸' COLLATE utf8mb4_unicode_ci AS category_name,'sldprt' COLLATE utf8mb4_0900_ai_ci AS match_text,'EXTENSION' COLLATE utf8mb4_0900_ai_ci AS match_type
 UNION ALL
 SELECT '零配件图纸' COLLATE utf8mb4_unicode_ci AS category_name,'sldasm' COLLATE utf8mb4_0900_ai_ci AS match_text,'EXTENSION' COLLATE utf8mb4_0900_ai_ci AS match_type
 UNION ALL
 SELECT '零配件图纸' COLLATE utf8mb4_unicode_ci AS category_name,'x_t' COLLATE utf8mb4_0900_ai_ci AS match_text,'EXTENSION' COLLATE utf8mb4_0900_ai_ci AS match_type
 UNION ALL
 SELECT '零配件图纸' COLLATE utf8mb4_unicode_ci AS category_name,'step' COLLATE utf8mb4_0900_ai_ci AS match_text,'EXTENSION' COLLATE utf8mb4_0900_ai_ci AS match_type
 UNION ALL
 SELECT '零配件图纸' COLLATE utf8mb4_unicode_ci AS category_name,'stp' COLLATE utf8mb4_0900_ai_ci AS match_text,'EXTENSION' COLLATE utf8mb4_0900_ai_ci AS match_type
 UNION ALL
 SELECT '零配件图纸' COLLATE utf8mb4_unicode_ci AS category_name,'零件图纸' COLLATE utf8mb4_0900_ai_ci AS match_text,'CONTAINS' COLLATE utf8mb4_0900_ai_ci AS match_type
 UNION ALL
 SELECT '零配件图纸' COLLATE utf8mb4_unicode_ci AS category_name,'组件图纸' COLLATE utf8mb4_0900_ai_ci AS match_text,'CONTAINS' COLLATE utf8mb4_0900_ai_ci AS match_type
)
SELECT CASE WHEN

  NOT EXISTS (SELECT 1 FROM (SELECT DISTINCT category_name FROM seed) s WHERE NOT EXISTS
     (SELECT 1 FROM dcc_file_category c WHERE c.name=s.category_name AND c.deleted=0 AND c.active=1))
  AND NOT EXISTS (SELECT 1 FROM dcc_file_category c JOIN (SELECT DISTINCT category_name FROM seed) s ON s.category_name=c.name
    WHERE c.deleted=0 AND c.active=1 GROUP BY c.tenant_id,c.name HAVING COUNT(*)>1)
  AND NOT EXISTS (SELECT 1 FROM seed s JOIN dcc_file_category c ON c.name=s.category_name AND c.deleted=0 AND c.active=1
    JOIN dcc_file_category_match_rule r ON r.tenant_id=c.tenant_id AND r.category_id=c.id AND r.match_text=s.match_text AND r.match_type=s.match_type AND r.deleted=0
    WHERE r.active<>1 OR r.weight<=0)
  AND NOT EXISTS (SELECT 1 FROM seed s JOIN dcc_file_category c ON c.name=s.category_name AND c.deleted=0 AND c.active=1
    JOIN dcc_file_category_match_rule r ON r.tenant_id=c.tenant_id AND r.category_id=c.id AND r.match_text=s.match_text AND r.match_type=s.match_type AND r.deleted=0
    GROUP BY c.tenant_id,c.id,s.match_text,s.match_type HAVING COUNT(*)>1)
THEN 'TARGET_PREFLIGHT_PASS:20260731_dcc_file_category_match_rule_seed' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260731_dcc_file_category_match_rule_seed' END;
