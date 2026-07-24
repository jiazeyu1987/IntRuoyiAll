# 执行日志：DCC 批量识别已有记录策略

BDD: 默认跳过所有已有识别记录 -> Given 文件已有当前识别版本的成功或失败台账 / When 用户使用默认策略发起批量识别 / Then 后端不重新调用 Codex，并按已有台账计入成功或失败。
BDD: 跳过成功但重试失败和未识别 -> Given 同一批候选中存在成功台账、失败台账和未识别文件 / When 用户选择“跳过成功，重试失败和未识别” / Then 后端跳过成功台账，重新识别失败台账和未识别文件。
BDD: 覆盖全部已有值 -> Given 文件已有当前识别版本的台账 / When 用户选择覆盖全部 / Then 后端重新调用 Codex 并覆盖产品名称、产品编号和项目编码。
BDD: 策略随任务进度可见 -> Given 用户创建批量识别任务 / When 前端展示进度弹窗 / Then 展示创建任务时选择的覆盖策略文本。

GREEN: experience-preflight -> PASS，已读取 PowerShell、经验索引、backend-api-delivery/frontend-feature-delivery/database-schema-delivery 及其 contract；本阶段不执行测试服写入、发布或重启。

RED: mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileBatchRecognitionServiceTest,DccControlledFileBatchRecognitionControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test; python -X utf8 -m pytest script/tests/test_dcc_browser_batch_recognition_task_sql.py script/tests/test_dcc_sql_scripts.py -q -> FAIL，旧实现缺少 `existingRecordPolicy` 字段、策略常量、失败台账重试逻辑和 schema 迁移。
GREEN: implementation -> PASS，已新增三档已有记录策略字段、后端执行分支、前端三档单选、批任务响应展示、新迁移和 SQL/静态契约测试。
GREEN: mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileBatchRecognitionServiceTest,DccControlledFileBatchRecognitionControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，22 tests, 0 failures, 0 errors。
GREEN: python -X utf8 -m pytest script/tests/test_dcc_browser_batch_recognition_task_sql.py script/tests/test_dcc_sql_scripts.py -q -> PASS，16 passed。
GREEN: node tests/e2e/dcc-browser-batch-recognition-static.spec.js -> PASS。
GREEN: pnpm exec eslint src/views/dcc/controlled-file/browser/index.vue src/api/dcc/controlledFile/workflow.ts tests/e2e/dcc-browser-batch-recognition-static.spec.js -> PASS。
GREEN: python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file sql/mysql/20260513_dcc_base_schema.sql --sql-file sql/mysql/20260623_dcc_browser_batch_recognition_task.sql --sql-file sql/mysql/20260629_dcc_controlled_file_recognition_record.sql --sql-file sql/mysql/20260701_dcc_batch_recognition_worker_ledger_export.sql --sql-file sql/mysql/20260706_dcc_recognition_traceable_failure_messages.sql --sql-file sql/mysql/20260706_dcc_batch_recognition_existing_record_policy.sql -> PASS。
CLOSEOUT PREVIEW: python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260706-dcc-batch-recognition-existing-record-policy --mode preview -> PASS，delete=<none>, blocked=<none>, warnings=<none>。
NOTE: pnpm exec vue-tsc --noEmit --skipLibCheck -> BLOCKED by pre-existing unrelated frontend type errors after raising Node heap; no reported error points to this task's touched DCC browser/workflow/static-contract files.
