# 停止本地前后端程序

## Task Goal

- 按 IntRuoyi 本地运行规则停止 `int_main` 前端和后端程序。
- 固定端口：前端 `8081`，后端 `48081`。

## Milestones

- [x] 读取本地运行、worktree、任务收尾、PowerShell 编码规则。
- [x] 创建本次任务目录与初始任务记录。
- [x] 确认前后端端口归属。
- [x] 停止确认为本项目的前端和后端进程。
- [x] 验证 `8081` 和 `48081` 不再监听。
- [x] 更新验证报告和最终状态。

## Expected Verification

- 停止前，端口 `8081` 由 `E:\IntRuoyi\IntRuoyiFronted` 的 Vite 进程监听。
- 停止前，端口 `48081` 由 `E:\IntRuoyi\IntRuoyiBackend` 的 Java 后端进程监听。
- 停止后，端口 `8081` 和 `48081` 无监听进程。

## Current Status

completed

## 经验门禁

### 本地端口归属门禁

- Trigger: 停止本机 `8081` / `48081` 前后端服务。
- Preflight check: 停止前记录 PID、进程名、命令行，并确认命令行属于当前 `int_main` 主工作区前后端路径。
- Blocker: 端口被未知进程、非 IntRuoyi 进程或其他 runtime profile 占用。
- Verification: 停止后重新检查 `8081` / `48081` 监听状态。
- Forbidden action: 不强杀未知进程、不换端口、不静默跳过服务。
- Evidence: `docs\local-runtime.md`、`docs\worktree-restrictions.md`。

## Verification Evidence

- 停止前端口检查：`8081` 由 PID `25356`、`node.exe` 监听，命令行位于 `E:\IntRuoyi\IntRuoyiFronted`。
- 停止前端口检查：`48081` 由 PID `47120`、`java.exe` 监听，命令行位于 `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`。
- 停止动作：已停止 PID `25356` 和 PID `47120`。
- 停止后端口复查：`8081` 为 `FREE`。
- 停止后端口复查：`48081` 为 `FREE`。
- 收尾清理：`task-closeout-cleanup` preview/apply 均通过，删除项、阻塞项、警告项均为 `<none>`。

## Blockers

- 无。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务仅停止已确认归属的固定本地运行态。
- `是否存在临时补丁或绕过`：否。