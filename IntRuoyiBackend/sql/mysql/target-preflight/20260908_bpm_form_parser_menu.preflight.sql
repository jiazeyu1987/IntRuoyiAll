-- release-target-preflight: migrationId=20260908_bpm_form_parser_menu; allowedEnvironments=test,backup,prod
-- Read-only data prerequisites; evaluated immediately before this migration.
WITH defs(id,permission) AS(SELECT 605071222,'form:parser:query' COLLATE utf8mb4_unicode_ci UNION ALL SELECT 605071223,'form:parser:production-batch-record')
SELECT CASE WHEN
  (NOT EXISTS(SELECT 1 FROM system_tenant_package p WHERE p.deleted=0 AND
 CASE WHEN p.menu_ids IS NULL OR NOT JSON_VALID(p.menu_ids) THEN TRUE
 WHEN JSON_TYPE(CAST(p.menu_ids AS JSON))<>'ARRAY' THEN TRUE
 ELSE EXISTS(SELECT 1 FROM JSON_TABLE(p.menu_ids,'$[*]' COLUMNS(n FOR ORDINALITY)) j
 WHERE JSON_TYPE(JSON_EXTRACT(p.menu_ids,CONCAT('$[',j.n-1,']')))<>'INTEGER'
 OR CAST(JSON_UNQUOTE(JSON_EXTRACT(p.menu_ids,CONCAT('$[',j.n-1,']'))) AS DECIMAL(65,0)) NOT BETWEEN 0 AND 9223372036854775807) END))
  AND (EXISTS(SELECT 1 FROM system_menu WHERE id=990200 AND deleted=0 AND type=1))
  AND (NOT EXISTS(SELECT 1 FROM system_menu WHERE deleted=0 AND parent_id=990200 AND path='form-center/parser' AND id<>605071222))
  AND (NOT EXISTS(SELECT 1 FROM system_menu WHERE deleted=0 AND permission IN('form:parser:query','form:parser:production-batch-record') AND id NOT IN(605071222,605071223)))
  AND (NOT EXISTS(SELECT 1 FROM system_menu m JOIN defs d ON d.id=m.id WHERE m.deleted<>0 OR NOT(COALESCE(m.permission,'')=d.permission)))
  AND (NOT EXISTS(SELECT 1 FROM system_role_menu rm LEFT JOIN system_role r ON r.id=rm.role_id AND r.tenant_id=rm.tenant_id AND r.deleted=0 WHERE rm.menu_id=605071201 AND rm.deleted=0 AND r.id IS NULL))
  AND (NOT EXISTS(SELECT 1 FROM system_role_menu rm JOIN system_role_menu source ON source.role_id=rm.role_id AND source.menu_id=605071201 AND source.deleted=0 WHERE rm.menu_id IN(605071222,605071223) AND rm.deleted=0 AND rm.tenant_id<>source.tenant_id))
THEN 'TARGET_PREFLIGHT_PASS:20260908_bpm_form_parser_menu' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260908_bpm_form_parser_menu' END;
