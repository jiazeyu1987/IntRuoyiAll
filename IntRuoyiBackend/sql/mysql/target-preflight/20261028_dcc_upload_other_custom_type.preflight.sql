-- release-target-preflight: migrationId=20261028_dcc_upload_other_custom_type; allowedEnvironments=test,backup,prod
-- Inspect the original mapping and reserved taxonomy codes without creating the migration's temporary tables.
WITH RECURSIVE target AS (
 SELECT c.id category_id,c.tenant_id,
 CASE WHEN EXISTS (SELECT 1 FROM dcc_file_category other WHERE other.tenant_id=c.tenant_id
   AND other.file_type_taxonomy_id=c.file_type_taxonomy_id AND other.id<>c.id AND other.active=1 AND other.deleted=0)
 THEN NULL ELSE c.file_type_taxonomy_id END file_type_taxonomy_id
 FROM dcc_file_category c JOIN system_tenant t ON t.id=c.tenant_id AND t.status=0 AND t.deleted=0
 WHERE c.name='其他' AND c.active=1 AND c.deleted=0
), configured_path AS (
 SELECT c.category_id,c.tenant_id,f.id,f.parent_id,f.level_no,f.level_no leaf_level,f.active,f.deleted,1 depth
 FROM target c JOIN dcc_file_type_taxonomy f ON f.id=c.file_type_taxonomy_id AND f.tenant_id=c.tenant_id
 UNION ALL
 SELECT path.category_id,path.tenant_id,p.id,p.parent_id,p.level_no,path.leaf_level,p.active,p.deleted,path.depth+1
 FROM configured_path path JOIN dcc_file_type_taxonomy p ON p.id=path.parent_id AND p.tenant_id=path.tenant_id
 WHERE path.parent_id<>0 AND path.depth<6
)
SELECT CASE
 WHEN NOT EXISTS (SELECT 1 FROM target) THEN 'TARGET_PREFLIGHT_BLOCKED:20261028_dcc_upload_other_custom_type:MISSING_CATEGORY'
 WHEN EXISTS (SELECT tenant_id FROM target GROUP BY tenant_id HAVING COUNT(*)>1)
 THEN 'TARGET_PREFLIGHT_BLOCKED:20261028_dcc_upload_other_custom_type:AMBIGUOUS_CATEGORY'
 WHEN EXISTS (SELECT 1 FROM target c LEFT JOIN dcc_file_type_taxonomy f
 ON f.id=c.file_type_taxonomy_id AND f.tenant_id=c.tenant_id AND f.active=1 AND f.deleted=0
 WHERE c.file_type_taxonomy_id IS NOT NULL AND (f.id IS NULL OR f.level_no NOT BETWEEN 3 AND 5
 OR EXISTS (SELECT 1 FROM dcc_file_type_taxonomy child WHERE child.tenant_id=c.tenant_id AND child.parent_id=f.id AND child.deleted=0)))
 THEN 'TARGET_PREFLIGHT_BLOCKED:20261028_dcc_upload_other_custom_type:INVALID_LEAF'
 WHEN EXISTS (SELECT category_id FROM configured_path GROUP BY category_id
 HAVING MAX(parent_id=0)<>1 OR MAX(depth) NOT BETWEEN 3 AND 5 OR MAX(depth)<>MAX(leaf_level)
 OR SUM(active<>1 OR deleted<>0 OR level_no+depth<>leaf_level+1)>0)
 THEN 'TARGET_PREFLIGHT_BLOCKED:20261028_dcc_upload_other_custom_type:INVALID_PATH'
 WHEN EXISTS (SELECT f.tenant_id,f.code FROM dcc_file_type_taxonomy f JOIN target c ON c.tenant_id=f.tenant_id
 WHERE c.file_type_taxonomy_id IS NULL AND f.deleted=0
 AND f.code IN ('DCC_UPLOAD_OTHER_ROOT','DCC_UPLOAD_OTHER_GROUP','DCC_UPLOAD_OTHER_LEAF')
 GROUP BY f.tenant_id,f.code HAVING COUNT(*)>1)
 THEN 'TARGET_PREFLIGHT_BLOCKED:20261028_dcc_upload_other_custom_type:DUPLICATE_RESERVED_CODE'
 WHEN EXISTS (SELECT 1 FROM target c
 LEFT JOIN dcc_file_type_taxonomy root ON root.tenant_id=c.tenant_id AND root.code='DCC_UPLOAD_OTHER_ROOT' AND root.deleted=0
 LEFT JOIN dcc_file_type_taxonomy grp ON grp.tenant_id=c.tenant_id AND grp.code='DCC_UPLOAD_OTHER_GROUP' AND grp.deleted=0
 LEFT JOIN dcc_file_type_taxonomy leaf ON leaf.tenant_id=c.tenant_id AND leaf.code='DCC_UPLOAD_OTHER_LEAF' AND leaf.deleted=0
 WHERE c.file_type_taxonomy_id IS NULL AND (
 (root.id IS NOT NULL AND (root.parent_id<>0 OR root.level_no<>1 OR root.name<>'通用文件' OR root.active<>1))
 OR (grp.id IS NOT NULL AND (root.id IS NULL OR grp.parent_id<>root.id OR grp.level_no<>2 OR grp.name<>'其他类型' OR grp.active<>1))
 OR (leaf.id IS NOT NULL AND (grp.id IS NULL OR leaf.parent_id<>grp.id OR leaf.level_no<>3 OR leaf.name<>'其他' OR leaf.active<>1
 OR EXISTS (SELECT 1 FROM dcc_file_type_taxonomy child WHERE child.tenant_id=c.tenant_id AND child.parent_id=leaf.id AND child.deleted=0)
 OR EXISTS (SELECT 1 FROM dcc_file_category other WHERE other.tenant_id=c.tenant_id AND other.file_type_taxonomy_id=leaf.id
 AND other.id<>c.category_id AND other.active=1 AND other.deleted=0)))))
 THEN 'TARGET_PREFLIGHT_BLOCKED:20261028_dcc_upload_other_custom_type:RESERVED_CODE_CONFLICT'
 ELSE 'TARGET_PREFLIGHT_PASS:20261028_dcc_upload_other_custom_type' END;
