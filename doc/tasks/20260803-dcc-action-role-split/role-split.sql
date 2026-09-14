-- DCC action role split for local tenant 1 / category 906104.
-- Non-destructive and idempotent: creates independent roles and additive bindings only.
-- Keep session string literals aligned with IntRuoyi varchar columns.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

START TRANSACTION;

SET @tenant_id := 1;
SET @category_id := 906104;
SET @viewer_user := 'wangsiyu';

SET @role_view_code := 'dcc_action_view_independent';
SET @role_download_code := 'dcc_action_download_independent';
SET @role_training_code := 'dcc_action_training_independent';
SET @role_distribute_code := 'dcc_action_distribute_independent';

SELECT COUNT(*) INTO @category_exists
FROM dcc_file_category
WHERE id = @category_id AND tenant_id = @tenant_id AND deleted = 0 AND active = 1;

SELECT COUNT(*) INTO @view_menu_count
FROM system_menu
WHERE deleted = b'0'
  AND status = 0
  AND permission = 'dcc:controlled-file:query'
  AND path = 'controlled-file/browser';

SELECT COUNT(*) INTO @preview_menu_count
FROM system_menu
WHERE deleted = b'0'
  AND status = 0
  AND permission = 'dcc:controlled-file:preview';

SELECT COUNT(*) INTO @download_menu_count
FROM system_menu
WHERE deleted = b'0'
  AND status = 0
  AND permission = 'dcc:controlled-file:download';

SELECT COUNT(*) INTO @training_menu_count
FROM system_menu
WHERE deleted = b'0'
  AND status = 0
  AND permission = 'dcc:controlled-file:training:mine';

SELECT COUNT(*) INTO @viewer_user_count
FROM system_users
WHERE tenant_id = @tenant_id AND deleted = b'0' AND username = @viewer_user;

SELECT COUNT(*) INTO @training_user_count
FROM system_users
WHERE tenant_id = @tenant_id
  AND deleted = b'0'
  AND username IN (
    'chenchen',
    'sunrongrong',
    'liuru',
    'zhaojie',
    'xuejianxia',
    'tengweihua',
    'shihaisong',
    'malingling',
    'zhaomingyu'
  );

SET @precheck_error := CASE
  WHEN @category_exists <> 1 THEN 'DCC_ROLE_SPLIT_BLOCKED: category 906104 missing or inactive in tenant 1'
  WHEN @view_menu_count <> 1 THEN 'DCC_ROLE_SPLIT_BLOCKED: DCC controlled-file/browser view menu missing or duplicated'
  WHEN @preview_menu_count <> 1 THEN 'DCC_ROLE_SPLIT_BLOCKED: DCC preview permission missing or duplicated'
  WHEN @download_menu_count <> 1 THEN 'DCC_ROLE_SPLIT_BLOCKED: DCC download permission missing or duplicated'
  WHEN @training_menu_count <> 1 THEN 'DCC_ROLE_SPLIT_BLOCKED: DCC training mine permission missing or duplicated'
  WHEN @viewer_user_count <> 1 THEN 'DCC_ROLE_SPLIT_BLOCKED: wangsiyu missing in tenant 1'
  WHEN @training_user_count <> 9 THEN 'DCC_ROLE_SPLIT_BLOCKED: expected 9 training users missing in tenant 1'
  ELSE NULL
END;

SELECT IFNULL(@precheck_error, 'OK') AS precheck_status;

