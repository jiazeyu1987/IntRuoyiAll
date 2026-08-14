# CI/CD Environment Evidence

## Environment

- Scope: test-only code release.
- Deploy target: `172.30.30.58`.
- Build metadata only: `172.30.30.59`; no remote operation is authorized.
- Forbidden targets/actions: `172.30.30.57`, remote operations on `172.30.30.59`, `mark-tested`, `promote-prod`, `promote-backup`.
- Release worktree: `D:\IntRuoyiWorktree\release-third-party-feedback-20260801`.
- Frozen source commit: `ef6052c6dcfab1930a19bccfb980b83cf9f16839`.

## Commands

- Build: `pwsh.exe -NoProfile -File publish-int-ruoyi.ps1 -Mode build-release -Component intruoyi -Environment test -ReleaseTag release-20260801-third-party-feedback-formal-import-r11 -NasConfigPath <approved-nas-json> -TestServerHost 172.30.30.58 -BackupServerHost 172.30.30.59 -SkipDatabaseSync -SkipMinioSync`.
- Deploy: use the same script in `deploy-release` mode with `Environment=test`, `ServerHost=172.30.30.58`, and the same code-only switches.
- Tests: targeted NAS baseline/empty-facts pytest, complete release tooling pytest, and the target MES JUnit.

## Secrets

- Required secret source: approved NAS JSON outside Git.
- Secret owner: existing IntRuoyi maintenance runtime configuration.
- SSH and runtime credentials remain external to Git and are not printed in task evidence.

## Pipeline

- Changed pipeline file: `IntRuoyiBackend/script/deploy/publish-int-ruoyi.ps1`.
- The Git comparison baseline now comes from the latest NAS release manifest rather than failed local packages.
- Empty Git facts are accepted by the formal `summaryGenerator=none` path.

## Verification

- RED: local failed r9 manifest was selected instead of the NAS package, and an empty facts collection failed PowerShell parameter binding.
- GREEN: 2 targeted pytest cases passed; complete release tooling suite passed with 106 tests.
- Business regression: target MES JUnit passed with 1 test and 0 failures.
- Runtime verification remains pending until r11 is built and deployed.

## Rollback

- If deployment verification fails, stop without promoting the release.
- Restore the previous test-server release through the existing release rollback path using the previous successful package recorded in the release manifest and operation log.
- Do not alter database or MinIO because this release uses `SkipDatabaseSync` and `SkipMinioSync`.

## Blockers

- None before r11 build.
- Any missing/invalid manifest, dirty source repo, artifact hash mismatch, active release lock, target drift, health failure, frontend HTTP failure, or release-info mismatch blocks deployment completion.
