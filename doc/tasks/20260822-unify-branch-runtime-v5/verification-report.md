# Verification Report

## Scope

主工作区端口合同与共享 V5 登记表的统一。

## Verification

通过。

- `python -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py -q -p no:cacheprovider` -> `16 passed in 5.94s`。
- `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> `Branch runtime port guard passed`。
- PowerShell runtime profile parse -> `runtime-profile-syntax-ok`。
- Shared registry at the verification run: contract `2026-08-21-branch-runtime-v5`; active `14`; slot `31` count `1`, worktree `20260820-pqc-inspection-equipment-selection`, branch `codex/20260820-pqc-inspection-equipment-selection`, ports `8206/48206`。
- Final read-only recheck: contract remains V5, active count is `16` because unrelated concurrent worktrees were added; slot `31` remains `8206/48206` and the registry was not written by this task。
- Core V5 runtime profile compared with the existing V5 source worktree using `git diff --no-index --ignore-cr-at-eol` -> no content differences。
- Scoped `git diff --check` -> pass (only normal CRLF conversion warnings)。

## Scope Boundary

仅修改主工作区的运行时合同、守卫、文档索引和回归测试；未修改共享登记表、其他 worktree 业务文件、分支、进程或端口。

## Closeout

Task-closeout preview/apply passed with no deletions; task status is completed.
