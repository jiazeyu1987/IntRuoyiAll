# 生产组长设备列表接口不存在

## 任务目标

修复本机生产组长工作台请求 `admin-api/mes/pro/process-pool/team-leader/team-device/list` 返回“请求地址不存在”的问题；核对前后端路由合同与 `int_main` 运行 Jar，确保已存在的正式 Controller 映射加载到 `48081`，不新增兼容路由或前端 fallback。

## 适用经验门禁

- `docs/experience-index.md` 已存在。
- 命中 `docs/local-runtime.md#2026-07-24-隔离构建-Jar-加载门禁`：源码已有接口但运行态返回“请求地址不存在”时，必须核对监听 PID、运行 Jar 和内嵌 MES 模块关键 class；未登录 `401` 不能证明路由加载。
- 当前工作区包含并发改动，不得用未验证的共享 `target` 或旧 Jar 冒充最新运行态。

## 里程碑

- [x] M1：核对前端请求路径、Controller 映射和现有合同测试。
- [x] M2：复现运行态接口不存在并确认监听 PID/Jar 归属。
- [x] M3：构建或选取可验证的新运行 Jar并重启归属明确的 `int_main` 后端。
- [x] M4：完成登录态接口与真实页面回归、证据校验和收尾清理。

## 预期验证

- 静态合同断言前端 `getTeamDeviceList` 与后端 `@GetMapping("/team-device/list")` 路径一致。
- Controller 聚焦测试覆盖 GET 路由和维护权限。
- 新运行 Jar 内的 MES 模块包含 `getTeamDeviceList` 映射。
- 登录态请求返回业务码 `0`，不再返回“请求地址不存在”。
- 真实生产组长工作台加载设备列表时无该接口错误。
- `git diff --check` 通过；未获 Git 操作授权，不执行 stage、commit、merge 或 push。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；前后端路由已一致，修复运行 Jar 与源码版本漂移。
- `是否存在临时补丁或绕过`：否；不得新增别名接口、前端空列表或隐藏 toast。

## Current Status

completed - 当前完整后端源码快照已完成 35 项目标回归、完整 Maven package、SHA-256 和内嵌 MES 路由门禁；`48081` 已由 PID `2396` 加载 `backend-latest-20260807-1919-team-device-list.jar` 且 health=`UP`。登录态设备列表返回业务码 `0`，真实生产组长工作台无“请求地址不存在”或设备列表加载错误；任务专属临时构建与验证产物已按 preview/apply 清理，保留三份核心任务记录。

## Cleanup Keep

- doc/tasks/20260807-team-device-list-endpoint-not-found/task.md
- doc/tasks/20260807-team-device-list-endpoint-not-found/execution-log.md
- doc/tasks/20260807-team-device-list-endpoint-not-found/verification-report.md

## Cleanup Candidates

- doc/tasks/20260807-team-device-list-endpoint-not-found/bug-regression-evidence.md
- doc/tasks/20260807-team-device-list-endpoint-not-found/ci-cd-evidence.md
- doc/tasks/20260807-team-device-list-endpoint-not-found/team-device-route-static.spec.cjs
- output/runtime/20260807-team-device-list-endpoint-not-found
