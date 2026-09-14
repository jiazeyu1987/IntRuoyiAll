# CI/CD Evidence

## Environment And Target

- Environment: test only.
- Server: `172.30.30.58`.
- Frontend: `http://172.30.30.58:8081/`.
- Backend health: `http://172.30.30.58:48081/actuator/health`.
- Forbidden targets: production `172.30.30.57`, backup `172.30.30.59`, `mark-tested`, `promote-prod`, `promote-backup`.

## Commands

- Status: `IntRuoyiBackend\script\deploy\show-int-ruoyi-test-status.bat`.
- Publish: `IntRuoyiBackend\script\deploy\publish-int-ruoyi.ps1 -Environment test -ServerHost 172.30.30.58`.
- Rollback: use the same release tooling against the previous tested releaseTag only after explicit user confirmation.

## Secrets

- Test login credentials were provided by the user for this task.
- Passwords, tokens, private keys, database secrets and connection strings must not be logged, committed, or printed in raw command output.

## Evidence

- GREEN: clean source commit `e9eca0b3`, `dirty=false` before build.
- GREEN: migration policy gate passed for 402 migrations, including both DCC category-rule migrations.
- GREEN: test-server status precheck returned backend HTTP 200 / `UP`, frontend HTTP 200, current tag `release-20260729-sqlfix-test-r260729d-r1`.
- REJECTED: `release-20260731-dcc-file-category-rules-test-r1` is incomplete because manifest and preflight plan are absent.
- BLOCKER CLEARED: concurrent build operation ended `FAILED` before deployment and released the build guard; no overlapping deployment is active.
- Pending: releaseTag / manifest / image tag verification.
- Pending: backend health and frontend HTTP 200.
- Pending: DCC batch-recognition task terminal result.

## Build Input

- Release tag: `release-20260731-dcc-file-category-rules-test-r2`.
- Source clone: `D:\IntRuoyiWorktree\dcc-file-category-rules-test-release`.
- Source commit: `e9eca0b386a7a01b28421084a937792245609d8f`.
- Scope: `Component=intruoyi`, code-only, skip business database sync and MinIO sync; required schema/seed migrations remain mandatory.
