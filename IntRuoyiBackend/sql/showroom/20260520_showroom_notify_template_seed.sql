INSERT INTO `system_notify_template`
(`id`, `name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6810, '展厅审批待办通知', 'SHOWROOM_APPROVAL_PENDING', 2, '展厅系统',
       '展厅{targetTypeText}【{targetName}】待{approvalStage}，点击查看对应内容。',
       '["targetTypeText","targetName","approvalStage","targetType","targetId","changeRequestId","notifyOpen"]', 0, 'showroom approval pending notify seed', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code` = 'SHOWROOM_APPROVAL_PENDING');

INSERT INTO `system_notify_template`
(`id`, `name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6811, '展厅发布完成通知', 'SHOWROOM_APPROVAL_PUBLISHED', 2, '展厅系统',
       '展厅{targetTypeText}【{targetName}】已审批通过并发布，点击查看对应内容。',
       '["targetTypeText","targetName","targetType","targetId","changeRequestId","notifyOpen"]', 0, 'showroom approval published notify seed', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code` = 'SHOWROOM_APPROVAL_PUBLISHED');

INSERT INTO `system_notify_template`
(`id`, `name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6812, '展厅审批驳回通知', 'SHOWROOM_APPROVAL_REJECTED', 2, '展厅系统',
       '展厅{targetTypeText}【{targetName}】在{approvalStage}被驳回，原因：{rejectionReason}。点击后可继续修改原提交内容。',
       '["targetTypeText","targetName","approvalStage","rejectionReason"]', 0, 'showroom approval rejected notify seed', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code` = 'SHOWROOM_APPROVAL_REJECTED');

INSERT INTO `system_notify_template`
(`id`, `name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6813, '展厅补充指派通知', 'SHOWROOM_ASSIGNMENT', 2, '展厅系统',
       '请处理展厅{targetType}【{targetId}】的{fieldCode}补充指派，发起人：{assignedBy}。',
       '["fieldCode","targetType","targetId","assignedBy"]', 0, 'showroom assignment notify seed', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` WHERE `code` = 'SHOWROOM_ASSIGNMENT');

UPDATE `system_notify_template`
SET `name` = '展厅审批待办通知',
    `nickname` = '展厅系统',
    `content` = '展厅{targetTypeText}【{targetName}】待{approvalStage}，点击查看对应内容。',
    `params` = '["targetTypeText","targetName","approvalStage","targetType","targetId","changeRequestId","notifyOpen"]',
    `status` = 0,
    `remark` = 'showroom approval pending notify seed',
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `code` = 'SHOWROOM_APPROVAL_PENDING';

UPDATE `system_notify_template`
SET `name` = '展厅发布完成通知',
    `nickname` = '展厅系统',
    `content` = '展厅{targetTypeText}【{targetName}】已审批通过并发布，点击查看对应内容。',
    `params` = '["targetTypeText","targetName","targetType","targetId","changeRequestId","notifyOpen"]',
    `status` = 0,
    `remark` = 'showroom approval published notify seed',
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `code` = 'SHOWROOM_APPROVAL_PUBLISHED';

UPDATE `system_notify_template`
SET `name` = '展厅审批驳回通知',
    `nickname` = '展厅系统',
    `content` = '展厅{targetTypeText}【{targetName}】在{approvalStage}被驳回，原因：{rejectionReason}。点击后可继续修改原提交内容。',
    `params` = '["targetTypeText","targetName","approvalStage","rejectionReason"]',
    `status` = 0,
    `remark` = 'showroom approval rejected notify seed',
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `code` = 'SHOWROOM_APPROVAL_REJECTED';

UPDATE `system_notify_template`
SET `name` = '展厅补充指派通知',
    `nickname` = '展厅系统',
    `content` = '请处理展厅{targetType}【{targetId}】的{fieldCode}补充指派，发起人：{assignedBy}。',
    `params` = '["fieldCode","targetType","targetId","assignedBy"]',
    `status` = 0,
    `remark` = 'showroom assignment notify seed',
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `code` = 'SHOWROOM_ASSIGNMENT';
