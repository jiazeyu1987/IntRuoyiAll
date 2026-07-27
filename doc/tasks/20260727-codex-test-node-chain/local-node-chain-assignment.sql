SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS assign_local_codex_test_node_chains;
DELIMITER //
CREATE PROCEDURE assign_local_codex_test_node_chains()
BEGIN
  IF (
    SELECT COUNT(*)
      FROM `system_codex_test_case`
     WHERE `tenant_id` = 1
       AND `deleted` = b'0'
       AND `name` IN (
         '工艺路线节点：基础维护',
         '工艺路线节点：复制绑定',
         '工艺路线节点：版本发布',
         '工艺路线节点：状态删除',
         '批记录节点：解析',
         '批记录节点：版本治理',
         '批记录节点：绑定快照',
         '批记录节点：批次任务',
         '批记录节点：填写审批',
         '批记录节点：归档追溯',
         '智能排产节点：工单入池',
         '智能排产节点：手动重排',
         '智能排产节点：范围保护',
         '智能排产节点：产能口径'
       )
  ) <> 14 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Local codex node-chain target count mismatch';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_codex_test_case`
     WHERE `tenant_id` = 1
       AND `deleted` = b'0'
       AND `name` IN (
         '工艺路线节点：基础维护',
         '工艺路线节点：复制绑定',
         '工艺路线节点：版本发布',
         '工艺路线节点：状态删除',
         '批记录节点：解析',
         '批记录节点：版本治理',
         '批记录节点：绑定快照',
         '批记录节点：批次任务',
         '批记录节点：填写审批',
         '批记录节点：归档追溯',
         '智能排产节点：工单入池',
         '智能排产节点：手动重排',
         '智能排产节点：范围保护',
         '智能排产节点：产能口径'
       )
       AND (`node_chain_name` IS NOT NULL OR `node_chain_sort` IS NOT NULL)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Local codex node-chain target is already assigned';
  END IF;

  UPDATE `system_codex_test_case`
     SET `node_chain_name` = CASE
           WHEN `project` = '工艺路线' THEN '工艺路线节点闭环'
           WHEN `project` = '批记录' THEN '批记录节点闭环'
           WHEN `project` = '智能排产' THEN '智能排产节点闭环'
         END,
         `node_chain_sort` = CASE `name`
           WHEN '工艺路线节点：基础维护' THEN 1
           WHEN '工艺路线节点：复制绑定' THEN 2
           WHEN '工艺路线节点：版本发布' THEN 3
           WHEN '工艺路线节点：状态删除' THEN 4
           WHEN '批记录节点：解析' THEN 1
           WHEN '批记录节点：版本治理' THEN 2
           WHEN '批记录节点：绑定快照' THEN 3
           WHEN '批记录节点：批次任务' THEN 4
           WHEN '批记录节点：填写审批' THEN 5
           WHEN '批记录节点：归档追溯' THEN 6
           WHEN '智能排产节点：工单入池' THEN 1
           WHEN '智能排产节点：手动重排' THEN 2
           WHEN '智能排产节点：范围保护' THEN 3
           WHEN '智能排产节点：产能口径' THEN 4
         END,
         `updater` = 'codex',
         `update_time` = NOW()
   WHERE `tenant_id` = 1
     AND `deleted` = b'0'
     AND `name` IN (
       '工艺路线节点：基础维护',
       '工艺路线节点：复制绑定',
       '工艺路线节点：版本发布',
       '工艺路线节点：状态删除',
       '批记录节点：解析',
       '批记录节点：版本治理',
       '批记录节点：绑定快照',
       '批记录节点：批次任务',
       '批记录节点：填写审批',
       '批记录节点：归档追溯',
       '智能排产节点：工单入池',
       '智能排产节点：手动重排',
       '智能排产节点：范围保护',
       '智能排产节点：产能口径'
     );

  IF ROW_COUNT() <> 14 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Local codex node-chain update count mismatch';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM (
        SELECT `node_chain_name`, COUNT(*) AS `node_count`,
               MIN(`node_chain_sort`) AS `min_sort`,
               MAX(`node_chain_sort`) AS `max_sort`,
               COUNT(DISTINCT `node_chain_sort`) AS `distinct_sort_count`
          FROM `system_codex_test_case`
         WHERE `tenant_id` = 1
           AND `deleted` = b'0'
           AND `node_chain_name` IN (
             '工艺路线节点闭环',
             '批记录节点闭环',
             '智能排产节点闭环'
           )
         GROUP BY `node_chain_name`
      ) AS `chain_summary`
     WHERE (`node_chain_name` = '工艺路线节点闭环'
            AND (`node_count` <> 4 OR `min_sort` <> 1 OR `max_sort` <> 4 OR `distinct_sort_count` <> 4))
        OR (`node_chain_name` = '批记录节点闭环'
            AND (`node_count` <> 6 OR `min_sort` <> 1 OR `max_sort` <> 6 OR `distinct_sort_count` <> 6))
        OR (`node_chain_name` = '智能排产节点闭环'
            AND (`node_count` <> 4 OR `min_sort` <> 1 OR `max_sort` <> 4 OR `distinct_sort_count` <> 4))
  ) OR (
    SELECT COUNT(DISTINCT `node_chain_name`)
      FROM `system_codex_test_case`
     WHERE `tenant_id` = 1
       AND `deleted` = b'0'
       AND `node_chain_name` IN (
         '工艺路线节点闭环',
         '批记录节点闭环',
         '智能排产节点闭环'
       )
  ) <> 3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Local codex node-chain verification failed';
  END IF;
END//
DELIMITER ;

CALL assign_local_codex_test_node_chains();

DROP PROCEDURE IF EXISTS assign_local_codex_test_node_chains;
