# 20260724-run-shedule-local-runtime

## Task Goal

启动 `int_shedule` 分支本地前后端程序，并按分支运行端口矩阵验证：

- Frontend: `http://127.0.0.1:8021/`
- Backend: `http://127.0.0.1:48021/actuator/health`

## Milestones

- [x] 读取并记录本地运行、worktree、端口矩阵和任务收尾规则。
- [x] 检查 `8021` / `48021` 端口占用和进程归属。
- [x] 修复缺失的 BPM / ERP 同步运行时编译前置条件，并生成后端可执行 Jar。
- [x] 按用户指定 Docker 依赖端口配置本地 MySQL / Redis。
- [x] 使用分支运行脚本启动后端和前端。
- [x] 验证前端入口与后端健康检查可访问。

## Expected Verification

- `Get-NetTCPConnection -LocalPort 8021,48021` 显示端口监听归属。
- 后端健康检查 `http://127.0.0.1:48021/actuator/health` 返回可用状态。
- 前端入口 `http://127.0.0.1:8021/` 返回 HTTP 成功响应。

## Current Status

ready_for_closeout

## Verification Result

- Docker MySQL dependency: `127.0.0.1:23306` listening before backend startup.
- Docker Redis dependency: `127.0.0.1:26379` listening before backend startup.
- Backend runtime: `48021` listening, owning process `46016`.
- Backend health: `GET http://127.0.0.1:48021/actuator/health` -> HTTP `200`.
- Frontend runtime: `8021` listening, owning process `44120`.
- Frontend entry: `GET http://127.0.0.1:8021/` -> HTTP `200`.

## 经验门禁

### 本地分支运行端口门禁

- Trigger: 启动本机前端、后端、Vite、Java 服务，尤其是 `int_shedule` 分支运行态。
- Preflight check: 启动前读取 `docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md`，确认 profile 为 `int_shedule`，端口为 `8021/48021`。
- Blocker: 端口被未知进程、其他 profile 或无关程序占用时必须停止，不得强杀或换端口。
- Verification: 记录端口监听检查、启动命令、工作目录、端口、进程 ID、前端入口和后端健康检查结果。
- Forbidden action: 禁止非 `int_main` 使用 `8081/48081`，禁止静默换端口，禁止通过修改共享 `.env` 或 `application-local.yaml` 实现分支端口。
- Evidence: `docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，使用既有分支运行脚本和端口矩阵，并按用户确认的 Docker MySQL/Redis 端口连接真实本地依赖。
- `是否存在临时补丁或绕过`：否。
## E2E Homepage Debug

- [x] 使用 Playwright 真实浏览器访问 http://127.0.0.1:8021/。
- [x] 记录控制台错误、页面错误、失败网络请求和截图路径。
- [ ] 修复 `remaining.ts` 引用的 DCC 文控日志页面缺失问题。
- [ ] 复跑静态契约、类型检查和 Playwright 首页访问。

## 2026-07-25 Rerun Verification

- Docker MySQL dependency: `127.0.0.1:23306` reachable before backend startup.
- Docker Redis dependency: `127.0.0.1:26379` reachable before backend startup.
- Backend runtime: `48021` listening, owning process `31412`.
- Backend health: `GET http://127.0.0.1:48021/actuator/health` -> HTTP `200`.
- Frontend first start: `8021` listened on process `30612` but HTTP requests timed out; stopped the current task-owned Vite process.
- Frontend runtime: restarted through `scripts\runtime\start-branch-frontend.ps1 -Slot 0 -HostAddress 127.0.0.1`; `8021` listening, owning process `39436`.
- Frontend entry: `GET http://127.0.0.1:8021/` -> HTTP `200`.
- Runtime logs: `backend-runtime-20260725-080817.*.log`, `frontend-runtime-20260725-082126.*.log`.

## 2026-07-25 Commit Closeout Note

- Static contract: `node tests\e2e\dcc-controlled-file-logs-static.spec.js` -> PASS.
- Cleanup preview: keep `task.md`, `execution-log.md`, `verification-report.md`; delete only task runtime logs; no blocked or warnings.
- Cleanup apply: BLOCKED by Windows log handle on `backend-runtime-20260725-080817.stderr.log`; services were left running intentionally and not stopped during commit.
- Current status remains `ready_for_closeout` until runtime log handles are released and cleanup apply can pass.
