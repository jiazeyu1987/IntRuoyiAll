# Task: DCC Training Closed Loop

## Goal

Implement the DCC training closed loop UI so that:

- the DCC training menu becomes a dual-tab page with read-only rule mapping and
  training execution tracking
- training users get a dedicated `我的培训` path and training preview page
- the preview page shows accumulated focused viewing time toward 600 seconds
- the UI blocks acknowledgement before 600 seconds and enables it after the
  threshold
- admins can see which file and which user have completed training
- the full chain can be exercised by a real-data Playwright E2E

## Scope

- Create this frontend task package before task-specific edits.
- Add training execution, my training, and training preview UI flows.
- Update DCC training, file detail, router, and API client types.
- Add a real Playwright chain for admin + trainee validation.
- Keep unrelated dirty frontend files out of scope.
- Do not use mock data or fallback behavior.

## Previous Task Check

- Previous frontend task:
  `doc/tasks/20260516-batch-record-single-page-layout-constraints/task.md`
- Status before this task: blocked by user reprioritization before closeout.
- Impact: the paused batch-record layout task is unrelated and does not block
  this DCC training delivery.

## Milestones

- [x] M1: Check the previous frontend task state and create this task package.
- [x] M2: Record BDD scenarios and RED evidence for the missing UI/E2E chain.
- [x] M3: Add failing frontend/E2E coverage for tabs, timing, and acknowledge
  gating.
- [x] M4: Implement the DCC training dual-tab page, my-training flow, preview
  timing, and detail-page updates.
- [x] M5: Run focused GREEN frontend verification and update evidence.
- [x] M6: Commit only task-scoped frontend files after verification passes.

## Expected Verification

- Focused ESLint / type verification for changed DCC files
- Real Playwright E2E for admin + trainee closed loop
- QA evidence validation

## Current Status

Completed. The DCC training menu is now a dual-tab page, `我的培训` and the
dedicated training preview page exist, training detail pages run the
focused-view session loop, and the controlled-file detail page shows
accumulated seconds plus acknowledgement eligibility. The final one-pass real
Playwright rerun is also green.

## Final Verification Result

- `pnpm exec eslint src/api/dcc/controlledFile/training.ts src/api/dcc/controlledFile/workflow.ts src/views/dcc/controlled-file/training/index.vue src/views/dcc/controlled-file/training/presentation.ts src/views/dcc/controlled-file/training/components/TrainingRulesReadonlyTab.vue src/views/dcc/controlled-file/training/components/TrainingExecutionTab.vue src/views/dcc/controlled-file/training/mine/index.vue src/views/dcc/controlled-file/training/task/index.vue src/views/dcc/controlled-file/detail/index.vue src/views/dcc/controlled-file/detail/presentation.ts src/router/modules/remaining.ts`
  -> PASS
- `set NODE_OPTIONS=--max-old-space-size=8192 && pnpm exec vue-tsc --noEmit`
  -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-training-closed-loop run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-training-closed-loop\scripts\verify-dcc-training-closed-loop-real-e2e.mjs`
  -> PASS
- Real result:
  - controlled file id: `51`
  - training progress id: `67`
  - published file status: `ACTIVE`
  - focused viewing accumulated to `606+` seconds before acknowledge
  - `acknowledged_at` was written
  - `training-executions/page` returned the acknowledged row for file `51`
  - screenshot:
    `D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/output/playwright/dcc-training-closed-loop-real-e2e-20260517.png`

## Cleanup Keep

- doc/tasks/20260516-dcc-training-closed-loop/frontend-feature-evidence.md
- doc/tasks/20260516-dcc-training-closed-loop/qa-test-suite-evidence.md
- doc/tasks/20260516-dcc-training-closed-loop/scripts/verify-dcc-training-closed-loop-real-e2e.mjs
