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
