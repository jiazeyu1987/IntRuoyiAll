-- release-target-preflight: migrationId=20260919_mes_edhr_live_label_print_menu_removal; allowedEnvironments=test,backup,prod
-- Read-only data prerequisites; evaluated immediately before this migration.
WITH defs(id,permission,type,parent_id) AS(SELECT 900320,'mes:pro-edhr-label:query' COLLATE utf8mb4_unicode_ci,2,900220 UNION ALL SELECT 900321,'mes:pro-edhr-label-template:query' COLLATE utf8mb4_unicode_ci,3,900320 UNION ALL SELECT 900322,'mes:pro-edhr-label-template:create' COLLATE utf8mb4_unicode_ci,3,900320 UNION ALL SELECT 900323,'mes:pro-edhr-label-template:activate' COLLATE utf8mb4_unicode_ci,3,900320 UNION ALL SELECT 900324,'mes:pro-edhr-label:query' COLLATE utf8mb4_unicode_ci,3,900320 UNION ALL SELECT 900325,'mes:pro-edhr-label:preview' COLLATE utf8mb4_unicode_ci,3,900320 UNION ALL SELECT 900326,'mes:pro-edhr-print-task:query' COLLATE utf8mb4_unicode_ci,2,900220 UNION ALL SELECT 900327,'mes:pro-edhr-print-task:query' COLLATE utf8mb4_unicode_ci,3,900326 UNION ALL SELECT 900328,'mes:pro-edhr-print-task:create' COLLATE utf8mb4_unicode_ci,3,900326 UNION ALL SELECT 900329,'mes:pro-edhr-print-task:mark-failed' COLLATE utf8mb4_unicode_ci,3,900326 UNION ALL SELECT 900330,'mes:pro-edhr-print-task:confirm' COLLATE utf8mb4_unicode_ci,3,900326 UNION ALL SELECT 900331,'mes:pro-edhr-print-task:query' COLLATE utf8mb4_unicode_ci,3,900326)
SELECT CASE WHEN
  (NOT EXISTS(SELECT 1 FROM system_tenant_package p WHERE p.deleted=0 AND
 CASE WHEN p.menu_ids IS NULL OR NOT JSON_VALID(p.menu_ids) THEN TRUE
 WHEN JSON_TYPE(CAST(p.menu_ids AS JSON))<>'ARRAY' THEN TRUE
 ELSE EXISTS(SELECT 1 FROM JSON_TABLE(p.menu_ids,'$[*]' COLUMNS(n FOR ORDINALITY)) j
 WHERE JSON_TYPE(JSON_EXTRACT(p.menu_ids,CONCAT('$[',j.n-1,']')))<>'INTEGER'
 OR CAST(JSON_UNQUOTE(JSON_EXTRACT(p.menu_ids,CONCAT('$[',j.n-1,']'))) AS DECIMAL(65,0)) NOT BETWEEN 0 AND 9223372036854775807) END))
  AND (EXISTS(SELECT 1 FROM system_menu WHERE id=900220 AND deleted=0))
  AND (NOT EXISTS(SELECT 1 FROM defs d LEFT JOIN system_menu m ON m.id=d.id AND BINARY m.permission=BINARY d.permission AND m.type=d.type AND m.parent_id=d.parent_id WHERE m.id IS NULL))
  AND ((SELECT COUNT(*) FROM system_menu WHERE id BETWEEN 900320 AND 900331 AND deleted=0) IN(0,12))
  AND (NOT EXISTS(SELECT 1 FROM system_menu WHERE deleted=0 AND id NOT BETWEEN 900320 AND 900331 AND parent_id BETWEEN 900320 AND 900331))
THEN 'TARGET_PREFLIGHT_PASS:20260919_mes_edhr_live_label_print_menu_removal' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260919_mes_edhr_live_label_print_menu_removal' END;
