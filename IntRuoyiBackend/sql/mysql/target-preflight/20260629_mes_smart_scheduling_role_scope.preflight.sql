-- release-target-preflight: migrationId=20260629_mes_smart_scheduling_role_scope; allowedEnvironments=test,backup,prod
WITH
required_core_menus AS (
  SELECT 900120 AS menu_id UNION ALL SELECT 5590 UNION ALL SELECT 5580 UNION ALL SELECT 5550
  UNION ALL SELECT 5262 UNION ALL SELECT 5540 UNION ALL SELECT 900104 UNION ALL SELECT 5985
  UNION ALL SELECT 5551 UNION ALL SELECT 5552 UNION ALL SELECT 5553 UNION ALL SELECT 5532
  UNION ALL SELECT 5535 UNION ALL SELECT 5555 UNION ALL SELECT 5969 UNION ALL SELECT 900200
  UNION ALL SELECT 5723 UNION ALL SELECT 5730
),
old_route_menus AS (
  SELECT 900121 AS menu_id UNION ALL SELECT 900122
),
new_route_menus AS (
  SELECT 5726 AS menu_id UNION ALL SELECT 5727
)
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('system_menu','system_role','system_role_menu','system_tenant','system_tenant_package')) = 5
  AND NOT EXISTS (
    SELECT 1
    FROM required_core_menus AS required_menu
    LEFT JOIN system_menu AS menu
      ON menu.id = required_menu.menu_id
     AND menu.deleted = b'0'
     AND menu.status = 0
    WHERE menu.id IS NULL
  )
  AND (
    NOT EXISTS (
      SELECT 1
      FROM old_route_menus AS old_route_menu
      LEFT JOIN system_menu AS menu
        ON menu.id = old_route_menu.menu_id
       AND menu.deleted = b'0'
       AND menu.status = 0
      WHERE menu.id IS NULL
    )
    OR NOT EXISTS (
      SELECT 1
      FROM new_route_menus AS new_route_menu
      LEFT JOIN system_menu AS menu
        ON menu.id = new_route_menu.menu_id
       AND menu.deleted = b'0'
       AND menu.status = 0
      WHERE menu.id IS NULL
    )
  )
  AND (SELECT COUNT(*) FROM system_role WHERE code IN ('mes_scheduler','mes_team_leader','mes_workshop_director') AND deleted = b'0') >= 3
  AND NOT EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted = b'0' AND menu_ids IS NOT NULL AND NOT JSON_VALID(menu_ids))
THEN 'TARGET_PREFLIGHT_PASS:20260629_mes_smart_scheduling_role_scope' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260629_mes_smart_scheduling_role_scope' END;
