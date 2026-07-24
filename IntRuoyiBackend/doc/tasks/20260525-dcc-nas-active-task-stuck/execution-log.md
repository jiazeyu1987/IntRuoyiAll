# DCC NAS 转移活跃任务卡住执行日志

- BDD: 活跃 NAS 转移任务应可继续或明确失败 -> Given 已存在活跃 NAS 转移任务, When 用户再次点击同一父文件夹转移, Then 系统应阻止重复任务，同时旧任务应继续处理至完成或给出可见失败原因。
- BDD: 后端中断后的 NAS 转移恢复 -> Given 后端在 NAS 转移中断后重启, When 调度器恢复 `RUNNING` 任务, Then 未完成条目应重新进入 `WAITING` 并继续执行，已完成条目不重复导入。
- 诊断: 任务 `1` 初始状态为 `RUNNING`，`last_run_at=2026-05-25 20:19:30`，条目统计为 `COMPLETED/DIRECTORY=18`、`COMPLETED/FILE=381`、`RUNNING/FILE=1`、`WAITING/DIRECTORY=2`、`WAITING/FILE=275`。
- 诊断: 条目 `400` 卡在 `RUNNING`，路径为 `1. QMS documents/3-1 RE 可编辑/INT∕RE∕7.4-02-02（A 0）外协服务方调查评价记录表Outsourced service providers investigation and evaluation record.xlsx`。
- 诊断: 运行日志显示 `markTaskFailed` 写入 `last_failure_message` 时触发 `Data truncation: Data too long for column 'last_failure_message'`，导致任务保持活跃状态并挡住再次转移。
- RED: `mvn -pl yudao-module-dcc -am -Dtest=DccControlledFileNasTransferServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 新增长失败信息回归用例期望 `lastFailureMessage`/`lastError` 不超过 512 并带 `[truncated]`，当前实现直接写入原始异常消息。
- 修复: `DccControlledFileNasTransferServiceImpl` 在写入 `last_failure_message`、`last_error`、`failure_report_error` 前统一限制到 512 字符并追加 `[truncated]`，同时记录完整异常日志。
- 修复: 稳定 `processWaitingTasks_expandsDirectoriesAndImportsFiles` 单测，使其直接准备待调度任务，避免被 `transfer()` 的异步触发线程影响。
- GREEN: `mvn -pl yudao-module-dcc -am -Dtest=DccControlledFileNasTransferServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。
- 恢复: 重启主后端后发现旧 worktree 后端 `20260525-dcc-screenshot-implementation` 仍连接同一数据库并用旧代码调度任务 `1`；停止该旧进程后再次重启主后端。
- 验证: 启动恢复日志记录 `recoveredTaskCount(1)` 与 `recoveredItemCount(1)`；随后任务 `1` 从 `381` 个已完成文件推进到最终 `COMPLETED`。
- 验证: 最终数据库状态为任务 `1` `COMPLETED`，条目统计 `COMPLETED/DIRECTORY=49`、`COMPLETED/FILE=935`、`FAILED/FILE/submit=5`。
- 验证: 失败条目 `400/401/403/404/420` 的失败原因均为 `file_number` 字段过长，`last_error` 长度为 512 且以 `[truncated]` 结尾，任务不再卡在 `RUNNING`。
