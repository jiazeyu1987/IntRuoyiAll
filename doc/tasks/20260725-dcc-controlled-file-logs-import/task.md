# Fix DCC Controlled File Logs Import

## Task Goal

Fix the Vite import-analysis failure caused by `src/router/modules/remaining.ts` importing a missing DCC controlled-file logs view in the running D checkout.

## Milestones

- [x] Create task documentation and capture reported failure.
- [x] Reproduce the missing import through a deterministic frontend check.
- [x] Identify root cause and implement the smallest formal fix.
- [x] Add or update regression coverage for the route import.
- [x] Run targeted frontend verification and record evidence.
- [ ] Prepare closeout records.

## Expected Verification

- A RED check confirms `@/views/dcc/controlled-file/logs/index.vue` is unresolved before the fix.
- A GREEN check confirms the DCC controlled-file logs static contract passes after the fix.
- Full Vite build verification is attempted or records the exact blocker.

## Current Status

blocked

## Design Constraints Check

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是
- `是否存在临时补丁或绕过`：否

## Experience Gate

- `docs/experience-index.md` was read after task directory creation.
- No specific long-lived experience gate matched this missing `controlled-file/logs/index.vue` frontend route import. General frontend, E2E, task closeout, and PowerShell UTF-8 rules apply.

## Implementation Summary

- Restored `D:\ProjectPackage\IntRuoyi\IntRuoyiAll\IntRuoyiFronted\src\views\dcc\controlled-file\logs\index.vue` from the already-verified E checkout implementation.
- Added a narrow `.gitignore` exception so `IntRuoyiFronted/src/views/dcc/controlled-file/logs/index.vue` is not hidden by the broad `logs/` runtime-output ignore rule.
- Aligned the D checkout static contract SQL fixture path from legacy `ruoyi-vue-pro/sql/mysql/...` to current `IntRuoyiBackend/sql/mysql/...`, matching the current repository layout and E checkout.

## Experience Consolidation Summary

- Added a reusable frontend .gitignore gate to docs/frontend-development.md.
- Added an experience-index route for Vite import-analysis Failed to resolve import on source folders hidden by logs/.

## Remaining Closeout Blocker

- Final commit/push closeout was not performed because the D checkout had unrelated task-doc modifications during final status checks. These pre-existing/concurrent changes must be reconciled before committing this task's files.
## Final Remote Closeout Blocker

- `git push origin int_main` and `git push origin int_shedule` both failed twice with `TLS connect error: error:0A000126:SSL routines::unexpected eof while reading`.
- Local implementation commits exist, but the task cannot be marked `completed` until the branches push successfully and status no longer reports ahead of origin.