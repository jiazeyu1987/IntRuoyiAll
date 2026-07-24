# 执行记录：worktree 端口脚本 codex 名称修复

- BDD: codex 扁平后端 worktree 可推导任务名 -> Given git worktree 返回 `D:\ProjectPackage\Int\IntRuoyi\worktrees\ruoyi-vue-pro-dcc-nas-transfer-mirror-verify-20260525` 且分支为 `refs/heads/codex/dcc-nas-transfer-mirror-verify-20260525` / When 端口脚本推导 IntRuoyi worktree 名称 / Then 返回 `dcc-nas-transfer-mirror-verify-20260525`，不在名称推导阶段抛错。
- BDD: 未配对 worktree 仍失败 -> Given 后端存在 `dcc-nas-transfer-mirror-verify-20260525` worktree 但前端没有同名 worktree / When 同步端口登记表 / Then 脚本必须在前后端配对校验阶段失败并指出缺失前端，不得静默跳过。
- Reproduction: `powershell -NoProfile -ExecutionPolicy Bypass -Command ". .\script\deploy\worktree-port-map.ps1; ConvertTo-IntRuoyiWorktreeName -Path 'D:\ProjectPackage\Int\IntRuoyi\worktrees\ruoyi-vue-pro-dcc-nas-transfer-mirror-verify-20260525' -Branch 'refs/heads/codex/dcc-nas-transfer-mirror-verify-20260525' -RepoFolder 'ruoyi-vue-pro'"` -> FAIL, expected reason: 当前脚本只能识别 `int_main`、`task/<name>` 或 `worktrees\<name>\ruoyi-vue-pro`，不能识别 `codex/<name>` 和 `worktrees\ruoyi-vue-pro-<name>`。
- RED: `powershell -ExecutionPolicy Bypass -File .\script\tests\test-worktree-port-map.ps1` -> FAIL, expected reason: 新增 codex 扁平后端 worktree 测试触发同一名称推导异常。
- GREEN: `powershell -ExecutionPolicy Bypass -File .\script\tests\test-worktree-port-map.ps1` -> PASS, validated codex branch name derivation, flat repo-prefixed worktree path derivation, existing deterministic port assignment, historical max increment, and mismatch fail-fast behavior.
- GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -Command ". .\script\deploy\worktree-port-map.ps1; ConvertTo-IntRuoyiWorktreeName -Path 'D:\ProjectPackage\Int\IntRuoyi\worktrees\ruoyi-vue-pro-dcc-nas-transfer-mirror-verify-20260525' -Branch 'refs/heads/codex/dcc-nas-transfer-mirror-verify-20260525' -RepoFolder 'ruoyi-vue-pro'"` -> PASS, returned `dcc-nas-transfer-mirror-verify-20260525`.
- GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -Command ". .\script\deploy\worktree-port-map.ps1; ConvertTo-IntRuoyiWorktreeName -Path 'D:\ProjectPackage\Int\IntRuoyi\worktrees\ruoyi-vue-pro-dcc-nas-transfer-mirror-verify-20260525' -Branch '' -RepoFolder 'ruoyi-vue-pro'"` -> PASS, returned `dcc-nas-transfer-mirror-verify-20260525`.
- GREEN/Fail-fast: `powershell -NoProfile -ExecutionPolicy Bypass -Command ". .\script\deploy\worktree-port-map.ps1; Sync-IntRuoyiWorktreePorts -NoWrite | Out-Null"` -> FAIL at paired-worktree validation, expected reason: `Missing frontend: [dcc-nas-transfer-mirror-verify-20260525]`. The original name derivation failure no longer occurs.
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260526-worktree-port-codex-name-fix\execution-log.md` -> PASS.
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260526-worktree-port-codex-name-fix --mode preview` -> PASS, no task-specific temporary files require deletion.

## Bug

`worktree-port-map.ps1` could not derive an IntRuoyi worktree name for the existing backend worktree path `D:\ProjectPackage\Int\IntRuoyi\worktrees\ruoyi-vue-pro-dcc-nas-transfer-mirror-verify-20260525` on branch `refs/heads/codex/dcc-nas-transfer-mirror-verify-20260525`.

## Expected

The script must deterministically derive `dcc-nas-transfer-mirror-verify-20260525` from the `codex/<name>` branch or from the historical flat path format `worktrees\<repoFolder>-<name>`. If the resulting backend worktree has no matching frontend worktree, synchronization must fail at the existing paired-worktree validation with an explicit missing-frontend message.

## Root Cause

`ConvertTo-IntRuoyiWorktreeName` only recognized `int_main`, `task/<name>`, and the canonical nested path format `worktrees\<name>\<repoFolder>`. It did not recognize the existing `codex/<name>` branch prefix or repo-prefixed flat worktree directories.

## Verification

The regression test covers the original codex branch case, the branch-unavailable flat path case, existing deterministic port assignment, historical max increment, and mismatch fail-fast behavior.

Actual read-only synchronization now fails at the intended paired-worktree validation: `Missing frontend: [dcc-nas-transfer-mirror-verify-20260525]`, confirming the name derivation bug is fixed while the no-fallback pairing policy remains active.

## Blockers

The local workspace still contains a backend-only `dcc-nas-transfer-mirror-verify-20260525` worktree. Full port synchronization will continue to fail until that worktree is paired with a frontend worktree or explicitly cleaned up in a separate workspace maintenance task.
