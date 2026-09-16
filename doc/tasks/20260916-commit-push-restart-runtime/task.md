# Commit Push Restart Runtime

## Task Goal
提交并推送当前前后端代码变更，随后按 int_main 本地运行规则重启前后端。

## Milestones
- [x] Read required Git/runtime/closeout rules.
- [x] Inspect current dirty files and verification scope.
- [x] Run required targeted verification and port guard.
- [x] Commit current code changes.
- [ ] Push current branch to origin/int_main.
- [x] Restart backend and frontend using standard local scripts.
- [x] Verify backend health and frontend HTTP status.
- [ ] Mark task completed after closeout evidence is recorded.

## Expected Verification
- `git status --short --branch` before commit and after push.
- Targeted Maven tests for changed backend modules/classes.
- `scripts\preflight\branch-runtime-port-guard.ps1` before commit/push.
- Standard local restart script result.
- `http://127.0.0.1:48081/actuator/health` returns UP.
- `http://127.0.0.1:8081` returns HTTP 200.

## Current Status
blocked - local commit and runtime restart passed, but GitHub HTTPS TLS connection fails before push can complete.

## Design Constraints Check
- No fallback, mock success, silent downgrade, or random ports.
- Use int_main default ports 8081/48081.
- Do not stop unknown or unrelated processes.
- PowerShell commands avoid `&&`.
- Current dirty code is treated as user-authorized baseline commit for this commit/push task.

## Cleanup Keep
- doc/tasks/20260916-commit-push-restart-runtime/task.md
- doc/tasks/20260916-commit-push-restart-runtime/execution-log.md
- doc/tasks/20260916-commit-push-restart-runtime/verification-report.md
