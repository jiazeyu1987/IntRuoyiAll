SET SESSION group_concat_max_len = 100000000;
SET @target_user_id := 910250;
SET @target_tenant_id := 1;
SET @target_role_code := _utf8mb4'wenkong_no_download' COLLATE utf8mb4_unicode_ci;

SELECT CONCAT('USER', CHAR(9), u.id, CHAR(9), u.username, CHAR(9), u.nickname, CHAR(9), u.tenant_id, CHAR(9), u.status, CHAR(9), u.deleted + 0) AS rowdata
FROM system_users u
WHERE u.tenant_id = @target_tenant_id
  AND u.username = 'wangsiyu';

SELECT CONCAT('ACTIVE_ROLE', CHAR(9), r.id, CHAR(9), r.code, CHAR(9), r.name, CHAR(9), ur.deleted + 0) AS rowdata
FROM system_user_role ur
JOIN system_role r
  ON r.id = ur.role_id
 AND r.tenant_id = ur.tenant_id
 AND r.status = 0
 AND r.deleted = b'0'
WHERE ur.tenant_id = @target_tenant_id
  AND ur.user_id = @target_user_id
  AND ur.deleted = b'0'
ORDER BY r.code;

SELECT CONCAT('ROOT_MENU', CHAR(9), m.id, CHAR(9), m.name, CHAR(9), r.code) AS rowdata
FROM system_user_role ur
JOIN system_role r
  ON r.id = ur.role_id
 AND r.tenant_id = ur.tenant_id
 AND r.status = 0
 AND r.deleted = b'0'
JOIN system_role_menu rm
  ON rm.role_id = r.id
 AND rm.tenant_id = r.tenant_id
 AND rm.deleted = b'0'
JOIN system_menu m
  ON m.id = rm.menu_id
 AND m.status = 0
 AND m.deleted = b'0'
WHERE ur.tenant_id = @target_tenant_id
  AND ur.user_id = @target_user_id
  AND ur.deleted = b'0'
  AND m.id IN (6800, 900218, 990200)
ORDER BY m.id, r.code;

SELECT CONCAT('NO_DOWNLOAD_ROLE_SUMMARY', CHAR(9), r.id, CHAR(9), r.code,
              CHAR(9), COUNT(DISTINCT rm.menu_id),
              CHAR(9), SUM(CASE WHEN m.permission IN (
                  'dcc:controlled-file:directory:manage',
                  'dcc:controlled-file:access-rule:manage',
                  'dcc:controlled-file:category:manage',
                  'dcc:controlled-file:download'
              ) THEN 1 ELSE 0 END)) AS rowdata
FROM system_role r
JOIN system_role_menu rm
  ON rm.role_id = r.id
 AND rm.tenant_id = r.tenant_id
 AND rm.deleted = b'0'
JOIN system_menu m
  ON m.id = rm.menu_id
 AND m.deleted = b'0'
WHERE r.tenant_id = @target_tenant_id
  AND r.code = @target_role_code
  AND r.status = 0
  AND r.deleted = b'0'
GROUP BY r.id, r.code;

