-- Showroom whole-assignment verification seed for test tenant 122.
-- This script only prepares the minimum local/test data needed to verify:
-- 1. a tenant-scoped EDITOR role
-- 2. a test editor user showroomeditor
-- 3. tenant_admin + EDITOR bindings for that user

SET @seed_now = NOW();

INSERT INTO `system_role`
(`id`, `name`, `code`, `sort`, `data_scope`, `data_scope_dept_ids`, `status`, `type`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 910210, '展厅编辑', 'EDITOR', 2, 1, '', 0, 1, 'showroom whole assignment verification seed',
       'showroom-seed', @seed_now, 'showroom-seed', @seed_now, b'0', 122
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role`
    WHERE `code` = 'EDITOR'
      AND `tenant_id` = 122
      AND `deleted` = b'0'
);

UPDATE `system_role`
SET `name` = '展厅编辑',
    `sort` = 2,
    `data_scope` = 1,
    `data_scope_dept_ids` = '',
    `status` = 0,
    `type` = 1,
    `remark` = 'showroom whole assignment verification seed',
    `updater` = 'showroom-seed',
    `update_time` = @seed_now,
    `deleted` = b'0'
WHERE `code` = 'EDITOR'
  AND `tenant_id` = 122;

INSERT INTO `system_users`
(`id`, `username`, `password`, `nickname`, `remark`, `dept_id`, `post_ids`, `email`, `mobile`, `sex`, `avatar`, `status`, `login_ip`, `login_date`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 910202, 'showroomeditor', '$2a$10$0acJOIk2D25/oC87nyclE..0lzeu9DtQ/n3geP4fkun/zIVRhHJIO', '展厅编辑',
       'showroom whole assignment verification seed', NULL, NULL, NULL, NULL, 0, NULL, 0, NULL, NULL,
       'showroom-seed', @seed_now, 'showroom-seed', @seed_now, b'0', 122
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_users`
    WHERE `username` = 'showroomeditor'
      AND `deleted` = b'0'
);

UPDATE `system_users`
SET `password` = '$2a$10$0acJOIk2D25/oC87nyclE..0lzeu9DtQ/n3geP4fkun/zIVRhHJIO',
    `nickname` = '展厅编辑',
    `remark` = 'showroom whole assignment verification seed',
    `dept_id` = NULL,
    `post_ids` = NULL,
    `email` = NULL,
    `mobile` = NULL,
    `sex` = 0,
    `avatar` = NULL,
    `status` = 0,
    `updater` = 'showroom-seed',
    `update_time` = @seed_now,
    `deleted` = b'0',
    `tenant_id` = 122
WHERE `username` = 'showroomeditor';

SET @showroom_editor_user_id = (
    SELECT `id`
    FROM `system_users`
    WHERE `username` = 'showroomeditor'
      AND `deleted` = b'0'
    LIMIT 1
);

SET @tenant_admin_role_id = (
    SELECT `id`
    FROM `system_role`
    WHERE `code` = 'tenant_admin'
      AND `tenant_id` = 122
      AND `deleted` = b'0'
    LIMIT 1
);

SET @showroom_editor_role_id = (
    SELECT `id`
    FROM `system_role`
    WHERE `code` = 'EDITOR'
      AND `tenant_id` = 122
      AND `deleted` = b'0'
    LIMIT 1
);

INSERT INTO `system_user_role`
(`user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT @showroom_editor_user_id, @tenant_admin_role_id, 'showroom-seed', @seed_now, 'showroom-seed', @seed_now, b'0', 122
WHERE @showroom_editor_user_id IS NOT NULL
  AND @tenant_admin_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM `system_user_role`
      WHERE `user_id` = @showroom_editor_user_id
        AND `role_id` = @tenant_admin_role_id
        AND `deleted` = b'0'
  );

INSERT INTO `system_user_role`
(`user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT @showroom_editor_user_id, @showroom_editor_role_id, 'showroom-seed', @seed_now, 'showroom-seed', @seed_now, b'0', 122
WHERE @showroom_editor_user_id IS NOT NULL
  AND @showroom_editor_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM `system_user_role`
      WHERE `user_id` = @showroom_editor_user_id
        AND `role_id` = @showroom_editor_role_id
        AND `deleted` = b'0'
  );
