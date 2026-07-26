# Execution Log

## User Intent

- 用户请求：`重启前后端`。

## Rule Preflight

- 读取 `docs/task-closeout-rules.md`。
- 读取 `docs/local-runtime.md`。
- 读取 `docs/branch-runtime-ports.md`。
- 读取 `docs/powershell-memory.md`。
- 读取 `docs/powershell-encoding.md`。
- 读取 `docs/experience-index.md`，本任务命中本地运行态重启门禁。

## Dirty Worktree Baseline

- `git status --short --branch` 显示任务开始前已有既有脏改动。
- 已按项目规则保存基线提交：`8b113467 chore: baseline dirty worktree before runtime restart`。

## Milestone Log

- BDD: local runtime restart -> Given `E:\IntRuoyi` int_main runtime uses frontend port 8081 and backend port 48081, When the local frontend and backend are restarted, Then both ports are owned by confirmed IntRuoyi processes and the frontend entry plus backend health endpoint are reachable.
- Restart script attempt -> FAIL, `Missing int_main frontend path: E:\IntRuoyi\yudao-ui-admin-vue3`.
- RED: `powershell -NoProfile -ExecutionPolicy Bypass -File IntRuoyiBackend\script\tests\test-worktree-port-map.ps1` -> FAIL, `RepoFolder` did not accept `IntRuoyiFronted`.
- Root cause -> `worktree-port-map.ps1` still hardcoded the previous frontend directory name instead of current `IntRuoyiFronted`.
- GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File IntRuoyiBackend\script\tests\test-worktree-port-map.ps1` -> PASS.
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260726-restart-local-runtime\bug-regression-evidence.md` -> PASS.
- GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File IntRuoyiBackend\script\tests\test_restart_ruoyi_script_onlyoffice.ps1` -> PASS.
- GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full` -> PASS.
- Runtime verification -> backend `http://127.0.0.1:48081/actuator/health` returned `UP`; frontend `http://127.0.0.1:8081/` returned HTTP `200`.
- Port ownership -> `8081` PID `58060` `node.exe` under `E:\IntRuoyi\IntRuoyiFronted`; `48081` PID `52652` `java.exe` running the task runtime Jar under `E:\IntRuoyi\output\runtime\int_main`.
- Experience consolidation -> existing `docs/local-runtime.md` and `docs/experience-index.md` already contain the reusable `Missing int_main frontend path` gate, so no new long-term experience document was needed.
- Cleanup preview -> PASS, kept task records and `bug-regression-evidence.md`, no deletes, no blocked paths.
- Cleanup apply -> PASS, no deletes, main worktree detected, no worktree merge/removal needed.
- Status -> `completed`.
