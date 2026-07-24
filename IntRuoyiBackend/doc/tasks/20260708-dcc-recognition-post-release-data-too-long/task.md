# Task: DCC 产品名称识别 status 截断 post-release 修复

## 任务目标

修复测试服 DCC 产品名称识别在 `release-20260708-dcc-recognition-fix-4` 发布后仍新增的 `Data truncation: Data too long for column 'status'`，用真实日志、数据库时间基准、回归测试和测试服后验审计证明问题是否仍存在及是否已修复。

## 里程碑

- [x] M1：补读 PowerShell、服务器访问、发布恢复和 bug regression 门禁。
- [x] M2：用测试服真实日志和数据库记录定位剩余截断写入路径。
- [x] M3：补充会先失败的回归测试，证明长状态不得进入 `dcc_controlled_file_recognition_record.status`。
- [x] M4：实施最小根因修复，禁止非白名单识别记录状态入库。
- [x] M5：运行 targeted DCC 单测并记录 RED/GREEN。
- [x] M6：如代码变更，构建发布新测试服版本并以后端启动时间为基准审计无新增截断。

## 预期验证

- `mvn.cmd -pl yudao-module-dcc "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccControlledFileBatchRecognitionServiceTest" test` 通过。
- 测试服后端健康检查通过。
- 新版本后端启动时间之后，`dcc_controlled_file_recognition_record.failure_message LIKE '%Data too long for column ''status''%'` 无新增记录。

## 经验门禁

- PowerShell：中文读写、远端 SSH/MySQL 多层命令必须显式 UTF-8，避免 PowerShell 文本管道污染；复杂远端命令优先脚本化并后置断言。
- 服务器访问：测试服目标固定 `172.30.30.58`，运行目录 `/opt/intruoyi/runtime`，远端动作需确认目标容器、目标库和健康状态。
- 发布恢复：测试服是正式服前置筛选器；发布后不能只看健康检查，必须校验镜像 tag、manifest/schema 契约和本任务核心业务路径。
- Bug regression：必须先复现/隔离根因，补失败回归测试，再做最小修复并记录 RED/GREEN。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；本任务目标是禁止不合法状态入库并保留明确失败信息。
- 是否从根因和长期维护角度解决：是；计划在识别记录持久化边界统一约束数据库安全状态。
- 是否存在临时补丁或绕过：否。

## 当前状态

COMPLETED。已构建并发布测试服版本 `release-20260708-dcc-status-guard-v3-e1bd69ce96`；测试服后端容器启动时间 `2026-07-08T10:08:30.462225196Z` 之后，真实库审计 `dcc_controlled_file_recognition_record.failure_message LIKE '%Data too long for column ''status''%'` 新增数为 0。根因修复、targeted 回归、发布门禁、测试服健康检查和后验审计均已完成。
