# Test Release CI/CD Evidence

## Environment

- Target environment: test only.
- Target server: `172.30.30.58`.
- Forbidden targets: production `172.30.30.57` and backup `172.30.30.59`.

## Commands

- Build: `publish-int-ruoyi.ps1 -Mode build-release -Component intruoyi -ReleaseTag release-20260728-codeonly-noonlyoffice-test-r2 ... -SkipDatabaseSync -SkipMinioSync`.
- Deploy: `publish-int-ruoyi.ps1 -Mode deploy-release -Environment test -Component intruoyi -ReleaseTag release-20260728-codeonly-noonlyoffice-test-r2 ... -SkipDatabaseSync -SkipMinioSync`.
- Important scope guard: neither command may pass `-IncludeOnlyOffice`.

## Pipeline

- RED gate: release-info static contract failed before the publish script wrote frontend `release-info.json`.
- GREEN gates: parser gate, code-only SQL/tooling/release-info pytest, migration policy gate, branch runtime port guard, local/NAS manifest checks, deploy log scope checks, remote runtime checks.
- Manual gate: user authorized test server publish only; production, backup, `mark-tested`, `promote-prod` and `promote-backup` remained out of scope.

## Secrets

- NAS credentials are read from the approved local NAS release configuration.
- SSH and runtime secrets are supplied by the controlled environment.
- No credential values may be recorded in task documents or commits.

## Verification Plan

- Validate manifest: `publishScope=code-only`, `component=intruoyi`, `includeOnlyOffice=false`, sourceRepos dirty false.
- Validate package artifacts: local and NAS artifact hashes match; no database dump, MinIO snapshot or runtime-data; image tar does not include `onlyoffice/documentserver`.
- Validate deploy log: data/data-dependent SQL skipped; no `Starting application services ... onlyoffice`; no OnlyOffice health-check command.
- Validate runtime: `.env IMAGE_TAG`, backend/frontend image tags, backend health, frontend HTTP, operation lock.
- Validate release-info: frontend `/release-info.json` returns the final releaseTag and `publishScope=code-only`.

## GREEN Evidence

- Tests: code-only SQL/tooling/release-info contract tests passed with `7 passed`.
- Build: r2 build-release completed and uploaded to NAS; r2 local/NAS manifest checks passed.
- Deploy: r2 deploy-release completed on `172.30.30.58`; log shows only `backend frontend` and `--no-deps`.
- Runtime: r2 `.env`, backend/frontend images, backend health, frontend HTTP, release-info and release lock checks passed.
- Boundaries: r2 package has no database dump, no MinIO snapshot, no runtime-data and no OnlyOffice image; OnlyOffice container was not restarted by r2.

## Blockers

- r1 blocker: deployed frontend did not contain `release-info.json`; `/release-info.json` returned SPA `index.html`.
- Resolution: added a publish-script release-info generation gate and rebuilt/deployed fresh r2.
- Closeout blocker: cleanup preview cannot fast-forward merge `codex/20260727-onlyoffice-test-release` into `int_main` because the branch is behind `int_main`; task remains `ready_for_closeout` until integration is explicitly handled.

## Rollback

- Failed releaseTags are not reused.
- On failure before container switch, restore `.env IMAGE_TAG` to actual running image tag and close operation lock as `FAILED`.
- On failure after container switch, freeze logs and remote state, then use a fresh releaseTag after fixing root cause.
