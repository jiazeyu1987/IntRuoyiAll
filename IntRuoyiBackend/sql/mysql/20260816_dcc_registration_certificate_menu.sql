-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260818_dcc_registration_certificate_reminder,20260626_dcc_basic_data_global_submenu; type=data; riskLevel=medium
-- Purpose: Add domestic registration certificate dynamic menu, hidden routes and independent permissions.

BEGIN;

SET @menu_name_basic_data := '基础数据';
SET @menu_name_registration_certificate := '注册证';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990200, @menu_name_basic_data, '', 1, 35, 0, '/mdm', 'ep:coin', NULL, NULL,
       0, b'1', b'1', b'1', 'dcc-registration-certificate-menu', NOW(),
       'dcc-registration-certificate-menu', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1
    FROM `system_menu`
   WHERE `deleted` = b'0'
     AND (`id` = 990200 OR `path` = '/mdm')
);

UPDATE `system_menu`
   SET `name` = @menu_name_basic_data,
       `updater` = 'dcc-registration-certificate-menu',
       `update_time` = NOW()
 WHERE `deleted` = b'0'
   AND (`id` = 990200 OR `path` = '/mdm');

SET @dcc_basic_data_menu_id := (
    SELECT `id`
      FROM `system_menu`
     WHERE `deleted` = b'0'
       AND (`id` = 990200 OR `path` = '/mdm')
     LIMIT 1
);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990230, @menu_name_registration_certificate, 'dcc:registration-certificate:query-current',
       2, 22, @dcc_basic_data_menu_id, 'registration-certificate', 'ep:document-checked',
       'dcc/registration-certificate/index/index', 'DccRegistrationCertificateIndex',
       0, b'1', b'1', b'1', 'dcc-registration-certificate-menu', NOW(),
       'dcc-registration-certificate-menu', NOW(), b'0'
WHERE @dcc_basic_data_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `deleted` = b'0'
         AND (`id` = 990230
          OR (`parent_id` = @dcc_basic_data_menu_id AND `path` = 'registration-certificate')
          OR `permission` = 'dcc:registration-certificate:query-current')
  );

UPDATE `system_menu`
   SET `name` = @menu_name_registration_certificate,
       `permission` = 'dcc:registration-certificate:query-current',
       `type` = 2,
       `sort` = 22,
       `parent_id` = @dcc_basic_data_menu_id,
       `path` = 'registration-certificate',
       `icon` = 'ep:document-checked',
       `component` = 'dcc/registration-certificate/index/index',
       `component_name` = 'DccRegistrationCertificateIndex',
       `status` = 0,
       `visible` = b'1',
       `keep_alive` = b'1',
       `always_show` = b'1',
       `updater` = 'dcc-registration-certificate-menu',
       `update_time` = NOW()
 WHERE @dcc_basic_data_menu_id IS NOT NULL
   AND `deleted` = b'0'
   AND (`id` = 990230
    OR (`parent_id` = @dcc_basic_data_menu_id AND `path` = 'registration-certificate')
    OR `permission` = 'dcc:registration-certificate:query-current');

SET @dcc_registration_certificate_menu_id := (
    SELECT `id`
      FROM `system_menu`
     WHERE `deleted` = b'0'
       AND `parent_id` = @dcc_basic_data_menu_id
       AND `path` = 'registration-certificate'
     LIMIT 1
);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990231, '注册证详情', 'dcc:registration-certificate:query-current',
       2, 1, @dcc_basic_data_menu_id, 'registration-certificate/detail/:id', 'ep:document',
       'dcc/registration-certificate/detail/index', 'DccRegistrationCertificateDetail',
       0, b'0', b'1', b'0', 'dcc-registration-certificate-menu', NOW(),
       'dcc-registration-certificate-menu', NOW(), b'0'
WHERE @dcc_basic_data_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `deleted` = b'0'
         AND (`id` = 990231
          OR (`parent_id` = @dcc_basic_data_menu_id AND `path` = 'registration-certificate/detail/:id'))
  );

UPDATE `system_menu`
   SET `name` = '注册证详情',
       `permission` = 'dcc:registration-certificate:query-current',
       `type` = 2,
       `sort` = 1,
       `parent_id` = @dcc_basic_data_menu_id,
       `path` = 'registration-certificate/detail/:id',
       `icon` = 'ep:document',
       `component` = 'dcc/registration-certificate/detail/index',
       `component_name` = 'DccRegistrationCertificateDetail',
       `status` = 0,
       `visible` = b'0',
       `keep_alive` = b'1',
       `always_show` = b'0',
       `updater` = 'dcc-registration-certificate-menu',
       `update_time` = NOW()
 WHERE @dcc_basic_data_menu_id IS NOT NULL
   AND `deleted` = b'0'
   AND (`id` = 990231
    OR (`parent_id` = @dcc_basic_data_menu_id AND `path` = 'registration-certificate/detail/:id'));

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 990232, '注册证履历', 'dcc:registration-certificate:query-current',
       2, 2, @dcc_basic_data_menu_id, 'registration-certificate/history/:id', 'ep:clock',
       'dcc/registration-certificate/history/index', 'DccRegistrationCertificateHistory',
       0, b'0', b'1', b'0', 'dcc-registration-certificate-menu', NOW(),
       'dcc-registration-certificate-menu', NOW(), b'0'
