-- release-target-preflight: migrationId=20261116_erp_data_operation_roles; allowedEnvironments=test,backup,prod
-- Same pre-write identity and scope guards as the migration, read-only.
WITH tmp_erp_data_roles AS (SELECT 2565 AS page_id,605073600 AS permission_id,'物料列表' AS name,'erp_data_product_operator' AS code,'erp:data-product:operate' AS permission,1 AS sort UNION ALL SELECT 2590,605073601,'即时库存汇总','erp_data_stock_operator','erp:data-stock:operate',2 UNION ALL SELECT 6023,605073602,'物料清单','erp_data_bom_operator','erp:data-bom:operate',3 UNION ALL SELECT 6021,605073603,'生产用料清单','erp_data_material_operator','erp:data-material:operate',4 UNION ALL SELECT 6032,605073604,'生产领料单','erp_data_pick_operator','erp:data-pick:operate',5 UNION ALL SELECT (SELECT id FROM system_menu WHERE component='erp/production/replenishment-list/index' AND parent_id=991511 AND type=2 AND deleted=0 AND status=0),605073605,'生产补料单','erp_data_replenishment_operator','erp:data-replenishment:operate',6 UNION ALL SELECT 6025,605073606,'即时库存','erp_data_inventory_operator','erp:data-inventory:operate',7 UNION ALL SELECT 5530,605073607,'生产订单','erp_data_order_operator','erp:data-order:operate',8 UNION ALL SELECT 6013,605073608,'ERP配置','erp_data_config_operator','erp:data-config:operate',9),
tmp_erp_data_tenants AS (SELECT DISTINCT r.tenant_id FROM system_role r JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 WHERE r.deleted=0 AND r.code IN('tenant_admin','super_admin','mes_workshop_director','mes_team_leader','mes_scheduler'))
SELECT CASE
 WHEN (SELECT COUNT(*) FROM system_menu WHERE component='erp/production/replenishment-list/index' AND parent_id=991511 AND type=2 AND deleted=0 AND status=0)<>1 THEN 'TARGET_PREFLIGHT_BLOCKED:20261116_erp_data_operation_roles:REPLENISHMENT_IDENTITY'
 WHEN (NOT EXISTS(SELECT 1 FROM system_menu WHERE id=991511 AND path='/mes/edhr-data-sync' AND deleted=0 AND status=0)
 OR EXISTS(SELECT 1 FROM tmp_erp_data_roles n LEFT JOIN system_menu m ON m.id=n.page_id
           WHERE m.id IS NULL OR m.deleted<>0 OR m.status<>0 OR m.parent_id<>991511 OR m.type<>2)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261116_erp_data_operation_roles:CONDITION_1'
 WHEN (EXISTS(SELECT 1 FROM system_menu m JOIN tmp_erp_data_roles n ON m.id=n.permission_id OR m.permission=n.permission
           WHERE m.id<>n.permission_id OR m.permission<>n.permission OR m.parent_id<>n.page_id OR m.type<>3 OR m.deleted<>0 OR m.status<>0)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261116_erp_data_operation_roles:CONDITION_2'
 WHEN (EXISTS(SELECT 1 FROM system_tenant_package WHERE deleted=0 AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY'))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261116_erp_data_operation_roles:CONDITION_3'
 WHEN (NOT EXISTS(SELECT 1 FROM system_menu WHERE id=991502 AND parent_id=991511 AND component='erp/sync/config/index' AND deleted=0)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261116_erp_data_operation_roles:CONDITION_4'
 WHEN (NOT EXISTS(SELECT 1 FROM tmp_erp_data_tenants)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261116_erp_data_operation_roles:CONDITION_5'
 WHEN (EXISTS(SELECT 1 FROM system_role_category c JOIN tmp_erp_data_tenants t ON t.tenant_id=c.tenant_id
           WHERE c.code='erp_data' AND (c.deleted<>0 OR c.status<>0 OR c.name<>'ERP数据'))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261116_erp_data_operation_roles:CONDITION_6'
 WHEN (EXISTS(SELECT 1 FROM system_role r JOIN tmp_erp_data_roles n ON r.code=n.code JOIN tmp_erp_data_tenants t ON t.tenant_id=r.tenant_id
           WHERE r.deleted<>0 OR r.status<>0 OR COALESCE(r.remark,'')<>'ERP data operation roles 20261116')) THEN 'TARGET_PREFLIGHT_BLOCKED:20261116_erp_data_operation_roles:CONDITION_7'
 ELSE 'TARGET_PREFLIGHT_PASS:20261116_erp_data_operation_roles' END;
