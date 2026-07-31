# 运行本地前后端程序

## Task Goal

- 按 IntRuoyi 本地运行规则确认并运行 `int_main` 前端和后端程序。
- 固定端口：前端 `8081`，后端 `48081`。

## Milestones

- [x] 读取任务收尾、本地运行、PowerShell 编码规则。
- [x] 创建本次任务目录与初始任务记录。
- [x] 检查经验门禁并记录适用结论。
- [x] 确认前后端端口归属。
- [x] 验证前端入口与后端健康检查。
- [x] 更新验证报告和最终状态。

## Expected Verification

- 端口 `8081` 由 `E:\IntRuoyi\IntRuoyiFronted` 的 Vite 进程监听。
- 端口 `48081` 由 `E:\IntRuoyi\IntRuoyiBackend` 的 Java 后端进程监听。
- `http://127.0.0.1:8081/` 返回可访问响应。
- `http://127.0.0.1:48081/actuator/health` 返回健康响应。

## Current Status

completed

## 经验门禁

### 本地重启脚本路径门禁

- Trigger: 本地运行、重启或检查 `IntRuoyiFronted` 前端路径。
- Preflight check: 当前主工作区前端根目录必须是 `E:\IntRuoyi\IntRuoyiFronted`，不得使用旧路径 `E:\IntRuoyi\yudao-ui-admin-vue3`。
- Blocker: 若脚本报 `Missing int_main frontend path: E:\IntRuoyi\yudao-ui-admin-vue3`，必须停止该脚本路径。
- Verification: 本次未调用该脚本；端口 `8081` 进程命令行指向 `E:\IntRuoyi\IntRuoyiFronted`。
- Forbidden action: 不新建假目录、不改端口、不静默跳过前端路径检查。
- Evidence: `docs\local-runtime.md#2026-07-24-本地重启脚本路径门禁`。

### HTTP 健康检查与进程路径扫描门禁

- Trigger: PowerShell 本地 HTTP 健康检查与端口进程扫描。
- Preflight check: 后端 JSON 健康接口使用 `Invoke-RestMethod` 并断言结构化 `status`；进程扫描记录 PID 和命令行。
- Blocker: 健康状态不是 `UP`、前端 HTTP 状态不是 `200`、或端口被非主工作区进程占用。
- Verification: 后端 `BACKEND_STATUS=UP`，前端 `FRONTEND_STATUS=200`。
- Forbidden action: 不用端口替换、API-only 冒充前端验证或字节内容字符串误判。
- Evidence: `docs\powershell-preflight-lessons.md#2026-07-10 HTTP 健康检查与进程路径扫描门禁`。

## Verification Evidence

- 2026-07-24 本次复核：`8081` 已由 `node.exe` Vite 进程监听，PID `25356`，命令行位于 `E:\IntRuoyi\IntRuoyiFronted`。
- 2026-07-24 本次复核：`48081` 已由 `java.exe` 进程监听，PID `47120`，命令行位于 `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`。
- 2026-07-24 本次复核：后端健康检查 `BACKEND_STATUS=UP`。
- 2026-07-24 本次复核：前端入口检查 `FRONTEND_STATUS=200`，`FRONTEND_LENGTH=3562`。
- 初始端口检查：`8081` 已由 `node.exe` Vite 进程监听，命令行位于 `E:\IntRuoyi\IntRuoyiFronted`。
- 初始端口检查：`48081` 已由 `java.exe` 进程监听，命令行位于 `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`。
- 后端健康检查：`BACKEND_STATUS=UP`。
- 前端入口检查：`FRONTEND_STATUS=200`，`FRONTEND_LENGTH=3578`。
- 经验沉淀：已检查长期经验归宿，本次未产生超出现有 `docs\local-runtime.md` 与 `docs\powershell-preflight-lessons.md` 的新增通用经验。
- 收尾清理：`task-closeout-cleanup` preview/apply 均通过，删除项、阻塞项、警告项均为 `<none>`。
- Git 收尾：本次复核仅产生任务记录改动，准备按任务边界提交并推送。

## Blockers

- 无。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务仅确认并运行固定本地入口，不更改端口或绕过运行规则。
- `是否存在临时补丁或绕过`：否。
