-- release-target-preflight: migrationId=20260901_mes_pqc_leader_nonconformance_review_permission; allowedEnvironments=test,backup,prod
-- Match the immutable grant's identities and rejection conditions before any grant DML.
WITH expected_menu AS (
  SELECT 9008301 AS menu_id, 'mes:pro-edhr-nonconformance-review:query' AS permission
  UNION ALL SELECT 9008302, 'mes:pro-edhr-nonconformance-review:create'
), active_roles AS (
  SELECT id, tenant_id FROM system_role
  WHERE code = 'pqc_leader_permission' AND status = 0 AND deleted = b'0'
)
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('system_menu','system_role','system_role_menu')) = 3
  AND NOT EXISTS (
    SELECT 1 FROM expected_menu expected
    LEFT JOIN system_menu menu ON menu.id = expected.menu_id
      AND menu.permission = expected.permission AND menu.status = 0 AND menu.deleted = b'0'
    WHERE menu.id IS NULL
  )
  AND NOT EXISTS (
    SELECT 1 FROM expected_menu expected
    JOIN system_menu menu ON menu.permission = expected.permission
      AND menu.status = 0 AND menu.deleted = b'0'
    GROUP BY expected.permission HAVING COUNT(*) > 2
  )
  AND (SELECT COUNT(*) FROM system_menu
    WHERE permission = 'mes:pro-edhr-nonconformance-review:create'
      AND status = 0 AND deleted = b'0') <= 1
  AND EXISTS (SELECT 1 FROM active_roles)
  AND NOT EXISTS (SELECT 1 FROM active_roles GROUP BY tenant_id HAVING COUNT(*) > 1)
  -- The grant never removes dispose permission; its final check rejects an existing grant.
  AND NOT EXISTS (
    SELECT 1 FROM active_roles role
    JOIN system_role_menu role_menu ON role_menu.role_id = role.id
      AND role_menu.tenant_id = role.tenant_id AND role_menu.deleted = b'0'
    JOIN system_menu menu ON menu.id = role_menu.menu_id AND menu.deleted = b'0'
      AND menu.permission = 'mes:pro-edhr-nonconformance-review:dispose'
  )
  -- An existing row in another tenant blocks INSERT, but cannot be repaired by its UPDATE.
  AND NOT EXISTS (
    SELECT 1 FROM active_roles role CROSS JOIN expected_menu expected
    WHERE EXISTS (SELECT 1 FROM system_role_menu occupied
      WHERE occupied.role_id = role.id AND occupied.menu_id = expected.menu_id)
      AND NOT EXISTS (SELECT 1 FROM system_role_menu repairable
        WHERE repairable.role_id = role.id AND repairable.menu_id = expected.menu_id
          AND repairable.tenant_id = role.tenant_id)
  )
THEN 'TARGET_PREFLIGHT_PASS:20260901_mes_pqc_leader_nonconformance_review_permission' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260901_mes_pqc_leader_nonconformance_review_permission' END;
