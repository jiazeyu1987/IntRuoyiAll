-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- MES 排产工单自动排产资格历史数据回填。
-- 规则：已绑定工艺排产路线的排产工单可参与自动排产；未绑定路线的排产工单不可参与自动排产。

UPDATE `mes_pro_schedule_order`
SET `auto_schedulable` = b'1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `route_id` IS NOT NULL
  AND `auto_schedulable` IS NULL;

UPDATE `mes_pro_schedule_order`
SET `auto_schedulable` = b'0',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `route_id` IS NULL
  AND `auto_schedulable` IS NULL;
