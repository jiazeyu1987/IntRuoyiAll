-- Showroom menu seed.
-- This script only seeds system_menu rows so operators can manage showroom tabs
-- from 菜单管理. It intentionally does not grant any role bindings.

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
    (980100, '展厅', '', 1, 80, 0, 'showroom', 'ep:monitor', NULL, NULL, 0, b'1', b'1', b'1', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0'),
    (980101, '公司信息', '', 2, 1, 980100, 'company', 'ep:office-building', 'showroom-admin/index', 'ShowroomAdminCompany', 0, b'1', b'1', b'1', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0'),
    (980118, '公司版本', '', 2, 2, 980100, 'company-version', 'ep:clock', 'showroom-admin/index', 'ShowroomAdminCompanyVersion', 0, b'1', b'1', b'1', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0'),
    (980102, '产品管理', '', 2, 3, 980100, 'product', 'ep:goods', 'showroom-admin/index', 'ShowroomAdminProduct', 0, b'1', b'1', b'1', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0'),
    (980122, '关键词中英对照', '', 2, 4, 980100, 'keyword', 'ep:connection', 'showroom-admin/index', 'ShowroomAdminKeyword', 0, b'1', b'1', b'1', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0'),
    (980119, '提示管理', '', 2, 5, 980100, 'prompt', 'ep:edit-pen', 'showroom-admin/index', 'ShowroomAdminPrompt', 0, b'1', b'1', b'1', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0'),
    (980103, '展厅管理', '', 2, 6, 980100, 'hall', 'ep:grid', 'showroom-admin/index', 'ShowroomAdminHall', 0, b'1', b'1', b'1', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0'),
    (980104, '审批中心', '', 2, 7, 980100, 'approval', 'ep:checked', 'showroom-admin/index', 'ShowroomAdminApproval', 0, b'1', b'1', b'1', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0'),
    (980105, '版本历史', '', 2, 8, 980100, 'history', 'ep:clock', 'showroom-admin/index', 'ShowroomAdminHistory', 0, b'1', b'1', b'1', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0'),
    (980106, '补充指派', '', 2, 9, 980100, 'assignment', 'ep:message', 'showroom-admin/index', 'ShowroomAdminAssignment', 0, b'1', b'1', b'1', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0'),
    (980107, '产品讨论', '', 2, 10, 980100, 'discussion', 'ep:chat-line-round', 'showroom-admin/index', 'ShowroomAdminDiscussion', 0, b'1', b'1', b'1', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0'),
    (980108, '讲解工作台', '', 2, 11, 980100, 'narration-workbench', 'ep:headset', 'showroom-admin/index', 'ShowroomAdminNarration', 0, b'1', b'1', b'1', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0'),
    (980109, '前台大屏', '', 2, 12, 980100, 'display/screen/home', 'ep:monitor', 'showroom-frontstage/screen/views/ScreenHomeView', 'ShowroomDisplayScreenHome', 0, b'1', b'1', b'1', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0'),
    (980110, '大屏公司', '', 2, 1, 980109, 'display/screen/company', '', 'showroom-frontstage/screen/views/ScreenCompanyView', 'ShowroomDisplayScreenCompany', 0, b'0', b'1', b'0', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0'),
    (980111, '大屏展柜', '', 2, 2, 980109, 'display/screen/hall/:hallId(\\d+)', '', 'showroom-frontstage/screen/views/ScreenHallView', 'ShowroomDisplayScreenHall', 0, b'0', b'1', b'0', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0'),
    (980112, '大屏产品详情', '', 2, 3, 980109, 'display/screen/product/:productId(\\d+)', '', 'showroom-frontstage/screen/views/ScreenProductView', 'ShowroomDisplayScreenProduct', 0, b'0', b'1', b'0', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0'),
    (980113, '大屏设置', '', 2, 4, 980109, 'display/screen/settings', '', 'showroom-frontstage/screen/views/ScreenSettingsView', 'ShowroomDisplayScreenSettings', 0, b'0', b'1', b'0', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0'),
    (980114, '大屏讲解播放', '', 2, 5, 980109, 'display/screen/narration', '', 'showroom-frontstage/screen/views/ScreenNarrationView', 'ShowroomDisplayScreenNarration', 0, b'0', b'1', b'0', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0')
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `permission` = VALUES(`permission`),
    `type` = VALUES(`type`),
    `sort` = VALUES(`sort`),
    `parent_id` = VALUES(`parent_id`),
    `path` = VALUES(`path`),
    `icon` = VALUES(`icon`),
    `component` = VALUES(`component`),
    `component_name` = VALUES(`component_name`),
    `status` = VALUES(`status`),
    `visible` = VALUES(`visible`),
    `keep_alive` = VALUES(`keep_alive`),
    `always_show` = VALUES(`always_show`),
    `updater` = VALUES(`updater`),
    `update_time` = VALUES(`update_time`),
    `deleted` = VALUES(`deleted`);
