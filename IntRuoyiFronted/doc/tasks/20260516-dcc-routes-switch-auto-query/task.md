# Task: DCC Approval Routes Switch Auto Query

## Goal

Make the `DCC审批路线` page automatically run the routes query and derived preview refresh when the user switches the file category, without requiring an extra click on `查询路线`.

## Scope

- Confirm the previous frontend task state before new production edits.
- Keep the change scoped to the DCC approval-routes page.
- Reproduce the missing auto-query behavior with the real frontend at `http://127.0.0.1:8081`.
- Preserve existing backend contracts and page layout.

## Previous Task Check

- Previous frontend task: `doc/tasks/20260516-dcc-access-rules-real-content-e2e/task.md`
- Status before this task: blocked
- Impact: the previous task remains blocked by missing live access-rule data and does not conflict with this approval-routes behavior fix.

## Milestones

- [x] M1: Create this task package and confirm the previous frontend task is blocked, not active delivery work.
- [x] M2: Record BDD scenarios and RED evidence for the missing auto-query behavior.
- [x] M3: Implement the minimal frontend fix so category switching auto-runs query plus preview.
- [x] M4: Run GREEN verification and update evidence.
- [x] M5: Commit only this task's frontend changes if verification fully passes.

## Expected Verification

- `npx eslint D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\dcc\controlled-file\routes\index.vue`
- `npx --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-routes-switch-auto-query\scripts\verify-dcc-routes-switch-auto-query.mjs`

## Current Status

Completed on 2026-05-16. Real-browser regression proves category switching auto-runs both the routes query and the derived preview refresh without requiring the user to click `查询路线`.

## Blocker And Impact

- Blocker: none remaining.
- Impact: users no longer need a second manual action after switching categories on the approval-routes page.

## Final Verification Result

- `npx --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-routes-switch-auto-query\scripts\verify-dcc-routes-switch-auto-query.mjs` -> PASS
- `npx eslint D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\dcc\controlled-file\routes\index.vue` -> PASS
- Real result:
  - baseline category `产品技术要求`
  - switched category `生产用设备清单`
  - automatic requests after switching: `1` routes request, `1` preview request
  - screenshot: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\dcc-routes-switch-auto-query-20260516.png`
