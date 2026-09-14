SELECT JSON_ARRAYAGG(JSON_OBJECT('username', username, 'roleCode', role_code, 'permission', permission))
FROM (
  SELECT DISTINCT u.username, r.code AS role_code, m.permission
  FROM system_user_role ur
  JOIN system_users u ON u.id = ur.user_id AND u.tenant_id = ur.tenant_id AND u.deleted = b'0' AND u.status = 0
  JOIN system_role r ON r.id = ur.role_id AND r.tenant_id = ur.tenant_id AND r.deleted = b'0' AND r.status = 0
  JOIN system_role_menu rm ON rm.role_id = r.id AND rm.tenant_id = r.tenant_id AND rm.deleted = b'0'
  JOIN system_menu m ON m.id = rm.menu_id AND m.deleted = b'0' AND m.status = 0
  WHERE ur.tenant_id = 1
    AND ur.deleted = b'0'
    AND u.username IN ('wangsiyu', 'zhaohaichen')
    AND m.permission IN ('dcc:controlled-file:directory:manage','dcc:controlled-file:category:manage','dcc:controlled-file:download')
  ORDER BY u.username, r.code, m.permission
) detail;
