# 20260703 DCC 批量识别锁等待超时修复

## Current Status

completed

## 任务目标

修复 DCC 受控浏览“识别当前文件夹 + 子目录”批量识别过程中出现 `Lock wait timeout exceeded; try restarting transaction` 的问题。并发 Codex worker 应该并行识别不同文件，不能因为同时写同一任务进度行导致任务失败或把锁超时计为文件识别失败。

## 里程碑

1. 建立任务文档、经验门禁和 BDD/TDD 记录。completed
2. 定位批量识别事务边界、文件认领和任务进度热点锁根因。completed
3. 补充 RED 回归测试，证明并发 worker 不应并发争抢任务进度行。completed
4. 最小修复进度持久化策略，保持文件级识别和失败账本语义。completed
5. 运行目标验证、证据校验和提交直接改动。completed

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；命令输出和中文文件读写使用显式 UTF-8，不使用 `&&`。
- 项目经验索引：已读取 `docs/experience-index.md`；本轮命中 PowerShell、缺陷修复、后端 API、数据库持久化门禁。
- 缺陷修复：已读取 `bug-regression-fix-loop` 与 `bug-contract.md`；已先补 RED 回归再改生产代码。
- 后端 API：已读取 `backend-api-delivery` 与 `backend-contract.md`；保持批量识别任务接口、状态和错误暴露语义。
- 数据库持久化：已读取 `database-schema-delivery` 与 `database-contract.md`；本轮修事务/写入策略，不做 schema 改动。

## BDD 场景

- BDD: 并发 worker 不争抢任务进度行 -> Given 批量识别任务配置 5 个 worker 且候选文件为多个不同文件 / When 各 worker 并发完成单文件识别 / Then 单文件识别和识别账本可并发执行，但任务进度行只由协调线程按快照持久化，不因 worker 同时更新同一行产生锁等待超时。
- BDD: 文件识别失败仍记录失败账本 -> Given 某个文件识别服务在写入成功账本前失败 / When 批量任务继续处理后续文件 / Then 系统记录该文件失败账本和最后错误，并完成任务统计，不吞异常也不返回默认成功。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，修复并发 worker 对任务进度热点行的写入边界，而不是延长锁等待或重试掩盖问题。
- 是否存在临时补丁或绕过：否。

## 根因与修复

- 根因：`DccControlledFileBatchRecognitionServiceImpl` 在单个批量任务内启动多个 worker，每个 worker 完成单文件识别后都会立即调用 `taskMapper.updateById` 更新同一条 `dcc_controlled_file_batch_recognition_task` 进度行，导致高并发下形成热点行锁竞争。
- 修复：多 worker 模式改为 `ExecutorCompletionService` 收集单文件处理结果；worker 只执行文件认领、识别和失败账本写入，任务进度行由批量协调线程按完成结果统一串行持久化。
- 语义保持：单文件失败仍写失败识别账本；任务成功/失败/停止状态和现有统计字段保持原接口语义；未引入锁等待重试、降级或吞异常。

## 最终验证

- RED：`mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileBatchRecognitionServiceTest#processWaitingTasksDoesNotLetWorkersConcurrentlyUpdateTaskProgressRow" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，旧实现中 worker 并发写任务进度行，任务状态变为 `FAILED`。
- GREEN：`mvn.cmd -pl yudao-module-dcc -am clean "-Dtest=DccControlledFileBatchRecognitionServiceTest#processWaitingTasksDoesNotLetWorkersConcurrentlyUpdateTaskProgressRow" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，1 test。
- REGRESSION：`mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileBatchRecognitionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，15 tests。

## 当前阻塞

- 暂无。

## Cleanup Keep

- doc/tasks/20260703-dcc-batch-recognition-lock-timeout/backend-api-evidence.md
- doc/tasks/20260703-dcc-batch-recognition-lock-timeout/bug-regression-evidence.md