WITH RECURSIVE dept_chain AS (
  SELECT d.id, d.parent_id, d.name, 0 AS depth
  FROM system_users u
  JOIN system_dept d
    ON d.id = u.dept_id
   AND d.tenant_id = u.tenant_id
   AND d.deleted = b'0'
  WHERE u.tenant_id = @target_tenant_id
    AND u.id = @target_user_id
    AND u.deleted = b'0'
  UNION ALL
  SELECT parent.id, parent.parent_id, parent.name, dc.depth + 1
  FROM dept_chain dc
  JOIN system_dept parent
    ON parent.id = dc.parent_id
   AND parent.tenant_id = @target_tenant_id
   AND parent.deleted = b'0'
  WHERE dc.parent_id IS NOT NULL
    AND dc.parent_id > 0
    AND dc.depth < 20
), user_posts AS (
  SELECT post_id
  FROM system_user_post
  WHERE tenant_id = @target_tenant_id
    AND user_id = @target_user_id
    AND deleted = b'0'
), active_roles AS (
  SELECT r.id
  FROM system_user_role ur
  JOIN system_role r
    ON r.id = ur.role_id
   AND r.tenant_id = ur.tenant_id
   AND r.status = 0
   AND r.deleted = b'0'
  WHERE ur.tenant_id = @target_tenant_id
    AND ur.user_id = @target_user_id
    AND ur.deleted = b'0'
)
SELECT CONCAT('NO_DOWNLOAD_RULE_COUNTS', CHAR(9),
              (SELECT COUNT(*) FROM dcc_file_category_permission_rule c WHERE c.tenant_id = @target_tenant_id AND c.subject_type IN ('ROLE','3') AND c.subject_id IN (SELECT id FROM active_roles) AND c.action_type = 'DOWNLOAD' AND c.active = 1 AND c.deleted = 0),
              CHAR(9),
              (SELECT COUNT(*) FROM dcc_directory_access_rule d WHERE d.tenant_id = @target_tenant_id AND d.subject_type IN ('ROLE','3') AND d.subject_id IN (SELECT id FROM active_roles) AND d.can_download = 1 AND d.active = 1 AND d.deleted = 0),
              CHAR(9),
              (SELECT COUNT(*) FROM dcc_file_category_permission_rule c WHERE c.tenant_id = @target_tenant_id AND c.subject_type IN ('USER','1') AND c.subject_id = @target_user_id AND c.action_type = 'DOWNLOAD' AND c.active = 1 AND c.deleted = 0),
              CHAR(9),
              (SELECT COUNT(*) FROM dcc_directory_access_rule d WHERE d.tenant_id = @target_tenant_id AND d.subject_type IN ('USER','1') AND d.subject_id = @target_user_id AND d.can_download = 1 AND d.active = 1 AND d.deleted = 0),
              CHAR(9),
              (SELECT COUNT(*) FROM dcc_file_category_permission_rule c WHERE c.tenant_id = @target_tenant_id AND c.subject_type IN ('POSITION','POST','4') AND c.subject_id IN (SELECT post_id FROM user_posts) AND c.action_type = 'DOWNLOAD' AND c.active = 1 AND c.deleted = 0),
              CHAR(9),
              (SELECT COUNT(*) FROM dcc_directory_access_rule d WHERE d.tenant_id = @target_tenant_id AND d.subject_type IN ('POSITION','POST','4') AND d.subject_id IN (SELECT post_id FROM user_posts) AND d.can_download = 1 AND d.active = 1 AND d.deleted = 0),
              CHAR(9),
              (SELECT COUNT(*) FROM dcc_file_category_permission_rule c WHERE c.tenant_id = @target_tenant_id AND c.subject_type IN ('DEPT','2') AND c.subject_id IN (SELECT id FROM dept_chain) AND c.action_type = 'DOWNLOAD' AND c.active = 1 AND c.deleted = 0),
              CHAR(9),
              (SELECT COUNT(*) FROM dcc_directory_access_rule d WHERE d.tenant_id = @target_tenant_id AND d.subject_type IN ('DEPT','2') AND d.subject_id IN (SELECT id FROM dept_chain) AND d.can_download = 1 AND d.active = 1 AND d.deleted = 0)) AS rowdata;

SELECT CONCAT('ACTIVE_DYNAMIC_ENTITLEMENT_COUNT', CHAR(9), COUNT(*)) AS rowdata
FROM system_entitlement_grant g
WHERE g.tenant_id = @target_tenant_id
  AND g.resolved_user_id = @target_user_id
  AND g.status = _utf8mb4'ACTIVE' COLLATE utf8mb4_unicode_ci
  AND g.deleted = b'0';
