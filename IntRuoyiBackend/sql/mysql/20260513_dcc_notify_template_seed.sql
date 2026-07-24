-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=seed; riskLevel=low
INSERT INTO `system_notify_template`
(`id`, `name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6800, 'DCC审批通过通知', 'dcc_controlled_file_approved', 2, 'DCC系统',
       '受控文件《{title}》版本 {version} 已审批通过，并已生成受控版本。',
       '["title","version"]', 0, 'DCC finalization seed', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code` = 'dcc_controlled_file_approved');

INSERT INTO `system_notify_template`
(`id`, `name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6801, 'DCC审批驳回通知', 'dcc_controlled_file_rejected', 2, 'DCC系统',
       '受控文件《{title}》版本 {version} 已被驳回，原因：{reason}。',
       '["title","version","reason"]', 0, 'DCC finalization seed', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code` = 'dcc_controlled_file_rejected');

INSERT INTO `system_notify_template`
(`id`, `name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6802, 'DCC盖章失败通知', 'dcc_controlled_file_stamp_failed', 2, 'DCC系统',
       '受控文件《{title}》版本 {version} 盖章失败，原因：{reason}。',
       '["title","version","reason"]', 0, 'DCC finalization seed', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code` = 'dcc_controlled_file_stamp_failed');

INSERT INTO `system_notify_template`
(`id`, `name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6803, 'DCC下发通知', 'dcc_distribution', 2, 'DCC系统',
       '受控文件《{title}》版本 {version} 已正式下发，请及时查阅。',
       '["title","version","effectiveDate"]', 0, 'DCC distribution notify seed', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code` = 'dcc_distribution');

INSERT INTO `system_notify_template`
(`id`, `name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6804, 'DCC培训通知', 'dcc_training', 2, 'DCC系统',
       '受控文件《{title}》版本 {version} 已发起培训，请及时完成培训确认。',
       '["title","version","effectiveDate"]', 0, 'DCC training notify seed', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code` = 'dcc_training');

INSERT INTO `system_notify_template`
(`id`, `name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6805, 'DCC作废通知', 'dcc_obsolete', 2, 'DCC系统',
       '受控文件《{title}》版本 {version} 已作废，原因：{reason}。',
       '["title","version","reason"]', 0, 'DCC obsolete notify seed', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code` = 'dcc_obsolete');

INSERT INTO `system_notify_template`
(`id`, `name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6806, 'DCC待办通知', 'dcc_task_assigned', 2, 'DCC系统',
       '您收到一条 DCC 待办任务：{processInstanceName} - {taskName}，提交人：{startUserNickname}。请尽快处理。',
       '["processInstanceName","taskName","startUserNickname","detailUrl"]', 0, 'DCC task assigned notify seed', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code` = 'dcc_task_assigned');

INSERT INTO `system_notify_template`
(`id`, `name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6807, 'DCC审批通过通知', 'dcc_controlled_file_approved', 2, 'DCC系统',
       '受控文件流程《{processInstanceName}》已审批通过，请查看详情。',
       '["processInstanceName","detailUrl"]', 0, 'DCC process approve notify seed', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code` = 'dcc_controlled_file_approved');

INSERT INTO `system_notify_template`
(`id`, `name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6808, 'DCC审批驳回通知', 'dcc_controlled_file_rejected', 2, 'DCC系统',
       '受控文件流程《{processInstanceName}》已被驳回，原因：{reason}。',
       '["processInstanceName","reason","detailUrl"]', 0, 'DCC process reject notify seed', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code` = 'dcc_controlled_file_rejected');

INSERT INTO `system_notify_template`
(`id`, `name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6809, 'DCC待办超时提醒', 'dcc_task_timeout', 2, 'DCC系统',
       '您有一条 DCC 待办任务超时提醒：{processInstanceName} - {taskName}。请尽快处理。',
       '["processInstanceName","taskName","detailUrl"]', 0, 'DCC task timeout notify seed', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code` = 'dcc_task_timeout');

UPDATE `system_notify_template`
SET `name` = 'DCC审批通过通知',
    `nickname` = 'DCC系统',
    `content` = '受控文件流程《{processInstanceName}》已审批通过，请查看详情。',
    `params` = '["processInstanceName","detailUrl"]',
    `status` = 0,
    `remark` = 'DCC process approve notify seed',
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `code` = 'dcc_controlled_file_approved';

UPDATE `system_notify_template`
SET `name` = 'DCC审批驳回通知',
    `nickname` = 'DCC系统',
    `content` = '受控文件流程《{processInstanceName}》已被驳回，原因：{reason}。',
    `params` = '["processInstanceName","reason","detailUrl"]',
    `status` = 0,
    `remark` = 'DCC process reject notify seed',
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `code` = 'dcc_controlled_file_rejected';

UPDATE `system_notify_template`
SET `name` = 'DCC盖章失败通知',
    `nickname` = 'DCC系统',
    `content` = '受控文件《{title}》版本 {version} 盖章失败，原因：{reason}。',
    `params` = '["title","version","reason"]',
    `status` = 0,
    `remark` = 'DCC stamp failed notify seed',
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `code` = 'dcc_controlled_file_stamp_failed';

UPDATE `system_notify_template`
SET `name` = 'DCC下发通知',
    `nickname` = 'DCC系统',
    `content` = '受控文件《{title}》版本 {version} 已正式下发，请及时查阅。',
    `params` = '["title","version","effectiveDate"]',
    `status` = 0,
    `remark` = 'DCC distribution notify seed',
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `code` = 'dcc_distribution';

UPDATE `system_notify_template`
SET `name` = 'DCC培训通知',
    `nickname` = 'DCC系统',
    `content` = '受控文件《{title}》版本 {version} 已发起培训，请及时完成培训确认。',
    `params` = '["title","version","effectiveDate"]',
    `status` = 0,
    `remark` = 'DCC training notify seed',
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `code` = 'dcc_training';

UPDATE `system_notify_template`
SET `name` = 'DCC作废通知',
    `nickname` = 'DCC系统',
    `content` = '受控文件《{title}》版本 {version} 已作废，原因：{reason}。',
    `params` = '["title","version","reason"]',
    `status` = 0,
    `remark` = 'DCC obsolete notify seed',
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `code` = 'dcc_obsolete';

UPDATE `system_notify_template`
SET `name` = 'DCC待办通知',
    `nickname` = 'DCC系统',
    `content` = '您收到一条 DCC 待办任务：{processInstanceName} - {taskName}，提交人：{startUserNickname}。请尽快处理。',
    `params` = '["processInstanceName","taskName","startUserNickname","detailUrl"]',
    `status` = 0,
    `remark` = 'DCC task assigned notify seed',
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `code` = 'dcc_task_assigned';

UPDATE `system_notify_template`
SET `name` = 'DCC待办超时提醒',
    `nickname` = 'DCC系统',
    `content` = '您有一条 DCC 待办任务超时提醒：{processInstanceName} - {taskName}。请尽快处理。',
    `params` = '["processInstanceName","taskName","detailUrl"]',
    `status` = 0,
    `remark` = 'DCC task timeout notify seed',
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `code` = 'dcc_task_timeout';
