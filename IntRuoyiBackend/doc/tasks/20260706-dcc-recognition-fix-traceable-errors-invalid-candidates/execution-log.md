# 执行日志：DCC 产品识别可修错误修复

BDD: 长错误消息完整落账 -> Given Codex 或候选校验产生超过 512 字符的失败原因 / When 识别记录和批任务保存失败原因 / Then 系统保留足够长的失败信息，不被二次错误 `Data truncation` 覆盖。
BDD: 非业务文件不进入产品识别 -> Given 目录下存在 `Thumbs.db` 等系统文件 / When 批量识别收集候选文件 / Then 该文件不进入识别任务，不调用 Codex，也不产生误导性无基础数据失败。
BDD: 无效基础数据候选暴露可诊断原因 -> Given 文件名或目录规则命中停用、跨租户或非当前启用 DCC 基础数据候选 / When 识别失败 / Then 失败台账包含候选 ID、匹配类型、匹配文本和校验失败类型，便于补数据或修规则。
BDD: Codex 超时暂不改变业务逻辑 -> Given Codex CLI 超时 / When 本轮修复后再次出现超时 / Then 仍保留明确超时失败，不纳入本轮业务修复范围。

GREEN: experience-preflight -> PASS，已读取 PowerShell、经验索引、backend-api-delivery/database-schema-delivery/bug-regression-fix-loop 及其 contract；本阶段不执行测试服写入、发布或重启。

RED: mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccControlledFileBatchRecognitionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test; python -X utf8 -m pytest script/tests/test_dcc_browser_batch_recognition_task_sql.py script/tests/test_dcc_sql_scripts.py -q -> FAIL，旧实现复现 4 个 Java 断言失败和 2 个 SQL 契约失败：候选未过滤、失败消息仍截断到 512、无效候选缺少诊断、缺少新迁移。
GREEN: implementation -> PASS，已实现 2048 字段长度、无效候选失败台账详情、系统/临时文件候选过滤、schema/test schema 同步。
GREEN: mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccControlledFileBatchRecognitionServiceTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，69 tests, 0 failures, 0 errors。
GREEN: python -X utf8 -m pytest script/tests/test_dcc_browser_batch_recognition_task_sql.py script/tests/test_dcc_sql_scripts.py -q -> PASS，14 passed。
GREEN: python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file sql/mysql/20260513_dcc_base_schema.sql --sql-file sql/mysql/20260623_dcc_browser_batch_recognition_task.sql --sql-file sql/mysql/20260629_dcc_controlled_file_recognition_record.sql --sql-file sql/mysql/20260701_dcc_batch_recognition_worker_ledger_export.sql --sql-file sql/mysql/20260706_dcc_recognition_traceable_failure_messages.sql -> PASS。
GREEN: task-closeout-cleanup preview -> PASS，delete=<none>, blocked=<none>, warnings=<none>。
NOTE: full release migration policy gate with --sql-root sql/mysql -> BLOCKED by unrelated untracked draft sql/mysql/20260705_showroom_legacy_product_code_auto_confirmable_draft.sql missing release-migration metadata; not part of this task and not staged.