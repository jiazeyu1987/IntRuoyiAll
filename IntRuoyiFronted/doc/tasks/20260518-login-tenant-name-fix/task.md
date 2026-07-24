# Task: Fix Login Tenant Name Default

## Goal

Fix the login page so users no longer have to manually replace the tenant name with `芋道源码` before logging in.

## Scope

- Confirm the latest same-repository frontend task is explicitly completed before starting this login fix.
- Record BDD and RED evidence for the tenant-name mismatch before production changes.
- Apply only the minimal frontend fix needed to align the visible login tenant defaults and remembered login-form cache with the live backend tenant name.
- Run targeted real-browser regression verification and frontend type checking.

## Previous Task Check

- Previous frontend task: `doc/tasks/20260518-publish-script-reverify/task.md`
- Status before this task: completed.
- Impact: the latest same-repository frontend task is already closed, so this login-tenant fix can proceed.

## Milestones

- [x] M1: Record BDD and reproduce the current login tenant-name mismatch.
- [x] M2: Add a failing browser regression script for the tenant default and remembered-login cache path.
- [x] M3: Implement the minimal frontend fix.
- [x] M4: Run GREEN verification on the real login page and targeted type checking.
- [x] M5: Commit only task-scoped frontend files after required verification passes.

## Expected Verification

- `node --check D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\login-tenant-name-default.test.mjs`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session login-tenant-name-default open http://127.0.0.1:8081/login`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session login-tenant-name-default run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\login-tenant-name-default.test.mjs`
- `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260518-login-tenant-name-fix --mode preview`

## Current Status

Completed on 2026-05-18. The login tenant-name mismatch is fixed, browser regression verification passed, and the task is ready for a task-scoped frontend commit.

## Final Verification Result

- PASS: `node --check D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\login-tenant-name-default.test.mjs`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session login-tenant-name-default open http://127.0.0.1:8081/login`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session login-tenant-name-default run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\login-tenant-name-default.test.mjs`
- PASS: `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260518-login-tenant-name-fix --mode preview`

## Blocker And Impact

- Blocker: none.
- Impact: users can now land on a login form that already matches the live backend tenant name, and remembered-login cache no longer revives the retired tenant label.

## Cleanup Keep

- doc/tasks/20260518-login-tenant-name-fix/bug-regression-evidence.md
- scripts/login-tenant-name-default.test.mjs
