# Execution Log

## Intent

- User requested: 运行前后端程序。
- Runtime profile: `int_shedule`.
- Required ports: frontend `8021`, backend `48021`.
- User confirmed Docker dependencies: MySQL `127.0.0.1:23306/ruoyi-vue-pro`, Redis `127.0.0.1:26379`.

## Rule Reads

- Read `docs/local-runtime.md`.
- Read `docs/task-closeout-rules.md`.
- Read `docs/worktree-restrictions.md`.
- Read `docs/branch-runtime-ports.md`.
- Read `docs/experience-index.md`.
- Read `docs/backend-development.md`.
- Read `docs/powershell-memory.md`.
- Read `docs/powershell-encoding.md`.
- Read `docs/database-rules.md`.
- Read `docs/server-access.md`.

## BDD / TDD Notes

- BDD: Local runtime starts on branch ports -> Given current branch is `int_shedule`, Docker MySQL/Redis dependencies are listening on the user-confirmed ports, and ports `8021/48021` are free, When backend and frontend are started through `scripts/runtime/start-branch-*.ps1`, Then backend health and frontend entry respond on the branch ports.
- BDD: Missing sync runtime implementation blocks packaging -> Given BPM, ERP, and MES code imports the form-center and Kingdee runtime contracts, When the server reactor packages, Then each runtime contract is implemented and compilation succeeds without fallback behavior.
- RED: `mvn.cmd -pl yudao-server -am -DskipTests package` -> FAIL, missing BPM form-center runtime package and then missing ERP Kingdee sync runtime package.
- GREEN: `mvn.cmd -pl yudao-module-bpm -am -DskipTests compile` -> PASS.
- GREEN: `mvn.cmd -pl yudao-module-erp -am -DskipTests compile` -> PASS.
- GREEN: `mvn.cmd -pl yudao-module-erp "-Dtest=KingdeePurchaseOrderSyncJobTest,KingdeeSaleOrderSyncJobTest,KingdeeStockSyncJobTest" test` -> PASS, 9 tests passed.
- GREEN: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS; generated `IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`.

## Milestone Updates

- completed: Rule reads completed; task record created; branch profile confirmed as `int_shedule` with frontend `8021` and backend `48021`.
- completed: Port preflight returned no listeners for `8021` and `48021` before startup.
- completed: Installed frontend dependencies with `pnpm install`.
- completed: Restored missing BPM form-center runtime classes and ERP Kingdee synchronization runtime classes required by the current source tree. The runtime implementation persists run and watermark state, records failures, and rethrows exceptions; no fallback or mock-success behavior was added.
- completed: Updated current branch local backend config to use Docker MySQL `127.0.0.1:23306/ruoyi-vue-pro` and Redis `127.0.0.1:26379` per user confirmation.
- completed: Rebuilt `yudao-server-exec.jar` after config change.
- completed: Verified dependency listeners before startup: Docker MySQL `23306`, Docker Redis `26379`.
- completed: Started backend with `scripts\runtime\start-branch-backend.ps1 -Slot 0`; `48021` listening with process `46016`; health endpoint returned HTTP `200`.
- completed: Started frontend with `scripts\runtime\start-branch-frontend.ps1 -Slot 0`; `8021` listening with process `44120`; frontend entry returned HTTP `200`.

## Final Verification

- `GET http://127.0.0.1:48021/actuator/health` -> HTTP `200`.
- `GET http://127.0.0.1:8021/` -> HTTP `200`.
## E2E Homepage Debug

- in_progress: User reported frontend runtime error; running Playwright real-browser homepage access against http://127.0.0.1:8021/.
- BDD: Homepage compiles static route modules -> Given the `int_shedule` frontend is running at `http://127.0.0.1:8021/`, When Playwright opens the homepage, Then Vite serves `src/router/modules/remaining.ts` without 500 and the referenced DCC controlled-file logs page exists.
- RED: `node tests/e2e/dcc-controlled-file-logs-static.spec.js` -> FAIL, expected reason: missing required frontend file `src/views/dcc/controlled-file/logs/index.vue`.
- RED: Playwright real-browser homepage access `http://127.0.0.1:8021/` -> FAIL, `src/router/modules/remaining.ts` returned HTTP 500 because `@/views/dcc/controlled-file/logs/index.vue` could not be resolved.

## 2026-07-25 Rerun

- in_progress: User requested running frontend and backend again in the `int_shedule` workspace.
- completed: Re-read `docs/task-closeout-rules.md`, `docs/local-runtime.md`, `docs/branch-runtime-ports.md`, `docs/worktree-restrictions.md`, `docs/powershell-memory.md`, and existing task records.
- completed: Confirmed current branch `int_shedule`; required ports are frontend `8021` and backend `48021`.
- completed: Initial HTTP checks showed both `48021` and `8021` were not responding before startup.
- completed: Verified local dependency ports `127.0.0.1:23306` and `127.0.0.1:26379` were reachable.
- completed: Started backend with `scripts\runtime\start-branch-backend.ps1 -Slot 0`; backend listener `48021` is owned by `java.exe` process `31412`; health endpoint returned HTTP `200`.
- completed: First frontend start with `scripts\runtime\start-branch-frontend.ps1 -Slot 0` reached Vite ready state on process `30612`, but `curl`/`Invoke-WebRequest` against `/` and `/@vite/client` timed out with zero bytes.
- completed: Stopped the current task-owned unresponsive Vite process and restarted frontend with `scripts\runtime\start-branch-frontend.ps1 -Slot 0 -HostAddress 127.0.0.1`; frontend listener `8021` is owned by `node.exe` process `39436`; frontend entry returned HTTP `200`.
- GREEN: `Invoke-WebRequest -UseBasicParsing http://127.0.0.1:48021/actuator/health` -> PASS, HTTP `200`.
- GREEN: `Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8021/` -> PASS, HTTP `200`.
- completed: Runtime logs for this rerun are `backend-runtime-20260725-080817.*.log` and `frontend-runtime-20260725-082126.*.log`.

## 2026-07-25 Commit And Closeout

- completed: Re-read `docs\task-closeout-rules.md`, `docs\powershell-memory.md`, `docs\frontend-development.md`, and `docs\e2e-rules.md` before commit verification.
- GREEN: `node tests\e2e\dcc-controlled-file-logs-static.spec.js` -> PASS, DCC controlled-file logs consolidation static contract passed after SQL path correction.
- completed: Ran project experience consolidation by merging the branch-local Docker dependency port lesson into existing `docs\local-runtime.md` and adding a route in `docs\experience-index.md`; no new long-term document was needed.
- completed: `task-closeout-cleanup --mode preview` -> PASS, keep only `task.md`, `execution-log.md`, `verification-report.md`, delete only task runtime logs, no blocked or warnings.
- BLOCKER: `task-closeout-cleanup --mode apply` -> FAIL, `PermissionError [WinError 32]` on `backend-runtime-20260725-080817.stderr.log` because the backend service still holds the log file open. Service shutdown was not part of the commit request, so cleanup remains blocked and status stays `ready_for_closeout`.

## 2026-07-25 Commit Evidence

- implementation commit: `0ee5ba4f1534bcaf7a9044eb7c92f2f66cff2fcf` (`chore: record int_shedule local runtime verification`).
- committed files: `IntRuoyiBackend/yudao-server/src/main/resources/application-local.yaml`, `IntRuoyiFronted/tests/e2e/dcc-controlled-file-logs-static.spec.js`, `docs/local-runtime.md`, `docs/experience-index.md`, and task records.
- branch status after implementation commit: `int_shedule...origin/int_shedule [ahead 1]`; push remains pending.
