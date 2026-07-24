# DCC 批量识别已有记录策略

## 任务目标
- 给 DCC 受控浏览“识别当前文件夹及子文件夹”新增覆盖策略：跳过已经识别成功的文件，只重新识别未识别和之前识别失败的文件。
- 保留现有两种行为：默认跳过所有已有识别记录、显式覆盖全部已有产品名称/编号。
- 不处理 Codex 超时；超时仍作为环境/模型响应问题保留明确失败。

## 经验门禁
- PowerShell / Windows shell：已读取根目录 `docs/powershell-memory.md`，中文读写显式 UTF-8，不使用 `&&`。
- 经验索引：已读取 `docs/experience-index.md`，本任务命中前后端交付、数据库 schema、真实 E2E/测试门禁；本轮先做本地代码与自动化验证，不远程操作测试服。
- 后端交付：已读取 `backend-api-delivery` 与 `backend-contract.md`，按服务契约补充策略字段和行为测试。
- 前端交付：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`，批量识别弹窗只改既有用户路径和请求契约。
- 数据库交付：已读取 `database-schema-delivery` 与 `database-contract.md`，新增持久化字段需同步 migration、基线 schema、测试 schema 和 SQL 契约测试。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否；策略值不合法必须失败，不用默认成功或静默降级掩盖。
- 是否从根因和长期维护角度解决：是；将布尔覆盖开关升级为明确策略字段，并持久化到批任务。
- 是否存在临时补丁或绕过：否。

## BDD 场景
- BDD: 默认跳过所有已有识别记录 -> Given 文件已有当前识别版本的成功或失败台账 / When 用户使用默认策略发起批量识别 / Then 后端不重新调用 Codex，并按已有台账计入成功或失败。
- BDD: 跳过成功但重试失败和未识别 -> Given 同一批候选中存在成功台账、失败台账和未识别文件 / When 用户选择“跳过成功，重试失败和未识别” / Then 后端跳过成功台账，重新识别失败台账和未识别文件。
- BDD: 覆盖全部已有值 -> Given 文件已有当前识别版本的台账 / When 用户选择覆盖全部 / Then 后端重新调用 Codex 并覆盖产品名称、产品编号和项目编码。
- BDD: 策略随任务进度可见 -> Given 用户创建批量识别任务 / When 前端展示进度弹窗 / Then 展示创建任务时选择的覆盖策略文本。

## 里程碑
1. 建立任务文档和经验门禁。状态：已完成
2. 梳理前后端批量识别策略契约。状态：已完成
3. 补充失败优先重试策略测试。状态：已完成
4. 实现策略字段、迁移和界面选项。状态：已完成
5. 运行验证、收尾并提交。状态：已完成

## 实现内容
- 后端批量识别任务新增 `existingRecordPolicy` 三档策略：`SKIP_ALL_EXISTING`、`RETRY_FAILED`、`OVERWRITE_ALL`。
- `RETRY_FAILED` 会复用当前版本成功或未匹配台账，只重新调用 Codex 处理失败台账和未识别文件。
- 前端批量识别弹窗从单个覆盖勾选升级为三档单选，并在进度弹窗展示任务策略。
- 新增 `20260706_dcc_batch_recognition_existing_record_policy.sql`，并同步创建表 SQL、H2 测试 schema 和 SQL 契约测试。

## 验证结果
- RED：`mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileBatchRecognitionServiceTest,DccControlledFileBatchRecognitionControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test; python -X utf8 -m pytest script/tests/test_dcc_browser_batch_recognition_task_sql.py script/tests/test_dcc_sql_scripts.py -q` -> FAIL，旧实现缺少策略字段/常量、缺少迁移和 schema 字段。
- GREEN：`mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileBatchRecognitionServiceTest,DccControlledFileBatchRecognitionControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，22 tests, 0 failures。
- GREEN：`python -X utf8 -m pytest script/tests/test_dcc_browser_batch_recognition_task_sql.py script/tests/test_dcc_sql_scripts.py -q` -> PASS，16 passed。
- GREEN：`node tests/e2e/dcc-browser-batch-recognition-static.spec.js` -> PASS。
- GREEN：`pnpm exec eslint src/views/dcc/controlled-file/browser/index.vue src/api/dcc/controlledFile/workflow.ts tests/e2e/dcc-browser-batch-recognition-static.spec.js` -> PASS。
- GREEN：`python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file sql/mysql/20260513_dcc_base_schema.sql --sql-file sql/mysql/20260623_dcc_browser_batch_recognition_task.sql --sql-file sql/mysql/20260629_dcc_controlled_file_recognition_record.sql --sql-file sql/mysql/20260701_dcc_batch_recognition_worker_ledger_export.sql --sql-file sql/mysql/20260706_dcc_recognition_traceable_failure_messages.sql --sql-file sql/mysql/20260706_dcc_batch_recognition_existing_record_policy.sql` -> PASS。
- CLOSEOUT PREVIEW：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260706-dcc-batch-recognition-existing-record-policy --mode preview` -> PASS，delete/blocked/warnings 均为 none。
- NOTE：`pnpm exec vue-tsc --noEmit --skipLibCheck` 首次触发 Node 4GB 堆限制，提升 `NODE_OPTIONS=--max-old-space-size=8192` 后被仓库既有无关 TS 错误阻塞；错误未指向本次修改的 DCC 浏览页、workflow API 或静态合同测试。

## 当前状态
- 已完成：实现、验证、收尾预览和提交均完成。
