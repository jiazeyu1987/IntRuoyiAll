-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260724_system_codex_test_management; type=seed; riskLevel=low
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_system_codex_smart_scheduling_test_items;
DELIMITER //
CREATE PROCEDURE ensure_system_codex_smart_scheduling_test_items()
BEGIN
  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'system_codex_test_case'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing system_codex_test_case table';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'system_codex_test_checkpoint'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing system_codex_test_checkpoint table';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_codex_smart_scheduling_case_seed`;
  CREATE TEMPORARY TABLE `tmp_codex_smart_scheduling_case_seed` (
    `case_name` varchar(128) NOT NULL,
    `project` varchar(16) NOT NULL,
    `method_text` text NOT NULL,
    `test_data_text` text NOT NULL,
    `default_execution_mode` varchar(16) NOT NULL,
    `parallel_safe` bit NOT NULL,
    `status` varchar(16) NOT NULL,
    `sort` int NOT NULL,
    PRIMARY KEY (`case_name`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

  INSERT INTO `tmp_codex_smart_scheduling_case_seed` (
    `case_name`, `project`, `method_text`, `test_data_text`, `default_execution_mode`, `parallel_safe`, `status`, `sort`
  )
  VALUES
  (
    '智能排产-全链路冒烟：入池、预览、发布、日历、报工闭环',
    '智能排产',
    CONCAT(
      '使用 Playwright 真实页面执行 IntRuoyiFronted/tests/e2e/smart-scheduling-smoke-real-flow.e2e.js 的同等路径：登录测试租户，进入排产员工作台，创建并同步生产工单，按页面可见工单号入池为排产工单，打开自动排产抽屉生成预览，确认发布，核对排程日历，再导入第三方报工并完成归因和审批。',
      CHAR(10),
      '必须按页面可见业务唯一文本定位工单、排产工单和报工记录；禁止 API-only、直接 SQL、mock 截图、默认成功或跳过 Runner 前置检查。'
    ),
    CONCAT(
      '目标租户：测试租户；账号与 runner 本地凭据映射由 Runner 安全配置提供；生产工单号、产品编号、报工文件和 artifact 路径必须由本次 runId 生成并可追踪。',
      CHAR(10),
      '该项会写入 MES 测试数据，执行前必须确认测试租户、Runner 在线、Playwright 浏览器和清理责任。'
    ),
    'SEQUENTIAL',
    b'0',
    'ENABLE',
    910401
  ),
  (
    '智能排产-只读一致性：工作台、排产工单、排程日历',
    '智能排产',
    CONCAT(
      '使用 Playwright 真实页面执行 IntRuoyiFronted/tests/e2e/smart-scheduling-target-alignment-readonly.e2e.js 的同等路径：登录测试租户，只读打开生产工单、排产工单和排程日历，读取当前真实列表数据，核对工作台指标、排产工单分层进度字段、日历班次短缺和锁定状态。',
      CHAR(10),
      '只读模式不得提交 MES 写请求；如果目标页面没有真实数据，检查点必须记录 BLOCKED 和页面证据，禁止 API-only、mock 数据或默认通过。'
    ),
    '目标租户：测试租户；账号与 runner 本地凭据映射由 Runner 安全配置提供；只读巡检使用当前页面真实可见数据，不创建业务记录。',
    'SEQUENTIAL',
    b'0',
    'ENABLE',
    910402
  ),
  (
    '智能排产-可点击安全巡检：危险写入必须显式确认',
    '智能排产',
    CONCAT(
      '使用 Playwright 真实页面执行 IntRuoyiFronted/tests/e2e/smart-scheduling-clickable-coverage.e2e.js 的同等路径：遍历排产员工作台、排产工单、生产排产、排程日历、工艺流程排产配置、报工、璞慧排产和排产看板。',
      CHAR(10),
      '安全按钮可点击并记录页面反馈；危险写入按钮只能打开确认入口，不点击最终确认；任何 MES 写请求必须被检查点捕获并判失败。禁止 API-only、坐标猜测或静默忽略控制台错误。'
    ),
    '目标租户：测试租户；账号与 runner 本地凭据映射由 Runner 安全配置提供；本项不确认新增、删除、发布、审批、导入、重排、锁定或解锁等写入动作。',
    'SEQUENTIAL',
    b'0',
    'ENABLE',
    910403
  );

  INSERT INTO `system_codex_test_case` (
    `name`, `project`, `method_text`, `test_data_text`, `default_execution_mode`, `parallel_safe`, `status`, `sort`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `seed`.`case_name`, `seed`.`project`, `seed`.`method_text`, `seed`.`test_data_text`, `seed`.`default_execution_mode`,
    `seed`.`parallel_safe`, `seed`.`status`, `seed`.`sort`, 'codex', NOW(), 'codex', NOW(), b'0', 1
    FROM `tmp_codex_smart_scheduling_case_seed` AS `seed`
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_codex_test_case` AS `existing`
      WHERE `existing`.`tenant_id` = 1
        AND `existing`.`name` = `seed`.`case_name`
        AND `existing`.`deleted` = b'0'
   );

  UPDATE `system_codex_test_case` AS `case_item`
  JOIN `tmp_codex_smart_scheduling_case_seed` AS `seed`
    ON `case_item`.`tenant_id` = 1
   AND `case_item`.`name` = `seed`.`case_name`
   AND `case_item`.`deleted` = b'0'
     SET `case_item`.`method_text` = `seed`.`method_text`,
         `case_item`.`project` = `seed`.`project`,
         `case_item`.`test_data_text` = `seed`.`test_data_text`,
         `case_item`.`default_execution_mode` = `seed`.`default_execution_mode`,
         `case_item`.`parallel_safe` = `seed`.`parallel_safe`,
         `case_item`.`status` = `seed`.`status`,
         `case_item`.`sort` = `seed`.`sort`,
         `case_item`.`updater` = 'codex',
         `case_item`.`update_time` = NOW();

  DROP TEMPORARY TABLE IF EXISTS `tmp_codex_smart_scheduling_case_ids`;
  CREATE TEMPORARY TABLE `tmp_codex_smart_scheduling_case_ids` (
    `case_name` varchar(128) NOT NULL,
    `case_id` bigint NOT NULL,
    PRIMARY KEY (`case_name`)
  ) ENGINE=Memory;

  INSERT INTO `tmp_codex_smart_scheduling_case_ids` (`case_name`, `case_id`)
  SELECT `seed`.`case_name`, `case_item`.`id`
    FROM `tmp_codex_smart_scheduling_case_seed` AS `seed`
    JOIN `system_codex_test_case` AS `case_item`
      ON `case_item`.`tenant_id` = 1
     AND `case_item`.`name` = `seed`.`case_name`
     AND `case_item`.`deleted` = b'0';

  IF (SELECT COUNT(*) FROM `tmp_codex_smart_scheduling_case_ids`) <> 3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Codex smart scheduling test case seed count mismatch';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_codex_smart_scheduling_checkpoint_seed`;
  CREATE TEMPORARY TABLE `tmp_codex_smart_scheduling_checkpoint_seed` (
    `case_name` varchar(128) NOT NULL,
    `checkpoint_sort` int NOT NULL,
    `checkpoint_name` varchar(128) NOT NULL,
    `expected_text` text NOT NULL,
    `severity` varchar(16) NOT NULL,
    `remark` varchar(512) NOT NULL,
    PRIMARY KEY (`case_name`, `checkpoint_sort`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

  INSERT INTO `tmp_codex_smart_scheduling_checkpoint_seed` (
    `case_name`, `checkpoint_sort`, `checkpoint_name`, `expected_text`, `severity`, `remark`
  )
  VALUES
  (
    '智能排产-全链路冒烟：入池、预览、发布、日历、报工闭环',
    1,
    '生产工单入池并保留来源快照',
    '页面按本次 runId 的生产工单号完成入池；排产工单存在 sourceSnapshotJson 和启用工序快照；选中集合不得包含额外工单。',
    'CRITICAL',
    '覆盖待同步工单入池、来源快照和可见业务行定位。'
  ),
  (
    '智能排产-全链路冒烟：入池、预览、发布、日历、报工闭环',
    2,
    '自动排产预览返回工序快照和阻断明细',
    '自动排产预览接口业务码成功；预览结果包含工序任务、calendarContextToken、问题列表和阻断状态；阻断时不得继续确认发布。',
    'CRITICAL',
    '覆盖预览、前置阻断和发布前检查。'
  ),
  (
    '智能排产-全链路冒烟：入池、预览、发布、日历、报工闭环',
    3,
    '确认发布后排产工单状态与排程日历同步',
    '确认发布请求成功后，排产工单状态回写为已排产或生产中；排程日历能看到本次排产任务、班次和锁定/短缺提示。',
    'CRITICAL',
    '覆盖自动排产 apply 与日历联动。'
  ),
  (
    '智能排产-全链路冒烟：入池、预览、发布、日历、报工闭环',
    4,
    '第三方报工导入和归因不绕过审批',
    '第三方报工导入产生本次 runId 记录；归因后审批前排产进度不提前最终确认；不得用 API-only 直接改进度。',
    'MAJOR',
    '覆盖报工导入、归因和审批边界。'
  ),
  (
    '智能排产-全链路冒烟：入池、预览、发布、日历、报工闭环',
    5,
    '审批通过后排产进度按工序回写',
    '审批通过后排产工单 progressPercent、completedQuantity 和目标工序完成量按真实报工数量回写；拒绝或未审批记录不得推进进度。',
    'CRITICAL',
    '覆盖报工闭环最终状态。'
  ),
  (
    '智能排产-只读一致性：工作台、排产工单、排程日历',
    1,
    '工作台八项指标可见且接口成功',
    '排产员工作台页面可见；摘要接口业务码成功；八项指标、风险提示和快速入口不展示电子批记录内容。',
    'MAJOR',
    '覆盖工作台摘要和模块边界。'
  ),
  (
    '智能排产-只读一致性：工作台、排产工单、排程日历',
    2,
    '排产工单列表展示分层进度字段',
    '排产工单列表接口返回当前工序、待审批、待检、超报、冻结原因等分层进度字段；页面展示总量、完成、未完和冻结状态。',
    'MAJOR',
    '覆盖排产工单列表只读字段契约。'
  ),
  (
    '智能排产-只读一致性：工作台、排产工单、排程日历',
    3,
    '排程日历展示班次、短缺和锁定状态',
    '排程日历月视图业务码成功；页面展示班次、短缺、锁定或冻结相关文本；截图记录日历状态。',
    'MAJOR',
    '覆盖日历只读一致性。'
  ),
  (
    '智能排产-只读一致性：工作台、排产工单、排程日历',
    4,
    '只读模式不得产生 MES 写请求',
    '执行期间不得出现 POST、PUT、PATCH、DELETE 到 /admin-api/mes/ 的写请求；如出现必须失败并记录请求列表。',
    'CRITICAL',
    '覆盖只读巡检安全边界。'
  ),
  (
    '智能排产-可点击安全巡检：危险写入必须显式确认',
    1,
    '智能排产页面入口全部可访问',
    '排产员工作台、排产工单、生产排产、排程日历、工艺流程排产配置、报工、璞慧排产和排产看板均能通过真实路由打开并显示目标内容。',
    'MAJOR',
    '覆盖智能排产页面入口和动态菜单可用性。'
  ),
  (
    '智能排产-可点击安全巡检：危险写入必须显式确认',
    2,
    '危险写入按钮只允许打开确认入口',
    '自动排产、手动重排、导入、发布、审批、锁定、解锁、删除等危险按钮最多打开抽屉或确认框；不得点击最终确认。',
    'CRITICAL',
    '覆盖危险操作二次确认边界。'
  ),
  (
    '智能排产-可点击安全巡检：危险写入必须显式确认',
    3,
    '安全按钮不得触发 MES 写请求',
    '搜索、查询、刷新、返回、展开、收起、分页等安全按钮点击后不得产生 MES 写请求或控制台错误。',
    'MAJOR',
    '覆盖只读按钮行为。'
  ),
  (
    '智能排产-可点击安全巡检：危险写入必须显式确认',
    4,
    '检查结果必须记录页面和按钮明细',
    '执行结果 artifact 必须包含每个页面、按钮、请求、控制台错误和被阻止危险动作明细；失败检查点必须包含差异说明和截图路径。',
    'MAJOR',
    '覆盖 Runner 回写证据完整性。'
  );

  INSERT INTO `system_codex_test_checkpoint` (
    `case_id`, `sort`, `name`, `expected_text`, `severity`, `remark`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `case_ids`.`case_id`, `seed`.`checkpoint_sort`, `seed`.`checkpoint_name`,
    `seed`.`expected_text`, `seed`.`severity`, `seed`.`remark`,
    'codex', NOW(), 'codex', NOW(), b'0', 1
    FROM `tmp_codex_smart_scheduling_checkpoint_seed` AS `seed`
    JOIN `tmp_codex_smart_scheduling_case_ids` AS `case_ids`
      ON `case_ids`.`case_name` = `seed`.`case_name`
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_codex_test_checkpoint` AS `existing`
      WHERE `existing`.`case_id` = `case_ids`.`case_id`
        AND `existing`.`sort` = `seed`.`checkpoint_sort`
        AND `existing`.`deleted` = b'0'
   );

  UPDATE `system_codex_test_checkpoint` AS `checkpoint`
  JOIN `tmp_codex_smart_scheduling_case_ids` AS `case_ids`
    ON `checkpoint`.`case_id` = `case_ids`.`case_id`
   AND `checkpoint`.`deleted` = b'0'
  JOIN `tmp_codex_smart_scheduling_checkpoint_seed` AS `seed`
    ON `seed`.`case_name` = `case_ids`.`case_name`
   AND `seed`.`checkpoint_sort` = `checkpoint`.`sort`
     SET `checkpoint`.`name` = `seed`.`checkpoint_name`,
         `checkpoint`.`expected_text` = `seed`.`expected_text`,
         `checkpoint`.`severity` = `seed`.`severity`,
         `checkpoint`.`remark` = `seed`.`remark`,
         `checkpoint`.`tenant_id` = 1,
         `checkpoint`.`updater` = 'codex',
         `checkpoint`.`update_time` = NOW();

  IF (
    SELECT COUNT(*)
      FROM `tmp_codex_smart_scheduling_checkpoint_seed` AS `seed`
      JOIN `tmp_codex_smart_scheduling_case_ids` AS `case_ids`
        ON `case_ids`.`case_name` = `seed`.`case_name`
      JOIN `system_codex_test_checkpoint` AS `checkpoint`
        ON `checkpoint`.`case_id` = `case_ids`.`case_id`
       AND `checkpoint`.`sort` = `seed`.`checkpoint_sort`
       AND `checkpoint`.`deleted` = b'0'
  ) <> (SELECT COUNT(*) FROM `tmp_codex_smart_scheduling_checkpoint_seed`) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Codex smart scheduling checkpoint seed count mismatch';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_codex_smart_scheduling_checkpoint_seed`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_codex_smart_scheduling_case_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_codex_smart_scheduling_case_seed`;
END//
DELIMITER ;

CALL ensure_system_codex_smart_scheduling_test_items();

DROP PROCEDURE IF EXISTS ensure_system_codex_smart_scheduling_test_items;
