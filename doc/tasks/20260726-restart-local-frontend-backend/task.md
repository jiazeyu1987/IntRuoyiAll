# 20260726 Restart Local Frontend Backend

## Task Goal

重启本地 `int_main` 前端与后端服务，保持项目约定端口：前端 `8081`、后端 `48081`，并验证前端入口和后端健康状态。

## Milestones

- [x] 读取本地运行、端口、PowerShell 与任务收尾规则
- [x] 创建任务文档并记录适用经验门禁
- [x] 检查旧进程、固定端口与运行前置条件
- [x] 停止确认归属的旧前后端进程
- [x] 启动本地前后端服务
- [x] 验证前端入口与后端健康检查
- [ ] 完成任务收尾

## Expected Verification

- `8081` 由 `E:\IntRuoyi\IntRuoyiFronted` 的前端进程监听。
- `48081` 由 `E:\IntRuoyi\IntRuoyiBackend` 的后端进程监听。
- `http://127.0.0.1:8081/` 返回 HTTP `200`。
- `http://127.0.0.1:48081/actuator/health` 返回 `status=UP`。
- 记录旧 PID、停止依据、新 PID、启动路径和验证结果。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按固定端口和明确进程归属执行重启，不修改共享端口或数据源配置。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

### 本地重启脚本路径门禁

- Trigger: 本地重启、`restart-int-ruoyi-local.ps1`、`IntRuoyiFronted`。
- Preflight check: 前端根目录必须是 `E:\IntRuoyi\IntRuoyiFronted`；仅停止可确认属于当前 `int_main` 运行态的旧进程。
- Blocker: 若脚本或进程指向 `E:\IntRuoyi\yudao-ui-admin-vue3`、其他 worktree、其他 runtime profile 或未知路径，立即停止。
- Verification: 记录前后端端口 PID、归属判断、新进程 PID、后端 `UP` 和前端 HTTP `200`。
- Forbidden action: 不创建假目录、不随机换端口、不强杀未知进程、不静默跳过任一服务。
- Evidence: `docs/local-runtime.md#2026-07-24-本地重启脚本路径门禁`。

### 本地后端数据库凭据门禁

- Trigger: `48081` 未监听或后端日志出现数据源创建、认证或连接失败。
- Preflight check: 保持正式本地数据源配置；若旧运行态明确使用已验证 Docker MySQL 启动参数，则按相同正式参数重启且不记录凭据。
- Blocker: MySQL 不可达、认证失败或 `master` 数据源无法创建时，不得声明后端启动成功。
- Verification: 新后端 PID 属于 `E:\IntRuoyi\IntRuoyiBackend`，`/actuator/health` 返回 `UP`。
- Forbidden action: 不切换数据库、不使用 mock/空数据源、不临时修改共享凭据或端口。
- Evidence: `docs/local-runtime.md#2026-07-25-本地后端数据库凭据门禁`。

## Cleanup Keep

doc/tasks/20260726-restart-local-frontend-backend/task.md
doc/tasks/20260726-restart-local-frontend-backend/execution-log.md
doc/tasks/20260726-restart-local-frontend-backend/verification-report.md

## Verification Evidence

- 旧前端进程：PID `58060`，命令行归属 `E:\IntRuoyi\IntRuoyiFronted`，确认后停止。
- 旧后端状态：`48081` 无监听进程。
- 新前端进程：PID `55676`，Vite 命令行归属 `E:\IntRuoyi\IntRuoyiFronted`。
- 新后端进程：PID `53292`，可执行 Jar 归属 `E:\IntRuoyi\IntRuoyiBackend`，显式使用 `48081`。
- 前端入口连续三次返回 HTTP `200`。
- 后端健康检查连续三次返回 `UP`。
- 未修改共享端口、数据源配置或生产代码。
- `task-closeout-cleanup` preview/apply 均通过，无删除项、阻塞项或警告。

## Runtime Handoff

- 当前重启验证完成后，`48081` 运行态交由任务“修复 eDHR 批次处理报错”接管更新。
- 本任务不会再次停止或启动 `48081`，也不会修改该修复任务的 worktree 或任务文档。

## Closeout Status

- 并发端口规则任务已同步 `2026-07-26-branch-runtime-v3` 契约。
- 本任务未修改或暂存该并发任务的端口脚本、契约文档或任务文件。
- `scripts/preflight/branch-runtime-port-guard.ps1` 已复跑通过，最终提交/推送门禁解除。
