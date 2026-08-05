# Execution Log

## User Intent

- 用户请求：重启前后端。

## Rule Reads

- 读取 `docs\task-closeout-rules.md`：任务文档、验证、ready_for_closeout/completed 状态要求。
- 读取 `docs\local-runtime.md`：`int_main` 本地前端 `8081`、后端 `48081`，端口归属与验证要求。
- 读取 `docs\worktree-restrictions.md`：端口槽位与禁止随机换端口规则。
- 读取 `docs\powershell-memory.md`：PowerShell 编排与 dirty worktree 记录要求。
- 读取 `docs\experience-index.md`：命中本地重启、task-closeout 与 PowerShell 门禁；适用摘要已写入 `task.md`。

## BDD

- BDD: local int_main runtime restart -> Given `E:\IntRuoyi` 主工作区按 `int_main` 使用 `8081/48081`, When 前后端被安全重启, Then 后端 `/actuator/health` 返回 `UP` 且前端 `/` 返回 HTTP 200。

## Git Baseline Context

- `git status --short --branch` 显示 `int_main...origin/int_main [ahead 13]` 且已有大量并行源码、SQL、测试、任务文档改动。
- 本任务不修改业务代码；仅记录本任务文档与本地运行态操作证据。

## Runtime Evidence

- Pending.
