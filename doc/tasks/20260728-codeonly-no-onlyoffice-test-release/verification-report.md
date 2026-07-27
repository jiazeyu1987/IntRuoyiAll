# Verification Report

## Status

Ready for closeout.

## Required Evidence

- r2 local package manifest and artifact verification.
- r2 NAS package manifest and artifact verification.
- Test server preflight: no concurrent publish and no `RUNNING` release lock.
- r2 deploy result and sanitized deploy log.
- Post-deploy runtime: backend/frontend image tag, backend health `UP`, frontend HTTP 200, release lock `APPLIED`.
- Scope proof: no data sync and no OnlyOffice included or restarted by this release.

## Final Evidence

- Final releaseTag: `release-20260728-codeonly-noonlyoffice-test-r2`.
- Package: local and NAS manifests passed `code-only`, `component=intruoyi`, `includeOnlyOffice=false`, `onlyOfficeIncluded=false`, sourceRepos `dirty=false`, forbidden data path count `0`.
- Release info: package and deployed frontend `/release-info.json` both return r2 and `publishScope=code-only`.
- Deploy: test server `.env`, backend image and frontend image all use r2; backend health is `UP`; frontend HTTP is `200`; release lock is `APPLIED`; no `RUNNING` lock.
- No data: deploy logs show data required SQL and data-dependent SQL skipped; package/deploy cleanup removed required SQL/database dump paths from remote release package.
- No OnlyOffice: image tar contains no `onlyoffice/documentserver`; deploy starts only `backend frontend` with `--no-deps`; OnlyOffice container `StartedAt=2026-07-27T16:04:31.094101012Z` was unchanged.

## Closeout

- Cleanup preview status: blocked.
- Reason: current linked worktree branch cannot be fast-forward merged into `int_main`; cleanup apply and worktree removal were not run.
