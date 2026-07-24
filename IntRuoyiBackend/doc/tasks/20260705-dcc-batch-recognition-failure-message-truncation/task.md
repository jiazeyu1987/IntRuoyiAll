# DCC 批量识别失败原因截断修复

## 任务目标

- 修复 DCC 批量识别进度中 `failure_message` 因异常文本过长导致账本入库失败的问题。
- 保持失败原因可追踪：记录真实根因的可读前缀，不吞异常、不伪造成功、不引入 fallback。

## 里程碑

1. 建立任务文档、经验门禁与缺陷契约 - 已完成
2. 复现超长失败原因写入账本风险 - 已完成
3. 增加 RED 回归测试并最小修复 - 已完成
4. 运行目标回归验证并记录证据 - 已完成
5. 收尾清理预览与任务完成记录 - 已完成

## 经验门禁

- PowerShell / Windows shell / 中文编码：本任务已先读取 `docs/powershell-memory.md`，后续中文文件读取使用 `Get-Content -Encoding utf8`、`python -X utf8` 或 `apply_patch`，不使用默认编码重定向。
- 真实 E2E / 服务器 / 数据库写入：本轮先做后端单元回归，不执行真实 E2E、服务器写入、发布、备份、恢复或数据库 schema 修改；如后续需要高风险动作，必须先在 `execution-log.md` 记录 `GREEN: experience-preflight -> PASS`。

## BDD 场景

- `BDD: 批量识别失败原因过长时账本仍可保存 -> Given 批量识别中的单文件项目码识别抛出超过 failure_message 列宽的真实异常 / When 服务保存失败识别账本 / Then 失败账本保留可读原因且长度不超过 512，不再触发 Data truncation。`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。仅对入库文本按列契约截断，原业务异常继续向上抛出并由批量进度计失败。
- `是否从根因和长期维护角度解决`：是。统一项目码识别失败账本的字段长度契约，避免同类超长异常绕过任务进度截断保护。
- `是否存在临时补丁或绕过`：否。

## 预期验证

- RED：目标单测先失败，证明当前失败账本 `failureMessage` 可超过 512。
- GREEN：目标单测通过，证明失败账本写入前统一截断。
- REGRESSION：运行 DCC 批量识别与项目码识别相关目标测试。

## 当前状态

- 状态：completed
- 2026-07-05：已定位根因：`DccControlledFileBatchRecognitionServiceImpl` 的任务进度 `lastFailureMessage` 已截断，但 `DccControlledFileProjectCodeRecognitionServiceImpl` 捕获异常后写入识别账本 `failureMessage` 时仍使用原始异常文本。
- 2026-07-05：已在项目码识别失败账本入口统一按 512 字符字段契约截断，目标 RED/GREEN 与相邻 DCC 回归通过。
- 2026-07-05：收尾清理预览通过，无需删除临时产物；任务完成。

## 缺陷证据

- 根因：项目码识别服务失败路径直接保存超长异常文本到 `dcc_controlled_file_recognition_record.failure_message`，绕过了批量任务进度字段已有的截断保护。
- 修复：`DccControlledFileProjectCodeRecognitionServiceImpl#resolveFailureMessage` 写账本前统一调用 512 字符截断。
- RED：`mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest#recognizeProjectCode_truncatesLongFailureMessageBeforePersistingRecord" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`failureMessage.length() <= 512` 断言失败。
- GREEN：同一命令 -> PASS，1 test。
- REGRESSION：`mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccControlledFileBatchRecognitionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，43 tests。

## Cleanup Keep

- `doc/tasks/20260705-dcc-batch-recognition-failure-message-truncation/bug-regression-evidence.md`
