-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261118_mes_route_batch_change_bpm_seed,20261021_mes_craft_library; type=menu; riskLevel=medium
-- 默认审批角色可查看变更明细并通过既有审核入口处理本人任务；不授予业务资料修改或直接发布权限。
START TRANSACTION;
CREATE TEMPORARY TABLE batch_change_read_menus (id BIGINT PRIMARY KEY);
INSERT INTO batch_change_read_menus (id)
WITH RECURSIVE menu_tree AS (
 SELECT id,parent_id FROM system_menu WHERE permission IN ('mes:pro-craft:query','bpm:task:update') AND deleted=0
 UNION ALL
 SELECT m.id,m.parent_id FROM system_menu m JOIN menu_tree t ON m.id=t.parent_id WHERE m.deleted=0
) SELECT DISTINCT id FROM menu_tree;
INSERT INTO system_role_menu (role_id,menu_id,creator,updater,tenant_id)
SELECT r.id,m.id,'1','1',r.tenant_id FROM system_role r CROSS JOIN batch_change_read_menus m
WHERE r.code='mes_route_version_admin' AND r.deleted=0 AND r.tenant_id IN (1,122)
AND NOT EXISTS(SELECT 1 FROM system_role_menu existing WHERE existing.role_id=r.id AND existing.menu_id=m.id AND existing.tenant_id=r.tenant_id AND existing.deleted=0);
-- 目标审批角色所属租户套餐同步开放同一只读菜单链。
UPDATE system_tenant_package p
JOIN (SELECT source.id,JSON_ARRAYAGG(m.id) missing_ids FROM system_tenant_package source CROSS JOIN batch_change_read_menus m
 WHERE source.deleted=0 AND JSON_VALID(source.menu_ids)
 AND NOT JSON_CONTAINS(CAST(source.menu_ids AS JSON),CAST(m.id AS JSON),'$') GROUP BY source.id) additions ON additions.id=p.id
SET p.menu_ids=JSON_MERGE_PRESERVE(CAST(p.menu_ids AS JSON),additions.missing_ids)
WHERE p.deleted=0
AND EXISTS(SELECT 1 FROM system_tenant t JOIN system_role r ON r.tenant_id=t.id AND r.code='mes_route_version_admin' AND r.deleted=0
 WHERE t.package_id=p.id AND t.deleted=0 AND t.id IN (1,122));
DROP TEMPORARY TABLE batch_change_read_menus;
COMMIT;
