# Verification Report

## Summary

- Result: PASS.
- `int_main` backend on `48081` now runs the latest `origin/int_main` backend Jar built from commit `e71769ece854c56d911779b44afd5d57247ba9b5`.

## Evidence

- Build: `mvn -pl yudao-server -am "-DskipTests" package` -> `BUILD SUCCESS`.
- Runtime Jar: `E:\IntRuoyi\output\runtime\int_main\backend-runtime-latest-replan-shift-hours-20260806-220545.jar`.
- SHA256: `CD2F95C0CB7D819E064B5B81ECEBACC32D6428A8C4A767442C6421D5403A980E`.
- Old PID: `47520`, stopped after confirming it owned `48081` and belonged to `E:\IntRuoyi\output\runtime\int_main`.
- New PID: `936`.
- Health check: `http://127.0.0.1:48081/actuator/health` -> `{"status":"UP"}`.
- Cleanup: `task-closeout-cleanup` preview/apply passed with no deletions, and temporary build worktree was removed.

## Scope Notes

- Main workspace source files had unrelated dirty changes, so the Jar was built from a clean detached worktree under `D:\IntRuoyiWorktree\restart-latest-backend-20260806`.
- No production source code was changed by this restart task.
