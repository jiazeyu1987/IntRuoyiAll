# DCC 产品识别可修错误修复

## 任务目标
- 修复 DCC 产品名称识别中可修的三类问题：错误消息字段截断、无效 DCC 基础数据候选、非业务文件进入识别候选。
- 不处理 Codex CLI 300 秒超时和 stdout 读取超时，本轮按环境/模型响应问题暂不纳入修复。
- 保持 fail-fast：无法匹配基础数据时继续明确失败，不用 fallback、mock 或默认成功掩盖。

## 经验门禁
- PowerShell / Windows shell：已读取根目录 `docs/powershell-memory.md`，中文读写显式 UTF-8，不使用 `&&`。
- 后端交付：已读取 `backend-api-delivery` 与 `backend-contract.md`，按 BDD + RED/GREEN 实施服务行为修复。
- 数据库交付：已读取 `database-schema-delivery` 与 `database-contract.md`，schema 修改必须有 migration、测试 schema 和迁移契约测试。
- 缺陷修复：已读取 `bug-regression-fix-loop` 与 `bug-contract.md`，先用失败测试复现，再做最小修复。
- 收尾清理：已读取 `task-closeout-cleanup` 与 `closeout-rules.md`，收尾先 preview，不删除正式测试、迁移和任务核心文档。
- 测试服：本轮只做本地源码与自动化验证，不远程写入、不发布、不重启测试服。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否；无基础数据匹配继续失败，只增强可追溯性和候选过滤。
- 是否从根因和长期维护角度解决：是；通过 schema 扩容、候选集过滤和候选校验错误保真解决长期分析与数据契约问题。
- 是否存在临时补丁或绕过：否。

## BDD 场景
- BDD: 长错误消息完整落账 -> Given Codex 或候选校验产生超过 512 字符的失败原因 / When 识别记录和批任务保存失败原因 / Then 系统保留足够长的失败信息，不被二次错误 `Data truncation` 覆盖。
- BDD: 非业务文件不进入产品识别 -> Given 目录下存在 `Thumbs.db` 等系统文件 / When 批量识别收集候选文件 / Then 该文件不进入识别任务，不调用 Codex，也不产生误导性无基础数据失败。
- BDD: 无效基础数据候选暴露可诊断原因 -> Given 文件名或目录规则命中停用、跨租户或非当前启用 DCC 基础数据候选 / When 识别失败 / Then 失败台账包含候选 ID、匹配类型、匹配文本和校验失败类型，便于补数据或修规则。
- BDD: Codex 超时暂不改变业务逻辑 -> Given Codex CLI 超时 / When 本轮修复后再次出现超时 / Then 仍保留明确超时失败，不纳入本轮业务修复范围。

## 里程碑
1. 建立任务文档和经验门禁。状态：已完成
2. 定位识别服务、迁移和测试。状态：已完成
3. 补 RED 测试复现三类可修问题。状态：已完成
4. 实现 schema 与识别规则修复。状态：已完成
5. 运行验证、收尾清理并提交。状态：已完成

## 修复内容
- 将 DCC 识别失败消息保存长度从 512 扩到 2048，并新增 `20260706_dcc_recognition_traceable_failure_messages.sql` 迁移。
- 批量识别建任务时过滤 `Thumbs.db`、`desktop.ini`、`~$*` 临时文件，避免系统文件进入产品识别候选。
- 无效基础数据候选仍对接口返回标准错误码文案，同时把 `projectCodeId/matchType/matchText/reason` 写入失败台账。
- 同步修复 DCC project-code schema 覆盖中缺失的 `associated_file_count` 字段。

## 验证结果
- RED：定向 Java 测试旧实现失败，覆盖候选过滤、长错误消息保留、无效候选诊断；SQL 契约测试旧实现因缺少新迁移失败。
- GREEN：`mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccControlledFileBatchRecognitionServiceTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，69 tests, 0 failures。
- GREEN：`python -X utf8 -m pytest script/tests/test_dcc_browser_batch_recognition_task_sql.py script/tests/test_dcc_sql_scripts.py -q` -> PASS，14 passed。
- GREEN：`python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file sql/mysql/20260513_dcc_base_schema.sql --sql-file sql/mysql/20260623_dcc_browser_batch_recognition_task.sql --sql-file sql/mysql/20260629_dcc_controlled_file_recognition_record.sql --sql-file sql/mysql/20260701_dcc_batch_recognition_worker_ledger_export.sql --sql-file sql/mysql/20260706_dcc_recognition_traceable_failure_messages.sql` -> PASS。
- CLOSEOUT PREVIEW：task-closeout-cleanup preview -> PASS，delete/blocked/warnings 均为 none。
- NOTE：全量 `--sql-root sql/mysql` 门禁被无关未跟踪文件 `sql/mysql/20260705_showroom_legacy_product_code_auto_confirmable_draft.sql` 缺少 release-migration metadata 阻塞；该文件不是本任务产物，未纳入提交。

## 当前状态
- 已完成：本地验证、迁移门禁（本次迁移）、收尾预览和提交均完成。