CREATE TEMPORARY TABLE tmp_dcc_role_split_precheck_guard (
  fail_flag TINYINT NOT NULL,
  message VARCHAR(255) COLLATE utf8mb4_unicode_ci NULL,
  CONSTRAINT chk_tmp_dcc_role_split_precheck_guard CHECK (fail_flag = 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_dcc_role_split_precheck_guard (fail_flag, message)
SELECT 1, @precheck_error
WHERE @precheck_error IS NOT NULL;

DROP TEMPORARY TABLE tmp_dcc_role_split_precheck_guard;

INSERT INTO system_role
  (name, code, sort, category_id, data_scope, data_scope_dept_ids, status, type, remark,
   creator, updater, deleted, tenant_id)
SELECT 'DCC Action View', @role_view_code, 7101, NULL, 1, '', 0, 2,
       'Independent DCC view/preview role', 'codex', 'codex', b'0', @tenant_id
WHERE NOT EXISTS (
  SELECT 1 FROM system_role WHERE tenant_id = @tenant_id AND code = @role_view_code AND deleted = b'0'
);

INSERT INTO system_role
  (name, code, sort, category_id, data_scope, data_scope_dept_ids, status, type, remark,
   creator, updater, deleted, tenant_id)
SELECT 'DCC Action Download', @role_download_code, 7102, NULL, 1, '', 0, 2,
       'Independent DCC download role', 'codex', 'codex', b'0', @tenant_id
WHERE NOT EXISTS (
  SELECT 1 FROM system_role WHERE tenant_id = @tenant_id AND code = @role_download_code AND deleted = b'0'
);

INSERT INTO system_role
  (name, code, sort, category_id, data_scope, data_scope_dept_ids, status, type, remark,
   creator, updater, deleted, tenant_id)
SELECT 'DCC Action Training', @role_training_code, 7103, NULL, 1, '', 0, 2,
       'Independent DCC training/read-confirm role', 'codex', 'codex', b'0', @tenant_id
WHERE NOT EXISTS (
  SELECT 1 FROM system_role WHERE tenant_id = @tenant_id AND code = @role_training_code AND deleted = b'0'
);

INSERT INTO system_role
  (name, code, sort, category_id, data_scope, data_scope_dept_ids, status, type, remark,
   creator, updater, deleted, tenant_id)
SELECT 'DCC Action Distribute', @role_distribute_code, 7104, NULL, 1, '', 0, 2,
       'Independent DCC formal-distribute role', 'codex', 'codex', b'0', @tenant_id
WHERE NOT EXISTS (
  SELECT 1 FROM system_role WHERE tenant_id = @tenant_id AND code = @role_distribute_code AND deleted = b'0'
);

UPDATE system_role
SET name = CASE code
    WHEN @role_view_code THEN 'DCC Action View'
    WHEN @role_download_code THEN 'DCC Action Download'
    WHEN @role_training_code THEN 'DCC Action Training'
    WHEN @role_distribute_code THEN 'DCC Action Distribute'
    ELSE name
  END,
  status = 0,
  type = 2,
  updater = 'codex',
  update_time = NOW()
WHERE tenant_id = @tenant_id
  AND deleted = b'0'
  AND code IN (@role_view_code, @role_download_code, @role_training_code, @role_distribute_code);

SELECT id INTO @role_view_id
FROM system_role
WHERE tenant_id = @tenant_id AND code = @role_view_code AND deleted = b'0'
ORDER BY id LIMIT 1;

SELECT id INTO @role_download_id
FROM system_role
WHERE tenant_id = @tenant_id AND code = @role_download_code AND deleted = b'0'
ORDER BY id LIMIT 1;

SELECT id INTO @role_training_id
FROM system_role
WHERE tenant_id = @tenant_id AND code = @role_training_code AND deleted = b'0'
ORDER BY id LIMIT 1;

SELECT id INTO @role_distribute_id
FROM system_role
WHERE tenant_id = @tenant_id AND code = @role_distribute_code AND deleted = b'0'
ORDER BY id LIMIT 1;

INSERT INTO system_role_menu
  (role_id, menu_id, creator, updater, deleted, tenant_id)
SELECT @role_view_id, m.id, 'codex', 'codex', b'0', @tenant_id
FROM system_menu m
WHERE m.deleted = b'0'
  AND m.status = 0
  AND (
    (m.permission = 'dcc:controlled-file:query' AND m.path = 'controlled-file/browser')
    OR m.permission = 'dcc:controlled-file:preview'
  )
  AND NOT EXISTS (
    SELECT 1 FROM system_role_menu rm
    WHERE rm.role_id = @role_view_id AND rm.menu_id = m.id AND rm.deleted = b'0'
  );

INSERT INTO system_role_menu
  (role_id, menu_id, creator, updater, deleted, tenant_id)
SELECT @role_download_id, m.id, 'codex', 'codex', b'0', @tenant_id
FROM system_menu m
WHERE m.deleted = b'0'
  AND m.status = 0
  AND m.permission = 'dcc:controlled-file:download'
  AND NOT EXISTS (
    SELECT 1 FROM system_role_menu rm
    WHERE rm.role_id = @role_download_id AND rm.menu_id = m.id AND rm.deleted = b'0'
  );

INSERT INTO system_role_menu
  (role_id, menu_id, creator, updater, deleted, tenant_id)
SELECT @role_training_id, m.id, 'codex', 'codex', b'0', @tenant_id
FROM system_menu m
WHERE m.deleted = b'0'
  AND m.status = 0
  AND m.permission = 'dcc:controlled-file:training:mine'
  AND NOT EXISTS (
    SELECT 1 FROM system_role_menu rm
    WHERE rm.role_id = @role_training_id AND rm.menu_id = m.id AND rm.deleted = b'0'
  );

INSERT INTO dcc_file_category_permission_rule
  (category_id, action_type, subject_type, subject_id, active, remark, tenant_id,
   create_time, update_time, creator, updater, deleted, scope_type)
SELECT @category_id, 'VIEW', 'ROLE', @role_view_id, 1,
       'Independent DCC view role', @tenant_id, NOW(), NOW(), 'codex', 'codex', 0, 'GLOBAL'
WHERE NOT EXISTS (
  SELECT 1 FROM dcc_file_category_permission_rule
  WHERE tenant_id = @tenant_id AND category_id = @category_id AND action_type = 'VIEW'
    AND subject_type = 'ROLE' AND subject_id = @role_view_id AND deleted = 0
);

INSERT INTO dcc_file_category_permission_rule
  (category_id, action_type, subject_type, subject_id, active, remark, tenant_id,
   create_time, update_time, creator, updater, deleted, scope_type)
SELECT @category_id, 'DOWNLOAD', 'ROLE', @role_download_id, 1,
       'Independent DCC download role', @tenant_id, NOW(), NOW(), 'codex', 'codex', 0, 'GLOBAL'
WHERE NOT EXISTS (
  SELECT 1 FROM dcc_file_category_permission_rule
  WHERE tenant_id = @tenant_id AND category_id = @category_id AND action_type = 'DOWNLOAD'
    AND subject_type = 'ROLE' AND subject_id = @role_download_id AND deleted = 0
);

INSERT INTO dcc_file_category_permission_rule
  (category_id, action_type, subject_type, subject_id, active, remark, tenant_id,
   create_time, update_time, creator, updater, deleted, scope_type)
SELECT @category_id, 'DISTRIBUTE', 'ROLE', @role_distribute_id, 1,
       'Independent DCC distribute role', @tenant_id, NOW(), NOW(), 'codex', 'codex', 0, 'GLOBAL'
WHERE NOT EXISTS (
  SELECT 1 FROM dcc_file_category_permission_rule
  WHERE tenant_id = @tenant_id AND category_id = @category_id AND action_type = 'DISTRIBUTE'
    AND subject_type = 'ROLE' AND subject_id = @role_distribute_id AND deleted = 0
);

UPDATE dcc_file_category_permission_rule
SET active = 1,
    updater = 'codex',
    update_time = NOW()
WHERE tenant_id = @tenant_id
  AND category_id = @category_id
  AND deleted = 0
  AND subject_type = 'ROLE'
  AND (
    (subject_id = @role_view_id AND action_type = 'VIEW')
    OR (subject_id = @role_download_id AND action_type = 'DOWNLOAD')
    OR (subject_id = @role_distribute_id AND action_type = 'DISTRIBUTE')
  );

INSERT INTO system_user_role
  (user_id, role_id, creator, updater, deleted, tenant_id)
SELECT u.id, @role_view_id, 'codex', 'codex', b'0', @tenant_id
FROM system_users u
WHERE u.tenant_id = @tenant_id
  AND u.deleted = b'0'
  AND u.username = @viewer_user
  AND NOT EXISTS (
    SELECT 1 FROM system_user_role ur
    WHERE ur.user_id = u.id AND ur.role_id = @role_view_id AND ur.deleted = b'0'
  );

INSERT INTO system_user_role
  (user_id, role_id, creator, updater, deleted, tenant_id)
SELECT u.id, @role_distribute_id, 'codex', 'codex', b'0', @tenant_id
FROM system_users u
WHERE u.tenant_id = @tenant_id
  AND u.deleted = b'0'
  AND u.username = @viewer_user
  AND NOT EXISTS (
    SELECT 1 FROM system_user_role ur
    WHERE ur.user_id = u.id AND ur.role_id = @role_distribute_id AND ur.deleted = b'0'
  );

INSERT INTO system_user_role
  (user_id, role_id, creator, updater, deleted, tenant_id)
SELECT u.id, @role_training_id, 'codex', 'codex', b'0', @tenant_id
FROM system_users u
WHERE u.tenant_id = @tenant_id
  AND u.deleted = b'0'
  AND u.username IN (
    'chenchen',
    'sunrongrong',
    'liuru',
    'zhaojie',
    'xuejianxia',
    'tengweihua',
    'shihaisong',
    'malingling',
    'zhaomingyu'
  )
  AND NOT EXISTS (
    SELECT 1 FROM system_user_role ur
    WHERE ur.user_id = u.id AND ur.role_id = @role_training_id AND ur.deleted = b'0'
  );

COMMIT;

SELECT JSON_OBJECT(
  'roleIds', JSON_OBJECT(
    'view', CAST(@role_view_id AS CHAR),
    'download', CAST(@role_download_id AS CHAR),
    'training', CAST(@role_training_id AS CHAR),
    'distribute', CAST(@role_distribute_id AS CHAR)
  ),
  'targetCategoryId', CAST(@category_id AS CHAR)
) AS result_json;
