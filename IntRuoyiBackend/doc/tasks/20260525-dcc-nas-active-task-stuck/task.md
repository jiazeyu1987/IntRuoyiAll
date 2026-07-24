# 任务：DCC NAS 转移活跃任务卡住诊断

## 任务目标

- 定位用户在 `NAS管理 -> 转移到 DCC` 提示 `nas transfer task already active: 1` 的原因。
- 判断任务 `1` 是否仍在后台继续处理、是否因调度/数据状态中断而无法继续。
- 在不重复发起转移、不绕过真实任务状态的前提下，给出恢复或修复方案。

## 前序任务检查

- 后端上一任务 `20260525-runtime-control-real-dr-flow` 状态为 `blocked`。
- 阻塞原因属于运行控制台测试服 DR 链路，不影响本次 DCC NAS 转移诊断。

## BDD 场景

- BDD: 活跃 NAS 转移任务应可继续或明确失败 -> Given 已存在活跃 NAS 转移任务, When 用户再次点击同一父文件夹转移, Then 系统应阻止重复任务，同时旧任务应继续处理至完成或给出可见失败原因。
- BDD: 后端中断后的 NAS 转移恢复 -> Given 后端在 NAS 转移中断后重启, When 调度器恢复 `RUNNING` 任务, Then 未完成条目应重新进入 `WAITING` 并继续执行，已完成条目不重复导入。

## 里程碑

- [x] M1：建立任务文档并确认前序任务不阻塞。
- [x] M2：读取任务 `1` 的任务表、条目表和运行日志。
- [x] M3：复现或解释 active 拦截的真实状态。
- [x] M4：按需要补回归测试和最小修复。
- [x] M5：完成验证、记录恢复结果和收尾。

## 预期验证

- 查询 `dcc_controlled_file_nas_transfer_task` 中任务 `1` 的 `status`、`next_check_at`、`last_run_at`、`last_failure_message`。
- 查询 `dcc_controlled_file_nas_transfer_task_item` 中任务 `1` 的 `WAITING/RUNNING/COMPLETED/FAILED` 数量。
- 若需要代码修复，先补失败回归测试，再执行 GREEN 验证。

## 当前状态

- 状态：completed
- 已完成：定位并修复 NAS 转移失败原因过长导致任务卡在 `RUNNING` 的问题；停止连接同一数据库的旧 worktree 后端干扰进程；重启主后端后任务 `1` 已恢复并处理完成。
- 最终验证：任务 `1` 状态为 `COMPLETED`；条目统计为 `COMPLETED/DIRECTORY=49`、`COMPLETED/FILE=935`、`FAILED/FILE/submit=5`；失败条目的 `last_error` 长度为 512 且以 `[truncated]` 结尾。
- 阻塞：暂无。

## 根因记录

- 任务 `1` 原始状态为 `RUNNING`，且条目 `400` 卡在 `RUNNING`；再次点击转移时被 `selectActiveTask()` 拦截，所以提示 `nas transfer task already active: 1`。
- 旧日志显示条目提交失败后，失败信息写入 `last_failure_message varchar(512)` 时触发 `Data too long for column 'last_failure_message'`，导致失败处理本身失败，任务无法落到明确终态。
- 本地同时存在一个旧 worktree 后端进程连接同一 MySQL，并继续用旧代码调度任务 `1`；停止该进程后，主后端恢复任务并完成剩余转移。
