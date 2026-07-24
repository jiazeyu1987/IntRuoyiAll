# Execution Log

- CHECK: latest backend task `20260524-showroom-manual-release-live-revision-blocker` is still `进行中`; current scope is local deployment tooling only and will not stage or commit that unrelated task.
- BDD: 成对 worktree 自动分配端口 -> Given 前端与后端 Git worktree 均包含 `int_main` 和多个同名任务 worktree / When 同步端口登记表 / Then `int_main` 必须固定为 `8081/48081`，其他 worktree 必须按稳定顺序依次递增，且前后端 worktree 名称不匹配时必须失败。
- BDD: 新 worktree 使用下一个端口 -> Given 端口登记表已有历史分配 / When 新增成对 worktree 后再次同步 / Then 新 worktree 必须使用历史最大端口后的下一组端口，不得复用已删除 worktree 的历史端口。
- RED: `powershell -ExecutionPolicy Bypass -File .\script\deploy\test-worktree-port-map.ps1` -> FAIL, expected reason: `worktree-port-map.ps1` does not exist yet.
- GREEN: `powershell -ExecutionPolicy Bypass -File .\script\tests\test-worktree-port-map.ps1` -> PASS, validated initial sequential assignment, historical max increment, and mismatch fail-fast behavior. The test file was placed under `script/tests/` to satisfy the repository tooling-test gate.
- GREEN: `powershell -ExecutionPolicy Bypass -File .\script\deploy\sync-int-ruoyi-worktree-ports.ps1 -Json` -> PASS, current registry written to `D:\ProjectPackage\Int\IntRuoyi\worktrees\.ports\worktree-ports.json` with `int_main=8081/48081`; existing assignments are preserved and newly detected paired worktrees use the next historical port pair.
- GREEN: `powershell -ExecutionPolicy Bypass -File .\script\deploy\show-int-ruoyi-local-status.ps1 -WorktreeName int_main -Json` -> PASS, resolved `int_main` to `frontendPort=8081` and `backendPort=48081`.
- GREEN: `powershell -ExecutionPolicy Bypass -File .\script\deploy\show-int-ruoyi-local-status.ps1 -WorktreeName edhr-test -Json` -> PASS, resolved `edhr-test` to `frontendPort=8084` and `backendPort=48084`.
- GREEN: `python C:\Users\BJB110\.codex\skills\project-bootstrap-engineering\scripts\validate_project_bootstrap.py --evidence docs\engineering\bootstrap-evidence.md` -> PASS, bootstrap evidence contract validated.
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260524-worktree-port-allocation --mode preview` -> PASS, cleanup preview kept only `task.md` and `execution-log.md`, with no delete or blocked entries.
