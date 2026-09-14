# Verification Report

## Summary

- Branch: `int_main`
- Remote: `origin` -> `https://github.com/jiazeyu1987/IntRuoyiAll.git`
- Staged files before commit: `87`
- Pre-commit result: PASS
- Push result: PASS

## Evidence

- `git diff --cached --check` passed after removing EOF-only blank lines from three DCC test files.
- `scripts\preflight\branch-runtime-port-guard.ps1` passed for `int_main/int_main`, frontend `8081`, backend `48081`.
- Large file scan found `0` staged files over 100 MB.
- Runtime/build baseline from `doc/tasks/20260904-restart-local-runtime/verification-report.md`: standard full restart PASS, Maven reactor `BUILD SUCCESS`, backend health `UP`, frontend HTTP `200`.
- Commit `fcfd718d5` pushed `int_main` to `origin`.
- Commit `05537f25e` pushed the remaining frontend source diff to `origin`.
- Cleanup preview/apply passed with no deleted or blocked paths.

## Scope Notes

- This commit/push task did not run new E2E, database writes, service restarts, remote server operations, force push, reset, rebase, or history rewrite.
- Sensitive pattern matches were reviewed as business field names, token plumbing, or test placeholder values; no runtime log, PID, build artifact, `.env`, or archive path was staged.
- Remaining uncommitted local files were not included because they are outside the clean frontend/backend code push boundary or contain account/password text.
