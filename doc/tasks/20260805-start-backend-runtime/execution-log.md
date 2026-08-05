# Execution Log

## Intent

- User request: 启动后端。
- Runtime target: `E:\IntRuoyi` / `int_main` / backend port `48081`。

## BDD

- BDD: local backend starts on fixed int_main port -> Given the `E:\IntRuoyi` workspace is the `int_main` runtime, When the backend is started, Then port `48081` is owned by the project backend and `/actuator/health` returns `UP`.

## Progress

- Read `docs/local-runtime.md`, `docs/task-closeout-rules.md`, `docs/branch-runtime-ports.md`, and `docs/powershell-encoding.md`.
- Read `docs/experience-index.md`; applicable gates point back to `docs/local-runtime.md` local backend startup requirements.
- `git status --short --branch` completed and showed existing dirty changes plus unresolved conflicts before this task started.
- Read `docs/worktree-restrictions.md` before checking `48081` port ownership.
- Port check: `48081: NO_LISTENER`.
- Health check: `http://127.0.0.1:48081/actuator/health` failed with connection refused.
- Standard backend script identified as `IntRuoyiBackend/script/deploy/restart-int-ruoyi-local.ps1`; it performs Maven package before copying an independent runtime Jar.
- Project experience consolidation: searched existing memory/runtime docs; `docs/local-runtime.md` already covers fixed port, no old Jar fallback, source/runtime Jar safety, and fail-fast local backend startup gates, so no long-term experience document was changed.
- Continue request:复核 `git ls-files -u` 为空，相关冲突文件没有 `<<<<<<<` / `>>>>>>>` 标记，继续尝试正式启动脚本。
- RED: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> FAIL, `48081` 无监听且连接被拒绝。
- Standard restart: `IntRuoyiBackend/script/deploy/restart-int-ruoyi-local.ps1 -Component backend` -> completed Maven package and generated `output/runtime/int_main/backend-runtime-control-20260805-221422.jar`; first spawned process briefly reached `UP` then exited without a shutdown line, so it was not accepted as stable completion.
- Stable restart: existing standard script was re-dispatched through `Win32_Process.Create` to avoid exposing secret-bearing Java arguments in the task command and to keep the process detached from the current shell. It generated `output/runtime/int_main/backend-runtime-control-20260805-222248.jar` and launched Java PID `60192`.
- GREEN: delayed health check after stable restart -> PASS, `{"status":"UP"}`.
- Stability check: after an additional 60 seconds, PID `60192` was still running and `48081` health remained `UP`.

## Verification Evidence

- `Get-NetTCPConnection -LocalPort 48081 -State Listen` -> no listener.
- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> connection refused.
- `git diff --name-only --diff-filter=U` -> unresolved conflicts:
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/pqc/MesPqcProcessInspectionAggregateDetailDO.java`
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/pqc/MesPqcProcessInspectionAggregateDetailMapper.java`
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/enums/ErrorCodeConstants.java`
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesPqcProcessInspectionAggregationServiceImpl.java`
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesPqcProcessInspectionAggregationServiceTest.java`
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderSubmissionReviewServiceTest.java`
  - `docs/powershell-memory.md`
- Runtime Jar: `output/runtime/int_main/backend-runtime-control-20260805-222248.jar`.
- Runtime Jar SHA256: `4EA3E8BB6C585C738EB1F99AFE42C33827CB2908E275242819646213488F5A1F`.
- Listener: `0.0.0.0:48081 LISTENING 60192`.
- Health: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> `{"status":"UP"}`.
- Application log: `Tomcat started on port 48081`, `Started YudaoServerApplication`, `项目启动成功`.

## Blockers

- blocked: unresolved merge conflicts in backend source/test files prevent a formal Maven package and safe local backend startup. Starting from an old Jar or `target` Jar would violate the project no-fallback and runtime Jar rules.
- cleared for retry: follow-up check shows no unmerged index entries. Startup may still fail on build/runtime prerequisites and must be recorded as the real blocker if it does.
- none remaining for backend startup; service is running on `48081`.
