# Task: Production Work Order Row Freeze Toggle Action

## Goal

Replace the production work-order list row action `新增` with a status-driven `冻结/解冻` action so frozen rows show `解冻` and non-frozen rows show `冻结`, with the row action wired to the real temporary-freeze API.

## Scope

- Confirm the latest same-repository frontend task is explicitly completed or blocked before starting this task.
- Inspect the production work-order list row action seam and the frontend API contract for row-level temporary freeze changes.
- Record BDD scenarios before production code changes.
- Add a failing regression verifier for the row freeze/unfreeze action label and handler wiring.
- Implement the minimal frontend change needed to replace the current child-create action with row freeze/unfreeze behavior.
- Run targeted source verification, real-page Playwright verification, and task-scoped static validation.

## Previous Task Check

- Previous frontend task: `doc/tasks/20260518-route-product-item-link/task.md`
- Status before this task: blocked due user priority switch.
- Impact: task-order policy is satisfied because the previous same-repository frontend task is explicitly paused before this higher-priority work-order action change starts.

## Milestones

- [x] M1: Block the previous same-repository frontend task and create this task package.
- [x] M2: Record BDD scenarios and add RED verification for the row freeze/unfreeze action.
- [x] M3: Implement the minimal production work-order list frontend change.
- [x] M4: Run GREEN verification and update evidence.
- [x] M5: Commit only task-scoped frontend files after required verification passes.

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-workorder-row-freeze-toggle-action\scripts\verify-workorder-row-freeze-toggle-source.mjs`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session workorder-row-freeze-toggle run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-workorder-row-freeze-toggle-action\scripts\verify-workorder-row-freeze-toggle.mjs`
- `pnpm exec eslint src\views\mes\pro\workorder\index.vue src\api\mes\pro\workorder\index.ts`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260518-workorder-row-freeze-toggle-action/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260518-workorder-row-freeze-toggle-action --mode preview`

## Current Status

Completed on 2026-05-18. Frontend delivery, verification, closeout preview, and task-scoped commit are complete.

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-workorder-row-freeze-toggle-action\scripts\verify-workorder-row-freeze-toggle-source.mjs` -> PASS
- `pnpm exec eslint src\views\mes\pro\workorder\index.vue src\api\mes\pro\workorder\index.ts` -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli --session workorder-row-freeze-toggle run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-workorder-row-freeze-toggle-action\scripts\verify-workorder-row-freeze-toggle.mjs` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260518-workorder-row-freeze-toggle-action/frontend-feature-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260518-workorder-row-freeze-toggle-action --mode preview` -> PASS

## Blocker And Impact

- Blocker: none.
- Impact: none.

## Cleanup Keep

- doc/tasks/20260518-workorder-row-freeze-toggle-action/frontend-feature-evidence.md
- doc/tasks/20260518-workorder-row-freeze-toggle-action/scripts/verify-workorder-row-freeze-toggle-source.mjs
- doc/tasks/20260518-workorder-row-freeze-toggle-action/scripts/verify-workorder-row-freeze-toggle.mjs
