# Test Release CI/CD Evidence

## Environment

- Target environment: test only.
- Target server: `172.30.30.58`.
- Forbidden targets: production `172.30.30.57` and backup `172.30.30.59`.

## Commands

- Build: `publish-int-ruoyi.ps1 -Mode build-release -Component intruoyi -ReleaseTag release-20260728-codeonly-noonlyoffice-test-r2 ... -SkipDatabaseSync -SkipMinioSync`.
- Deploy: `publish-int-ruoyi.ps1 -Mode deploy-release -Environment test -Component intruoyi -ReleaseTag release-20260728-codeonly-noonlyoffice-test-r2 ... -SkipDatabaseSync -SkipMinioSync`.
- Important scope guard: neither command may pass `-IncludeOnlyOffice`.

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

## Rollback

- Failed releaseTags are not reused.
- On failure before container switch, restore `.env IMAGE_TAG` to actual running image tag and close operation lock as `FAILED`.
- On failure after container switch, freeze logs and remote state, then use a fresh releaseTag after fixing root cause.
