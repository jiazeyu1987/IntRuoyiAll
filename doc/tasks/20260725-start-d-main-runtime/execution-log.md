# Execution Log

## User Intent

- 用户要求：将前端启动在 `8101`，后端启动在 `48101`，使用 `E:\IntRuoyi` 同样的 Docker MySQL `127.0.0.1:23306/ruoyi-vue-pro`，Redis `127.0.0.1:26379`。

## Rule Checks

- 已读取 `docs\local-runtime.md`。
- 已读取 `docs\branch-runtime-ports.md`。
- 已读取 `docs\task-closeout-rules.md`。
- 已读取 `docs\powershell-encoding.md`。
- 已读取 `docs\experience-index.md`。
- 已读取 `docs\powershell-memory.md`。
- 已读取 `docs\worktree-restrictions.md`。
- 已读取 `docs\backend-development.md`。
- 已读取 `docs\frontend-development.md`。

## Milestone Evidence

- `BASELINE: git commit 7c21f74d -> saved pre-existing D-Main runtime port contract changes before this task`
- `GREEN: experience-preflight -> PASS, D-Main runtime gate recorded in task.md`
- `GREEN: scripts\preflight\branch-runtime-port-guard.ps1 -> PASS, int_main/int_main_d frontend 8101 backend 48101`
- `GREEN: port-preflight -> PASS, 8101/48101 NotListening; 23306/26379 Listen after starting int-ruoyi-mysql and int-ruoyi-redis`
- `RED: mvn.cmd -pl yudao-server -am -DskipTests package -> FAIL, yudao-module-bpm missing cn.iocoder.yudao.module.bpm.formcenter.runtime`
- `GREEN: sync-bpm-formcenter-runtime -> PASS, copied 4 same-source files from E:\IntRuoyi`
- `RED: mvn.cmd -pl yudao-server -am -DskipTests package -> FAIL, yudao-module-erp missing cn.iocoder.yudao.module.erp.service.sync.runtime`
- `GREEN: sync-erp-kingdee-runtime -> PASS, copied 6 same-source files from E:\IntRuoyi`
- `GREEN: mvn.cmd -pl yudao-server -am -DskipTests package -> PASS, BUILD SUCCESS, yudao-server-exec.jar generated`
- `GREEN: backend-start -> PASS, PID 29624 listening on 48101 with MySQL 23306 and Redis 26379 command-line overrides`
- `GREEN: backend-health -> PASS, http://127.0.0.1:48101/actuator/health status UP`
- `RED: frontend-start -> FAIL, vite command not found before local node_modules install`
- `GREEN: pnpm install --frozen-lockfile --reporter append-only -> PASS, dependencies installed including vite 5.1.4`
- `GREEN: frontend-start -> PASS, PID 43336 listening on 8101 via branch-main-d mode`
- `GREEN: frontend-http -> PASS, http://127.0.0.1:8101/ returned HTTP 200 OK`

## BDD / TDD

- `BDD: D-Main local runtime starts on isolated ports -> Given D:\ProjectPackage\IntRuoyi\IntRuoyiAll is int_main_d and Docker MySQL/Redis are available on 23306/26379, When the backend and frontend are started through the branch runtime profile, Then backend health is UP on 48101 and frontend returns HTTP 200 on 8101.`
- `RED: mvn.cmd -pl yudao-server -am -DskipTests package -> FAIL, expected reason: missing source packages prevented yudao-server Jar generation.`
- `GREEN: mvn.cmd -pl yudao-server -am -DskipTests package -> PASS after synchronizing same-source missing runtime packages.`
- `GREEN: runtime verification -> PASS, backend health UP and frontend HTTP 200.`