-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260724_system_codex_test_management; type=seed; riskLevel=low
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_dcc_codex_test_items_seed;
DELIMITER //
CREATE PROCEDURE ensure_dcc_codex_test_items_seed()
BEGIN
  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_codex_test_case_seed`;
  CREATE TEMPORARY TABLE `tmp_dcc_codex_test_case_seed` (
    `tenant_id` bigint NOT NULL,
    `name` varchar(128) NOT NULL,
    `project` varchar(16) NOT NULL,
    `method_text` text NOT NULL,
    `test_data_text` text NOT NULL,
    `default_execution_mode` varchar(16) NOT NULL,
    `parallel_safe` bit NOT NULL,
    `status` varchar(16) NOT NULL,
    `sort` int NOT NULL,
    `checkpoint_count` int NOT NULL,
    PRIMARY KEY (`tenant_id`, `name`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO `tmp_dcc_codex_test_case_seed` (
    `tenant_id`, `name`, `project`, `method_text`, `test_data_text`, `default_execution_mode`,
    `parallel_safe`, `status`, `sort`, `checkpoint_count`
  )
  VALUES
  (
    1,
    '智能文控受控文件上传审批发布闭环',
    '文控',
    CONCAT(
      'a. 通过真实前端进入 /dcc/controlled-file/upload，使用任务自有目录、分类、项目代码和源文件发起受控文件上传。', CHAR(10),
      'b. 在上传页面完成预览、元数据校验、分发/培训配置确认并提交审批，不得使用 API-only 直塞。', CHAR(10),
      'c. 通过 /dcc/controlled-file/approval-tasks 按页面待办完成审批流转，确认发布后进入 /dcc/controlled-file/browser。', CHAR(10),
      'd. 打开 /dcc/controlled-file/logs 核对 FILE_UPLOAD、FILE_APPROVAL、FILE_DISTRIBUTION 或 TRAINING_EXECUTION 日志。'
    ),
    CONCAT(
      '任务自有数据：文件编号前缀 Codex-DCC-UPLOAD-${runId}；目录、分类、项目代码、上传文件和审批用户均由 Runner 创建或明确复用测试基线。', CHAR(10),
      'parallelSafe=false；不得使用生产文件、admin 基线文档或无清理责任的真实业务记录。'
    ),
    'SEQUENTIAL',
    b'0',
    'ENABLE',
    2201,
    4
  ),
  (
    1,
    '智能文控受控文件修订版本链闭环',
    '文控',
    CONCAT(
      'a. 在 /dcc/controlled-file/browser 按任务自有文件编号定位已发布 ACTIVE 文件，并从详情页发起 REVISION 修订。', CHAR(10),
      'b. 上传新源文件或新版输出物，填写修订原因，提交审批并在 /dcc/controlled-file/approval-tasks 完成审批。', CHAR(10),
      'c. 回到受控浏览和详情页核对 master 当前版本、版本号、旧版状态和新版 ACTIVE 状态。', CHAR(10),
      'd. 在 /dcc/controlled-file/logs 按文件编号查询修订、审批和版本链日志。'
    ),
    CONCAT(
      '任务自有数据：基础文件前缀 Codex-DCC-REVISION-${runId}；修订原因包含 runId；只允许修改本任务创建的受控文件链。', CHAR(10),
      'parallelSafe=false；不得改用 API-only 更新版本链或直接 SQL 修改 current_active_controlled_file_id。'
    ),
    'SEQUENTIAL',
    b'0',
    'ENABLE',
    2202,
    4
  ),
  (
    1,
    '智能文控作废审批与受控浏览收敛',
    '文控',
    CONCAT(
      'a. 在 /dcc/controlled-file/browser 按任务自有文件编号定位 ACTIVE 文件，进入详情页发起 OBSOLETE 作废。', CHAR(10),
      'b. 填写作废原因并通过 /dcc/controlled-file/approval-tasks 完成作废审批，不得绕过 BPM 或表单策略。', CHAR(10),
      'c. 回到受控浏览确认作废文件不再作为可下载 ACTIVE 文件展示，详情页展示作废状态、原因和时间。', CHAR(10),
      'd. 在 /dcc/controlled-file/logs 核对 FILE_OBSOLETE 与审批日志可追溯。'
    ),
    CONCAT(
      '任务自有数据：文件编号前缀 Codex-DCC-OBSOLETE-${runId}；作废原因包含 runId；只处理本任务创建的 ACTIVE 文件。', CHAR(10),
      'parallelSafe=false；不得使用生产文件，不得通过数据库直接改状态。'
    ),
    'SEQUENTIAL',
    b'0',
    'ENABLE',
    2203,
    4
  ),
  (
    1,
    '智能文控受控浏览下载水印与访问日志',
    '文控',
    CONCAT(
      'a. 使用具备文控权限的测试账号进入 /dcc/controlled-file/browser，按任务自有分类和文件编号搜索受控文件。', CHAR(10),
      'b. 打开只读预览页，核对下载入口、只读水印、权限过滤和可见文件范围。', CHAR(10),
      'c. 在页面完成受控下载或下载阻断验证，确认无权限账号看不到或无法下载目标文件。', CHAR(10),
      'd. 打开 /dcc/controlled-file/logs 核对访问、预览、下载、水印追踪记录包含用户、文件编号和 runId。'
    ),
    CONCAT(
      '任务自有数据：文件编号前缀 Codex-DCC-BROWSE-${runId}；授权账号与无权账号由 Runner 明确标记；下载文件仅用于本任务验证。', CHAR(10),
      'parallelSafe=false；不得使用生产文件，不得用 API-only 代替真实预览和下载入口。'
    ),
    'SEQUENTIAL',
    b'0',
    'ENABLE',
    2204,
    4
  ),
  (
    1,
    '智能文控分发培训闭环',
    '文控',
    CONCAT(
      'a. 在 /dcc/controlled-file/categories 为任务自有分类配置分发规则和培训规则，或复用已确认的测试分类。', CHAR(10),
      'b. 通过 /dcc/controlled-file/upload 发布需要分发和培训的受控文件，确认发布后生成分发和培训任务。', CHAR(10),
      'c. 使用接收人真实页面完成分发签收，并通过 /dcc/controlled-file/training-mine 完成培训确认或培训记录上传。', CHAR(10),
      'd. 在文件详情和 /dcc/controlled-file/logs 核对分发、培训进度和日志状态。'
    ),
    CONCAT(
      '任务自有数据：分类前缀 Codex-DCC-TRAINING-${runId}；接收人、培训人、文件编号和培训记录文件均需可追踪可清理。', CHAR(10),
      'parallelSafe=false；不得污染生产培训任务或使用无授权真实用户。'
    ),
    'SEQUENTIAL',
    b'0',
    'ENABLE',
    2205,
    4
  ),
  (
    1,
    '智能文控项目代码识别分配闭环',
    '文控',
    CONCAT(
      'a. 进入 /dcc/controlled-file/basic-data 的项目代码或识别任务入口，创建任务自有项目代码、别名和测试目录映射。', CHAR(10),
      'b. 上传或选择任务自有受控文件，触发项目代码识别任务并等待识别结果可见。', CHAR(10),
      'c. 在识别结果页复核候选项目代码并完成文件分配，确认受控浏览和项目代码文件清单同步。', CHAR(10),
      'd. 在项目代码审计或 /dcc/controlled-file/logs 核对识别、复核、分配变更记录。'
    ),
    CONCAT(
      '任务自有数据：项目代码前缀 Codex-DCC-PCODE-${runId}；识别文件、别名、目录映射和分配记录只属于本任务。', CHAR(10),
      'parallelSafe=false；不得使用生产项目代码，不得用离线识别或 API-only 结果冒充页面闭环。'
    ),
    'SEQUENTIAL',
    b'0',
    'ENABLE',
    2206,
    4
  );

  IF (SELECT COUNT(*) FROM `tmp_dcc_codex_test_case_seed`) <> 6 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'DCC_CODEX_TEST_ITEMS_SEED_CASE_COUNT_MISMATCH';
  END IF;

  INSERT INTO `system_codex_test_case` (
    `name`, `project`, `method_text`, `test_data_text`, `default_execution_mode`, `parallel_safe`, `status`, `sort`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `seed`.`name`, `seed`.`project`, `seed`.`method_text`, `seed`.`test_data_text`, `seed`.`default_execution_mode`,
    `seed`.`parallel_safe`, `seed`.`status`, `seed`.`sort`, 'codex', NOW(), 'codex', NOW(), b'0', `seed`.`tenant_id`
    FROM `tmp_dcc_codex_test_case_seed` AS `seed`
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_codex_test_case` AS `existing`
      WHERE `existing`.`tenant_id` = `seed`.`tenant_id`
        AND `existing`.`name` = `seed`.`name`
        AND `existing`.`deleted` = b'0'
   );

  UPDATE `system_codex_test_case` AS `target`
  JOIN `tmp_dcc_codex_test_case_seed` AS `seed`
    ON `seed`.`tenant_id` = `target`.`tenant_id`
   AND `seed`.`name` = `target`.`name`
    SET `target`.`project` = `seed`.`project`,
       `target`.`method_text` = `seed`.`method_text`,
       `target`.`test_data_text` = `seed`.`test_data_text`,
       `target`.`default_execution_mode` = `seed`.`default_execution_mode`,
       `target`.`parallel_safe` = `seed`.`parallel_safe`,
       `target`.`status` = `seed`.`status`,
       `target`.`sort` = `seed`.`sort`,
       `target`.`updater` = 'codex',
       `target`.`update_time` = NOW()
 WHERE `target`.`deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_codex_test_checkpoint_seed`;
  CREATE TEMPORARY TABLE `tmp_dcc_codex_test_checkpoint_seed` (
    `tenant_id` bigint NOT NULL,
    `case_name` varchar(128) NOT NULL,
    `sort` int NOT NULL,
    `name` varchar(128) NOT NULL,
    `expected_text` text NOT NULL,
    `severity` varchar(16) NOT NULL,
    `remark` varchar(512) NULL,
    PRIMARY KEY (`tenant_id`, `case_name`, `sort`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO `tmp_dcc_codex_test_checkpoint_seed` (
    `tenant_id`, `case_name`, `sort`, `name`, `expected_text`, `severity`, `remark`
  )
  VALUES
  (1, '智能文控受控文件上传审批发布闭环', 1, '上传预览和提交', '上传页必须通过真实页面完成文件预览、元数据校验和提交，提交结果返回业务成功且生成任务自有文件编号。', 'CRITICAL', 'DCC upload path'),
  (1, '智能文控受控文件上传审批发布闭环', 2, '审批任务流转', '审批任务列表中必须出现任务自有文件待办，审批后文件状态进入已发布或受控可浏览状态。', 'CRITICAL', 'DCC approval path'),
  (1, '智能文控受控文件上传审批发布闭环', 3, '受控浏览可见', '受控浏览页按文件编号能看到且只能看到任务自有发布文件，详情页版本、分类、目录和状态一致。', 'MAJOR', 'DCC browser state'),
  (1, '智能文控受控文件上传审批发布闭环', 4, '日志追溯完整', '文控日志必须包含上传、审批、发布、分发或培训相关记录，记录中包含文件编号、操作者和任务 runId。', 'MAJOR', 'DCC audit log'),

  (1, '智能文控受控文件修订版本链闭环', 1, '发起修订', '从受控浏览或详情页发起修订，页面必须保留原文件链并生成任务自有新版草稿或审批流程。', 'CRITICAL', 'DCC revision start'),
  (1, '智能文控受控文件修订版本链闭环', 2, '版本链更新', '修订审批完成后 master 当前版本指向新版，版本号递增，旧版与新版链路可在详情页追溯。', 'CRITICAL', 'DCC revision chain'),
  (1, '智能文控受控文件修订版本链闭环', 3, '旧版受控收敛', '旧版不得继续作为当前 ACTIVE 下载入口暴露，新版成为受控浏览默认可见版本。', 'MAJOR', 'DCC old version state'),
  (1, '智能文控受控文件修订版本链闭环', 4, '修订日志可追溯', '日志页必须能按文件编号查到修订提交、审批和版本切换记录。', 'MAJOR', 'DCC revision log'),

  (1, '智能文控作废审批与受控浏览收敛', 1, '发起作废', '详情页发起作废时必须填写原因并进入正式审批流，不得直接修改状态或跳过表单策略。', 'CRITICAL', 'DCC obsolete start'),
  (1, '智能文控作废审批与受控浏览收敛', 2, '作废审批完成', '审批任务完成后文件详情展示作废状态、作废原因、作废时间和操作者。', 'CRITICAL', 'DCC obsolete approval'),
  (1, '智能文控作废审批与受控浏览收敛', 3, '浏览与下载收敛', '受控浏览默认列表和下载入口不得继续暴露已作废文件作为 ACTIVE 文件。', 'MAJOR', 'DCC obsolete visibility'),
  (1, '智能文控作废审批与受控浏览收敛', 4, '作废日志可追溯', '文控日志必须记录 FILE_OBSOLETE、审批动作和作废原因，且能按任务文件编号检索。', 'MAJOR', 'DCC obsolete log'),

  (1, '智能文控受控浏览下载水印与访问日志', 1, '权限过滤正确', '有权账号只能看到授权范围内的任务文件，无权账号搜索同一文件编号时不可见或被明确拒绝。', 'CRITICAL', 'DCC permission filter'),
  (1, '智能文控受控浏览下载水印与访问日志', 2, '预览只读', '预览页必须以只读方式打开任务文件，页面展示水印或只读标识，不能暴露编辑入口。', 'MAJOR', 'DCC readonly preview'),
  (1, '智能文控受控浏览下载水印与访问日志', 3, '下载与水印受控', '下载动作必须按权限、原因或水印策略受控，成功下载或阻断都要展示明确结果。', 'CRITICAL', 'DCC download guard'),
  (1, '智能文控受控浏览下载水印与访问日志', 4, '访问日志完整', '访问、预览、下载或阻断日志必须包含用户、文件编号、操作类型和任务 runId。', 'MAJOR', 'DCC access log'),

  (1, '智能文控分发培训闭环', 1, '分发任务生成', '发布需要分发的任务自有文件后，分发对象列表或详情页必须生成对应待签收记录。', 'CRITICAL', 'DCC distribution create'),
  (1, '智能文控分发培训闭环', 2, '培训任务生成', '发布需要培训的任务自有文件后，培训对象或我的培训页必须出现对应培训任务。', 'CRITICAL', 'DCC training create'),
  (1, '智能文控分发培训闭环', 3, '签收培训完成', '接收人通过真实页面完成分发签收和培训确认或记录上传后，文件详情进度更新为完成。', 'MAJOR', 'DCC user completion'),
  (1, '智能文控分发培训闭环', 4, '分发培训日志完整', '文控日志必须记录分发签收、培训确认或培训记录上传，包含文件编号、接收人和任务 runId。', 'MAJOR', 'DCC distribution training log'),

  (1, '智能文控项目代码识别分配闭环', 1, '识别任务创建', '项目代码或识别任务入口必须能基于任务自有文件创建识别任务，列表显示任务编号和待处理状态。', 'CRITICAL', 'DCC project code recognition task'),
  (1, '智能文控项目代码识别分配闭环', 2, '识别结果可复核', '识别结果页必须显示候选项目代码、置信信息或失败原因，不得以空成功掩盖识别失败。', 'CRITICAL', 'DCC project code review'),
  (1, '智能文控项目代码识别分配闭环', 3, '项目代码分配生效', '复核确认后，项目代码文件清单和受控浏览元数据必须体现任务自有项目代码分配。', 'MAJOR', 'DCC project code assignment'),
  (1, '智能文控项目代码识别分配闭环', 4, '识别审计可追溯', '项目代码审计或文控日志必须记录识别、复核、分配变更，包含项目代码、文件编号和操作者。', 'MAJOR', 'DCC project code audit');

  INSERT INTO `system_codex_test_checkpoint` (
    `case_id`, `sort`, `name`, `expected_text`, `severity`, `remark`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT
    `test_case`.`id`, `seed`.`sort`, `seed`.`name`, `seed`.`expected_text`, `seed`.`severity`, `seed`.`remark`,
    'codex', NOW(), 'codex', NOW(), b'0', `seed`.`tenant_id`
    FROM `tmp_dcc_codex_test_checkpoint_seed` AS `seed`
    JOIN `system_codex_test_case` AS `test_case`
      ON `test_case`.`tenant_id` = `seed`.`tenant_id`
     AND `test_case`.`name` = `seed`.`case_name`
     AND `test_case`.`deleted` = b'0'
   WHERE NOT EXISTS (
     SELECT 1
       FROM `system_codex_test_checkpoint` AS `existing`
      WHERE `existing`.`case_id` = `test_case`.`id`
        AND `existing`.`sort` = `seed`.`sort`
        AND `existing`.`deleted` = b'0'
   );

  UPDATE `system_codex_test_checkpoint` AS `target`
  JOIN `system_codex_test_case` AS `test_case`
    ON `test_case`.`id` = `target`.`case_id`
   AND `test_case`.`deleted` = b'0'
  JOIN `tmp_dcc_codex_test_checkpoint_seed` AS `seed`
    ON `seed`.`tenant_id` = `test_case`.`tenant_id`
   AND `seed`.`case_name` = `test_case`.`name`
   AND `seed`.`sort` = `target`.`sort`
   SET `target`.`name` = `seed`.`name`,
       `target`.`expected_text` = `seed`.`expected_text`,
       `target`.`severity` = `seed`.`severity`,
       `target`.`remark` = `seed`.`remark`,
       `target`.`tenant_id` = `seed`.`tenant_id`,
       `target`.`updater` = 'codex',
       `target`.`update_time` = NOW()
 WHERE `target`.`deleted` = b'0';

  IF EXISTS (
    SELECT 1
      FROM `tmp_dcc_codex_test_case_seed` AS `seed`
      JOIN `system_codex_test_case` AS `test_case`
        ON `test_case`.`tenant_id` = `seed`.`tenant_id`
       AND `test_case`.`name` = `seed`.`name`
       AND `test_case`.`deleted` = b'0'
      LEFT JOIN (
        SELECT `case_id`, COUNT(*) AS `checkpoint_count`
          FROM `system_codex_test_checkpoint`
         WHERE `deleted` = b'0'
         GROUP BY `case_id`
      ) AS `checkpoint`
        ON `checkpoint`.`case_id` = `test_case`.`id`
     WHERE COALESCE(`checkpoint`.`checkpoint_count`, 0) < `seed`.`checkpoint_count`
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'DCC_CODEX_TEST_ITEMS_SEED_CHECKPOINT_MISSING';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_codex_test_checkpoint_seed`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_dcc_codex_test_case_seed`;
END//
DELIMITER ;

CALL ensure_dcc_codex_test_items_seed();

DROP PROCEDURE IF EXISTS ensure_dcc_codex_test_items_seed;
