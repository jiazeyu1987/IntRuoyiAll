SELECT CONCAT(
    'USER_ROLE', CHAR(9), u.id, CHAR(9), r.id, CHAR(9), r.code,
    CHAR(9), r.name, CHAR(9), ur.deleted + 0
) AS rowdata
FROM system_users u
JOIN system_user_role ur
  ON ur.user_id = u.id
 AND ur.tenant_id = u.tenant_id
JOIN system_role r
  ON r.id = ur.role_id
 AND r.tenant_id = u.tenant_id
WHERE u.tenant_id = 1
  AND u.username = 'zhaohaichen'
ORDER BY ur.deleted, r.code, ur.id;

SELECT CONCAT(
    'ROOT_MENU', CHAR(9), m.id, CHAR(9), m.name, CHAR(9), r.code
) AS rowdata
FROM system_users u
JOIN system_user_role ur
  ON ur.user_id = u.id
 AND ur.tenant_id = u.tenant_id
 AND ur.deleted = b'0'
JOIN system_role r
  ON r.id = ur.role_id
 AND r.tenant_id = u.tenant_id
 AND r.status = 0
 AND r.deleted = b'0'
JOIN system_role_menu rm
  ON rm.role_id = r.id
 AND rm.tenant_id = u.tenant_id
 AND rm.deleted = b'0'
JOIN system_menu m
  ON m.id = rm.menu_id
 AND m.status = 0
 AND m.deleted = b'0'
WHERE u.tenant_id = 1
  AND u.username = 'zhaohaichen'
  AND u.deleted = b'0'
  AND m.id IN (6800, 900218, 990200)
ORDER BY m.id;

SELECT CONCAT(
    'NO_DOWNLOAD_ROLE_SUMMARY', CHAR(9), r.id, CHAR(9), r.code,
    CHAR(9), COUNT(DISTINCT rm.menu_id),
    CHAR(9), SUM(CASE WHEN m.permission IN (
        'dcc:controlled-file:directory:manage',
        'dcc:controlled-file:access-rule:manage',
        'dcc:controlled-file:category:manage',
        'dcc:controlled-file:download'
    ) THEN 1 ELSE 0 END)
) AS rowdata
FROM system_role r
JOIN system_role_menu rm
  ON rm.role_id = r.id
 AND rm.tenant_id = r.tenant_id
 AND rm.deleted = b'0'
JOIN system_menu m
  ON m.id = rm.menu_id
 AND m.deleted = b'0'
WHERE r.tenant_id = 1
  AND r.code = 'wenkong_no_download'
  AND r.status = 0
  AND r.deleted = b'0'
GROUP BY r.id, r.code;

SELECT CONCAT(
    'NO_DOWNLOAD_RULE_COUNTS', CHAR(9),
    (SELECT COUNT(*)
       FROM dcc_file_category_permission_rule c
      WHERE c.tenant_id = 1
        AND c.subject_type = 'ROLE'
        AND c.subject_id = r.id
        AND c.action_type = 'DOWNLOAD'
        AND c.active = 1
        AND c.deleted = 0),
    CHAR(9),
    (SELECT COUNT(*)
       FROM dcc_directory_access_rule d
      WHERE d.tenant_id = 1
        AND d.subject_type = 'ROLE'
        AND d.subject_id = r.id
        AND d.can_download = 1
        AND d.active = 1
        AND d.deleted = 0)
) AS rowdata
FROM system_role r
WHERE r.tenant_id = 1
  AND r.code = 'wenkong_no_download'
  AND r.status = 0
  AND r.deleted = b'0';

SELECT CONCAT(
    'DANGEROUS_ACTIVE_ROLE_COUNT', CHAR(9), COUNT(*)
) AS rowdata
FROM system_users u
JOIN system_user_role ur
  ON ur.user_id = u.id
 AND ur.tenant_id = u.tenant_id
 AND ur.deleted = b'0'
JOIN system_role r
  ON r.id = ur.role_id
 AND r.tenant_id = u.tenant_id
 AND r.deleted = b'0'
 AND r.status = 0
WHERE u.tenant_id = 1
  AND u.username = 'zhaohaichen'
  AND u.deleted = b'0'
  AND r.code IN ('doc_control', 'wenkong');

SELECT CONCAT(
    'ACTIVE_DYNAMIC_ENTITLEMENT_COUNT', CHAR(9), COUNT(*)
) AS rowdata
FROM system_entitlement_grant g
JOIN system_users u
  ON u.id = g.resolved_user_id
 AND u.tenant_id = g.tenant_id
WHERE g.tenant_id = 1
  AND u.username = 'zhaohaichen'
  AND g.status = 'ACTIVE'
  AND g.deleted = b'0';

