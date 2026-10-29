-- release-target-preflight: migrationId=20261026_showroom_master_data_menu; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  (NOT (NOT EXISTS (SELECT 1 FROM system_menu WHERE id=980100 AND parent_id=0
      AND path='showroom' AND type=1 AND status=0 AND deleted=b'0')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=990201 AND name='展厅主数据'
      AND component='mdm/product/index' AND component_name='MdmProduct'
      AND permission='mdm:product:query' AND type=2 AND status=0 AND deleted=b'0'
      AND ((parent_id=5101 AND path='showroom-product')
        OR (parent_id=980100 AND path='/mes/md/showroom-product')))))
  AND (NOT (EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND id<>990201
      AND path='/mes/md/showroom-product')))
  AND (NOT (EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY'))))
THEN 'TARGET_PREFLIGHT_PASS:20261026_showroom_master_data_menu' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261026_showroom_master_data_menu:BASELINE_CONFLICT' END;
