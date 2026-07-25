# Execution Log

## User Intent

用户要求运行前后端程序。当前路径为 `E:\IntRuoyiBranch\QMS\IntRuoyiAll`，应按 `int_qms` runtime profile 启动前端 `8061` 和后端 `48061`。

## Rule Bootstrap

- Read: `docs/local-runtime.md`.
- Read: `docs/task-closeout-rules.md`.
- Read: `docs/powershell-memory.md`.
- Read: `docs/branch-runtime-ports.md`.
- Read: `docs/powershell-encoding.md`.
- Read: `docs/worktree-restrictions.md`.
- Read: `docs/experience-index.md`.
- GREEN: experience-preflight -> PASS, applicable local runtime and PowerShell gates copied to `task.md`.

## Milestone Evidence

- Git status before task docs: `## int_qms...origin/int_qms`, clean.
- Branch: `int_qms`.

- Read: `docs/backend-development.md`.
- Read: `docs/frontend-development.md`.
- Port precheck: `48061` no listener output; `8061` no listener output.
- Backend prerequisite check: `IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar` missing; backend must be packaged before startup.
- Frontend prerequisite check: `IntRuoyiFronted\node_modules\.bin\vite.cmd` missing; frontend dependencies must be installed before startup.
## Build Blocker

- RED: `mvn.cmd -pl yudao-server -am -DskipTests package` -> FAIL, required package `cn.iocoder.yudao.module.bpm.formcenter.runtime` is absent, including `FormCenterRuntimeService` and `FormCenterBpmEventBridge`.
- Diagnostic-only source reconstruction exposed the next missing package `cn.iocoder.yudao.module.erp.service.sync.runtime`.
- Further diagnostic compilation showed MES callers require runtime members `initialWindowStart`, `forceInitialWindowStart`, and `initialSync`.
- The runtime source directories are absent from Git history and ignored by Git. Retaining inferred half-implementations would be an unsafe behavior change outside the startup request.
- Diagnostic-only BPM/ERP/MES Java sources were removed after investigation.
- Cleanup verification: `rg --files IntRuoyiBackend/yudao-module-bpm IntRuoyiBackend/yudao-module-erp IntRuoyiBackend/yudao-module-mes | rg "(formcenter/runtime|sync/runtime)"` returned no files.
- Git verification after cleanup: `## int_qms...origin/int_qms`; only `doc/tasks/run-qms-local-runtime-20260724/` is untracked.

## Final Runtime Verification

- `8061`: no listener.
- `48061`: no listener.
- Backend health endpoint was not called because no backend artifact or listener exists.
- Frontend entry was not called because dependencies are missing and the required paired backend runtime cannot be produced.
- Status: blocked. Required prerequisites are the authoritative Git-tracked BPM/ERP runtime implementations compatible with MES callers, followed by frontend dependency installation.
- GREEN: project-experience-consolidation -> PASS, reusable local startup prerequisite gate merged into `docs/local-runtime.md` and routed from `docs/experience-index.md`.
## Fix Attempt 2026-07-25

- BDD: QMS runtime source restored from comparable Shedule workspace -> Given Shedule local workspace can build/run because runtime Java source directories exist, When QMS is restored from the same runtime source contracts and Git ignore rules no longer ignore Java runtime packages, Then QMS backend packaging can see BPM form-center and ERP sync runtime classes and the files are eligible for Git tracking.
- RED: `python -m pytest IntRuoyiBackend\script\tests\test_runtime_source_tracking_guard.py` -> FAIL, expected reason: QMS is missing 9 runtime Java source files.
## Submission Verification 2026-07-25

- GREEN: `python -m pytest IntRuoyiBackend\script\tests\test_runtime_source_tracking_guard.py` -> PASS, 2 tests passed.
- GREEN: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS, `int_qms` frontend `8061`, backend `48061`.
- GREEN: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS, reactor build success and `yudao-server-exec.jar` attached.
- GREEN: `git diff --check` -> PASS, no whitespace errors; Git reported only the existing LF-to-CRLF notice for `docs/local-runtime.md`.
- NOTE: `apply_patch` and sandboxed UTF-8 writes were blocked by Windows sandbox ACLs for task docs; the same document updates were applied through an approved explicit UTF-8 PowerShell write.
- Status: code restoration and submission verification complete; task moved to `ready_for_closeout` before cleanup preview/apply.

## Runtime Verification 2026-07-25

- GREEN: `python -m pytest IntRuoyiBackend\script\tests\test_runtime_source_tracking_guard.py` -> PASS, 2 tests passed.
- Backend verification: `http://127.0.0.1:48061/actuator/health` -> `200 {"status":"UP"}`.
- Frontend startup: `scripts\runtime\start-branch-frontend.ps1` -> Vite v5.1.4 ready in 2984 ms, local entry `http://localhost:8061/`.
- Frontend verification: `http://127.0.0.1:8061/` -> `200 OK`.
- Port ownership: backend `48061` owner PID `7380`; frontend `8061` owner PID `32448`; both command lines point under `E:\IntRuoyiBranch\QMS\IntRuoyiAll`.
- Diagnostic note: no-plugin and all-plugin Vite API probes could create/listen on `8061`; final script startup succeeded after dependency rebuild/install state was settled, so no frontend production config fallback was introduced.
## Closeout 2026-07-25

- Stopped task-owned runtime processes after confirming ports `8061` and `48061` belonged to the current QMS workspace; subsequent port checks reported no listeners.
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id run-qms-local-runtime-20260724 --mode preview` -> PASS, no blocked paths or warnings.
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id run-qms-local-runtime-20260724 --mode apply` -> PASS, deleted task-owned temporary logs, PID files, and probe artifacts only.
- GREEN: `rg "runtime 源码包缺失|本地运行构建输入完整性门禁" docs\experience-index.md docs\local-runtime.md` -> PASS, experience route resolves to `docs/local-runtime.md`.
- Status: completed after cleanup evidence and experience route verification.
## Commit Evidence

- IMPLEMENTATION COMMIT: `5b00fd72` (`fix: restore qms runtime source tracking`).
- Implementation file list: `.gitignore`; `IntRuoyiBackend/script/tests/test_runtime_source_tracking_guard.py`; BPM runtime Java package; ERP sync runtime Java package; `IntRuoyiBackend/yudao-server/src/main/resources/application-local.yaml`; `docs/experience-index.md`; `docs/local-runtime.md`.
