# Task: Rename current admin to 瑛泰管理员

## Goal

Change the current logged-in administrator display name from its existing value to `瑛泰管理员` through the real frontend user path.

## Scope

- Verify the current administrator nickname shown in the authenticated admin shell.
- Use the existing personal profile path to update the current user's nickname.
- Verify the new nickname is visible after the update.
- Do not change backend contracts, add fallback behavior, or redesign unrelated UI.

## Milestones

- [x] M1: Previous frontend task checked and explicitly blocked before starting this task.
- [x] M2: This task document was created before any production data change.
- [x] M3: Record BDD scenarios and run a RED Playwright check for the target nickname.
- [x] M4: Update the current admin nickname through the real profile UI path.
- [x] M5: Rerun the real login verification and record GREEN evidence.
- [x] M6: Update evidence, finalize the task, and prepare the scoped frontend task commit if verification passes.

## Expected Verification

- Playwright can log in to `http://127.0.0.1:8081` with the configured admin account.
- The current authenticated user nickname is updated to `瑛泰管理员`.
- The top-right user display and the profile form both show `瑛泰管理员`.
- No mock, fallback, or direct database write is used.

## Current Status

Completed. The current admin nickname was updated through the real profile page, and the homepage/profile verification now shows `瑛泰管理员`.

## Blocker And Impact

- Blocker: None at task start.
- Impact: None.

## Final Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session admin-name-probe run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260513-admin-name-yingtai-manager\scripts\probe-login-tenant.mjs` -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli --session admin-name-update-2 run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260513-admin-name-yingtai-manager\scripts\update-admin-name.mjs` -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli --session admin-name-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260513-admin-name-yingtai-manager\scripts\verify-admin-name.mjs` -> PASS
