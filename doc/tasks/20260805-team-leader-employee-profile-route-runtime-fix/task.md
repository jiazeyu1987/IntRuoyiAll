# 生产人员档案列表接口运行态排障

## Task Goal

修复或定位本机 `int_main` 访问 `admin-api/mes/pro/process-pool/team-leader/employee-profile/list` 返回“请求地址不存在”的问题，确认源码、运行 Jar 和 48081 后端运行态是否一致。

## Milestones

- [x] 核对当前源码是否包含目标 Controller 路由和前端调用路径。
- [x] 核对 48081 监听进程、启动命令、运行 Jar 来源和归属。
- [x] 复现目标接口当前响应，判断是运行态未刷新、端口指向错误还是源码缺失。
- [x] 如确认运行态陈旧，在不影响无关进程的前提下刷新 `int_main` 后端运行态。
- [x] 复验目标接口不再返回“请求地址不存在”，并记录最终证据。

## Expected Verification

- `rg` 证明源码存在 `@GetMapping("/employee-profile/list")` 与前端 API wrapper。
- `Get-NetTCPConnection` 与 `Win32_Process` 证明 48081 进程归属。
- 目标接口复现记录能够区分未登录、业务错误与 MVC 路由不存在。
- 如需重启，`/actuator/health` 为 `UP`，并使用登录态或运行 Jar class 检查证明目标 Controller 已加载。

## Current Status

completed

运行态问题已修复并验证通过：`48081` 已切换到包含 `employee-profile/list` 路由的 hotpatch Jar，登录态调用目标接口返回业务 `code=0`。cleanup apply 已完成，仅保留正式任务记录。提交/推送收尾未执行，因为主工作区存在大量非本任务脏改动且当前分支已 ahead，需要避免混入并行任务文件。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，优先核对源码、运行 Jar 和端口归属，不通过改前端路径或默认成功掩盖。
- `是否存在临时补丁或绕过`：否。

## Experience Gate Summary

- 适用门禁：`docs/local-runtime.md#2026-07-24-隔离构建-Jar-加载门禁`。出现“请求地址不存在”时，必须先核对 `48081` PID、命令行、repo-root、运行 Jar 和内嵌 MES 模块关键 class；禁止只看 health 或未登录 `401` 就宣称 Controller 已加载。
- 适用门禁：`docs/e2e-rules.md#Worktree / int_main 运行态 URL 门禁`。融合后主运行态只能使用成对的 `8081/48081` 且端口归属 `E:\IntRuoyi`；禁止前后端 URL 指向不同运行态。
- 适用门禁：`docs/worktree-memory.md#主工作区端口被并行任务占用时的成对运行态门禁`。如主端口被无关任务或旧 Jar 占用，不能强杀或用旧 Jar 验收；需要使用已登记 worktree slot 的成对运行态，或阻塞。

## Closeout Note

- 本次只改变本机运行态和任务证据文件，没有修改业务源码。
- 任务自有构建 worktree `D:\IntRuoyiWorktree\20260805-team-leader-route-runtime-build` 已移除。
- `task-closeout-cleanup` apply 已删除临时脚本 `restart-backend-hotpatch.ps1`、`start-backend-hotpatch.ps1`、`verify-employee-profile-route-authenticated.mjs`。
- 非 Git 临时 staging 目录 `E:\IntRuoyi\output\runtime\int_main\team-leader-hotpatch-staging-20260805-203537` 的删除命令被本地策略拦截；该目录不影响运行态，hotpatch Jar 仍需保留。
