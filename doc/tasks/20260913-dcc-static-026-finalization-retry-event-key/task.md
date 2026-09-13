# DCC-STATIC-026 Finalization Retry Event Key

## Task Goal

Fix DCC-STATIC-026: publication finalization retry must use a stable key for one retry attempt and a new key after that attempt fails, so a later legal retry can move unified controlled-content state back to `FINALIZING` and complete finalization.

## Milestones

- [x] Read required project rules and bug-regression skill.
- [x] Record BDD and RED target before production changes.
- [x] Add a failing regression test for retry event key reuse after a failed retry.
- [x] Implement the minimal event-key fix for DCC finalization retry.
- [x] Run targeted static/unit/compile verification without E2E, services, database writes, or remote server work.
- [x] Commit and push the task branch after verification.
- [x] Run cleanup preview and record final closeout blocker or completion evidence.

## Expected Verification

- `node IntRuoyiBackend\yudao-module-dcc\src\test\js\dcc-static-026-finalization-retry-event-key-static.spec.cjs`
- `mvn -pl yudao-module-dcc -Dtest=DccControlledFileFinalizationServiceImplTest#retryStamp_failedRetryThenSecondRetryUsesNewAttemptEventKey test`
- `mvn -pl yudao-module-dcc -am "-DskipTests" compile`
- `git diff --check -- <task-owned files>`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260913-dcc-static-026-finalization-retry-event-key/bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260913-dcc-static-026-finalization-retry-event-key --mode preview`

## Design Constraints Check

- Scope limited to DCC-STATIC-026 finalization retry event keys.
- No fallback, graceful degradation, mock success, E2E, service restart, database write, remote server operation, or unrelated dirty change cleanup.
- Same retry attempt keeps one deterministic event key across start/failure recording; a later retry after failure uses a new attempt key.
- Commit must run from a registered `D:\IntRuoyiWorktree\` worktree because the C-disk Codex worktree fails the branch runtime guard.

## Current Status

blocked - Minimal implementation and targeted verification passed; task branch `codex/20260913-dcc-static-026-finalization-retry-event-key-closeout` was committed and pushed, but automatic closeout apply cannot run because local `int_main` cannot receive an ff-only merge from this task branch and `E:\IntRuoyi` still has untracked unrelated files.

## Cleanup Keep

- doc/tasks/20260913-dcc-static-026-finalization-retry-event-key/task.md
- doc/tasks/20260913-dcc-static-026-finalization-retry-event-key/execution-log.md
- doc/tasks/20260913-dcc-static-026-finalization-retry-event-key/verification-report.md
- doc/tasks/20260913-dcc-static-026-finalization-retry-event-key/bug-regression-evidence.md
- IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-026-finalization-retry-event-key-static.spec.cjs
