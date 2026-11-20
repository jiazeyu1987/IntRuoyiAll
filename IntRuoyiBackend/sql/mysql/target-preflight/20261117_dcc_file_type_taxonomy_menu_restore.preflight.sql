-- release-target-preflight: migrationId=20261117_dcc_file_type_taxonomy_menu_restore; allowedEnvironments=test,backup,prod
WITH qualified AS (
 SELECT DISTINCT r.id,r.tenant_id FROM system_role r
 JOIN system_role_menu rm ON rm.role_id=r.id AND rm.tenant_id=r.tenant_id AND rm.deleted=b'0'
 JOIN system_menu m ON m.id=rm.menu_id AND m.permission='dcc:controlled-file:category:manage' AND m.status=0 AND m.deleted=b'0'
 WHERE r.status=0 AND r.deleted=b'0'
  AND (r.tenant_id=0 OR EXISTS(SELECT 1 FROM system_tenant t WHERE t.id=r.tenant_id AND t.deleted=b'0'))
), taxonomy AS (
 SELECT id FROM system_menu WHERE component='dcc/controlled-file/basic-data/file-type-taxonomy/index' AND deleted=b'0'
)
SELECT CASE
 WHEN (SELECT COUNT(*) FROM system_menu WHERE path='/mdm' AND parent_id=0 AND status=0 AND deleted=b'0')<>1 THEN 'TARGET_PREFLIGHT_BLOCKED:20261117_dcc_file_type_taxonomy_menu_restore:ROOT_CONFLICT'
 WHEN (SELECT COUNT(*) FROM taxonomy)>1 OR EXISTS(SELECT 1 FROM taxonomy t JOIN system_menu m ON m.id=t.id WHERE t.id=990230 AND NOT(m.path='file-type-taxonomy' AND m.permission='dcc:controlled-file:category:manage' AND m.type=2 AND m.status=0 AND m.deleted=b'0')) THEN 'TARGET_PREFLIGHT_BLOCKED:20261117_dcc_file_type_taxonomy_menu_restore:LEGACY_ID_CONFLICT'
 WHEN NOT EXISTS(SELECT 1 FROM taxonomy) AND EXISTS(SELECT 1 FROM system_menu WHERE id=605072160) THEN 'TARGET_PREFLIGHT_BLOCKED:20261117_dcc_file_type_taxonomy_menu_restore:RESERVED_ID_OCCUPIED'
 WHEN EXISTS(SELECT 1 FROM system_menu WHERE parent_id=(SELECT MAX(id) FROM system_menu WHERE path='/mdm' AND parent_id=0 AND status=0 AND deleted=b'0') AND path='file-type-taxonomy' AND id<>COALESCE((SELECT MAX(id) FROM taxonomy),605072160) AND deleted=b'0') THEN 'TARGET_PREFLIGHT_BLOCKED:20261117_dcc_file_type_taxonomy_menu_restore:ROUTE_OCCUPIED'
 WHEN NOT EXISTS(SELECT 1 FROM qualified) THEN 'TARGET_PREFLIGHT_BLOCKED:20261117_dcc_file_type_taxonomy_menu_restore:NO_EXISTING_AUTHORIZATION'
 WHEN EXISTS(SELECT 1 FROM qualified q LEFT JOIN system_tenant t ON t.id=q.tenant_id AND t.deleted=b'0' LEFT JOIN system_tenant_package p ON p.id=t.package_id AND p.deleted=b'0' WHERE q.tenant_id<>0 AND (t.id IS NULL OR (t.package_id<>0 AND (p.id IS NULL OR p.menu_ids IS NULL OR JSON_TYPE(p.menu_ids)<>'ARRAY')))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261117_dcc_file_type_taxonomy_menu_restore:TENANT_PACKAGE_CONFLICT'
 ELSE 'TARGET_PREFLIGHT_PASS:20261117_dcc_file_type_taxonomy_menu_restore' END;