WHERE @dcc_basic_data_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `deleted` = b'0'
         AND (`id` = 990232
          OR (`parent_id` = @dcc_basic_data_menu_id AND `path` = 'registration-certificate/history/:id'))
  );

UPDATE `system_menu`
   SET `name` = '注册证履历',
       `permission` = 'dcc:registration-certificate:query-current',
       `type` = 2,
       `sort` = 2,
       `parent_id` = @dcc_basic_data_menu_id,
       `path` = 'registration-certificate/history/:id',
       `icon` = 'ep:clock',
       `component` = 'dcc/registration-certificate/history/index',
       `component_name` = 'DccRegistrationCertificateHistory',
       `status` = 0,
       `visible` = b'0',
       `keep_alive` = b'1',
       `always_show` = b'0',
       `updater` = 'dcc-registration-certificate-menu',
       `update_time` = NOW()
 WHERE @dcc_basic_data_menu_id IS NOT NULL
   AND `deleted` = b'0'
   AND (`id` = 990232
    OR (`parent_id` = @dcc_basic_data_menu_id AND `path` = 'registration-certificate/history/:id'));

DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_registration_certificate_permission_menu`;
CREATE TEMPORARY TABLE `tmp_dcc_registration_certificate_permission_menu` (
  `id` bigint NOT NULL,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `permission` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tmp_dcc_registration_certificate_permission` (`permission`)
);

INSERT INTO `tmp_dcc_registration_certificate_permission_menu`
(`id`, `name`, `permission`, `sort`)
VALUES
  (990233, '注册证新增', 'dcc:registration-certificate:create', 10),
  (990234, '注册证修改', 'dcc:registration-certificate:update', 20),
  (990235, '删除注册证草稿', 'dcc:registration-certificate:delete-draft', 30),
  (990236, '注册证正式化', 'dcc:registration-certificate:formalize', 40),
  (990237, '注册证访问申请', 'dcc:registration-certificate:access-request:create', 50),
  (990246, '注册证访问审批', 'dcc:registration-certificate:access-request:approve', 55),
  (990247, '注册证上传提交', 'dcc:registration-certificate:upload:create', 56),
  (990248, '注册证上传审批', 'dcc:registration-certificate:upload:approve', 57),
  (990238, '注册证配置查询', 'dcc:registration-certificate:config:query', 60),
  (990239, '注册证配置修改', 'dcc:registration-certificate:config:update', 70),
  (990240, '注册证延续上传', 'dcc:registration-certificate:renewal:upload', 80),
  (990241, '注册证延续作废', 'dcc:registration-certificate:renewal:void', 90),
  (990242, '注册证变更提交', 'dcc:registration-certificate:change:submit', 100),
  (990243, '注册证作废', 'dcc:registration-certificate:void', 110),
  (990244, '注册证支持文件上传', 'dcc:registration-certificate:supporting-document:upload', 120),
  (990245, '注册证支持文件确认', 'dcc:registration-certificate:supporting-document:confirm', 130);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT candidate.`id`, candidate.`name`, candidate.`permission`, 3, candidate.`sort`,
       @dcc_registration_certificate_menu_id, '', '', '', '',
       0, b'1', b'1', b'1', 'dcc-registration-certificate-menu', NOW(),
       'dcc-registration-certificate-menu', NOW(), b'0'
  FROM `tmp_dcc_registration_certificate_permission_menu` candidate
 WHERE @dcc_registration_certificate_menu_id IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
         FROM `system_menu` existing
        WHERE existing.`deleted` = b'0'
          AND (existing.`id` = candidate.`id`
           OR existing.`permission` = candidate.`permission`)
   );

UPDATE `system_menu` menu
JOIN `tmp_dcc_registration_certificate_permission_menu` candidate
  ON menu.`id` = candidate.`id`
  OR menu.`permission` = candidate.`permission`
   SET menu.`name` = candidate.`name`,
       menu.`permission` = candidate.`permission`,
       menu.`type` = 3,
       menu.`sort` = candidate.`sort`,
       menu.`parent_id` = @dcc_registration_certificate_menu_id,
       menu.`path` = '',
       menu.`icon` = '',
       menu.`component` = '',
       menu.`component_name` = '',
       menu.`status` = 0,
       menu.`visible` = b'1',
       menu.`keep_alive` = b'1',
       menu.`always_show` = b'1',
       menu.`updater` = 'dcc-registration-certificate-menu',
       menu.`update_time` = NOW()
 WHERE @dcc_registration_certificate_menu_id IS NOT NULL
   AND menu.`deleted` = b'0';

DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_registration_certificate_permission_menu`;

COMMIT;
