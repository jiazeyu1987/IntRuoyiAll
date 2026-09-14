# Bug Regression Evidence

## Bug

用户在“工序开始”节点打开批记录附件负责人配置时，前端请求 `admin-api/mes/pro/route/flow-config/batch-record-attachment-owners` 返回“请求地址不存在”。

## Expected

`E:\IntRuoyi` 的 `8081` 前端应代理到同项目 `48081` 后端，并由 `MesProRouteFlowConfigController` 提供以下接口：

- `GET /admin-api/mes/pro/route/flow-config/batch-record-attachment-owners`
- `POST /admin-api/mes/pro/route/flow-config/batch-record-attachment-owners/init-defaults`
- `POST /admin-api/mes/pro/route/flow-config/batch-record-attachment-owners/save`

## Reproduction

- `rg -n "batch-record-attachment-owners|RequestMapping\(|GetMapping\(|PostMapping\(" E:\IntRuoyi\IntRuoyiBackend\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\controller\admin\pro\route\MesProRouteFlowConfigController.java` -> current source contains the three mappings.
- `Get-NetTCPConnection -LocalPort 48081 -State Listen` plus `Win32_Process` command line -> `48081` is served by `D:\IntRuoyiWorktree\codex-test-run-monitor-runtime\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`, PID `53560`.
- `Get-Content D:\IntRuoyiWorktree\codex-test-run-monitor-runtime\IntRuoyiBackend\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\controller\admin\pro\route\MesProRouteFlowConfigController.java -Encoding utf8` -> that worktree controller does not contain any `batch-record-attachment-owners` mappings.

## Root Cause

The implementation exists in `E:\IntRuoyi`, but the frontend is currently hitting a backend Jar from another worktree on the `int_main` reserved backend port `48081`. That running Jar was built from source that does not include the batch-record attachment owner endpoints, so logged-in frontend calls can return “请求地址不存在”.

## RED/GREEN

- RED: 用户真实前端路径 -> FAIL，`admin-api/mes/pro/route/flow-config/batch-record-attachment-owners` 返回“请求地址不存在”。
- RED: runtime source inspection -> FAIL，PID `53560` on `48081` belongs to `D:\IntRuoyiWorktree\codex-test-run-monitor-runtime` and lacks `batch-record-attachment-owners` controller mappings.
- GREEN: source contract check -> PASS，`E:\IntRuoyi` source contains the three backend mappings and frontend API wrapper points to `/mes/pro/route/flow-config/batch-record-attachment-owners`.
- GREEN: not executed for live runtime yet; loading the correct `E:\IntRuoyi` backend Jar on `48081` is blocked by the existing foreign worktree process.

## Verification

- `48081 /actuator/health` returned `200` / `UP`, proving a backend is running.
- `8081` is served by `E:\IntRuoyi\IntRuoyiFronted` Vite in `env.local` mode.
- `E:\IntRuoyi\IntRuoyiFronted\.env.local` sets `VITE_BASE_URL=http://127.0.0.1:48081` and `VITE_PROXY_TARGET=http://127.0.0.1:48081`.
- Current diagnosis does not require production-code changes; the required remediation is runtime alignment.

## Blockers

- `48081` is occupied by PID `53560` from `D:\IntRuoyiWorktree\codex-test-run-monitor-runtime`, not the current `E:\IntRuoyi` backend.
- Per local runtime rules, do not silently kill a different worktree process or switch ports. User confirmation is required to stop PID `53560` and restart/load the correct `E:\IntRuoyi` backend on `48081`.

## 2026-07-26 Real E2E Retry Evidence

- Runtime: 任务专用隔离 worktree `D:\IntRuoyiWorktree\route-start-batch-record-attachments-e2e` 已加载修复 Jar，后端 `48087` health=`UP`，前端 `8087` HTTP 200。
- RED: 真实 Playwright 脚本 `route-start-batch-record-attachments-real.e2e.js` 已启动，登录 `测试租户/aoteman` 时返回“账号密码不正确”。
- Blocker: 当前没有可用的 `MES_ROUTE_START_ATTACHMENT_E2E_PASSWORD`；本地默认 `.env` 登录身份是 `芋道源码/admin`，不能替代本次测试租户写入型 E2E。
- Impact: 未进入“工序开始”节点页面路径，未初始化或保存批记录附件负责人配置；真实 E2E 仍需有效测试账号密码后复跑。
