# Branch Runtime Registry Regression Evidence

## Bug Summary

The isolated worktree `D:\IntRuoyiWorktree\system-backup-plan` was correctly registered in `D:\IntRuoyiWorktree\.ports\worktree-ports.json`, but branch runtime scripts ignored that registry and only resolved known fixed branch/path profiles. As a result, local runtime and real E2E startup were blocked for branch `codex/system-backup-plan`.

## Expected Behavior

Registered worktrees under `D:\IntRuoyiWorktree\` must resolve their configured `profile`, `slot`, frontend port, and backend port from the port registry. Invalid registry state must fail fast instead of falling back to base ports or fixed branch inference.

## Reproduction

- `powershell -ExecutionPolicy Bypass -File scripts\runtime\show-branch-runtime.ps1 -Slot 2` -> FAIL before fix: no branch runtime profile registered for `codex/system-backup-plan`.

## Root Cause

`scripts/runtime/branch-runtime-profile.ps1` had no registry reader and no registered worktree runtime context. `show-branch-runtime.ps1`, `start-branch-frontend.ps1`, `start-branch-backend.ps1`, and `branch-runtime-port-guard.ps1` therefore could not consume the authoritative `.ports` record for this worktree.

## Regression Test

- Added `IntRuoyiBackend/script/tests/test_branch_runtime_profile.py`.
- Covers legacy nested registry shape, top-level worktree entries, default slot resolution, port/profile mismatch fail-fast, and duplicate active entry fail-fast.

## RED: Evidence

- `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py` -> FAIL before implementation, missing registry-backed runtime context.

## GREEN: Evidence

- `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py` -> PASS, 3 tests.
- `powershell -ExecutionPolicy Bypass -File scripts\runtime\show-branch-runtime.ps1` -> PASS, `profile=int_main`, `slot=2`, frontend `8083`, backend `48083`.
- `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS.

## Risk And Scope

The fix is scoped to runtime profile resolution and startup/guard consumers. It does not change shared `.env`, backend `application-local.yaml`, or production deployment behavior. It intentionally fails fast on missing or inconsistent registry data.

## Verification

- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260725-system-backup-plan\bug-regression-evidence.md` -> PASS after adding the required markers.

## Blockers

- Real page E2E remains blocked until local/test runtime dependencies, login, and menu SQL application are verified through the real frontend path.
