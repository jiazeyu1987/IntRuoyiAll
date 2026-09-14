# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: Improve DCC training/read-confirmation UX by exposing countability, acknowledgement eligibility, training completion summaries, pending users, and manual-release permission gaps.
- Non-goal: Do not change backend contracts, roles, permissions, training completion logic, release APIs, or unrelated DCC pages.

## Requirements And Acceptance IDs

- ACC-1: Training task page shows countability state and focus/preview/timing reason.
- ACC-2: Acknowledgement button disabled state is explained near the action.
- ACC-3: DCC detail training section summarizes completion count and pending users.
- ACC-4: DCC detail page explains missing `DISTRIBUTE` permission when manual release cannot be performed.
- ACC-5: Training rule entry points warn that recipients need `dcc:controlled-file:training:mine`.

## UI Entry Points, Routes, Components, Owned Files

- Route: `/dcc/controlled-file/training-task/:progressId`
- Component: `IntRuoyiFronted/src/views/dcc/controlled-file/training/task/index.vue`
- Route: `/dcc/controlled-file/detail/:id`
- Component: `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue`
- Presentation helper: `IntRuoyiFronted/src/views/dcc/controlled-file/detail/presentation.ts`
- Components: `IntRuoyiFronted/src/views/dcc/controlled-file/training/components/TrainingRulesReadonlyTab.vue`, `IntRuoyiFronted/src/views/dcc/controlled-file/categories/components/CategoryTrainingRulesTab.vue`
- Static contract: `IntRuoyiFronted/tests/e2e/dcc-training-ux-prechecks-static.spec.cjs`

## API Contracts And Data States

- Existing APIs only: training task detail, preview, view-session start/heartbeat/stop, acknowledge, controlled file detail.
- No new API field required for this iteration.
- Permission gap is inferred from existing page state: file waits for manual distribution while the formal release action is unavailable.

## BDD Scenarios

- See `execution-log.md`.
- BDD: Training UX prechecks -> Given a training task, controlled-file detail, or training rule entry is opened, When countability, completion, release permission, or recipient permission affects the next action, Then the page shows the real state and required precondition.

## RED Command And Expected Failure

`pnpm --dir "E:\IntRuoyi\IntRuoyiFronted" e2e:dcc:training-ux-prechecks:static`

Expected and observed failure before implementation: the contract could not find the stable marker `dcc-training-task-countability-state`.

- RED: The task-specific contract failed before implementation because `dcc-training-task-countability-state` was missing.

## GREEN Command And Passing Result

- `pnpm --dir "E:\IntRuoyi\IntRuoyiFronted" e2e:dcc:training-ux-prechecks:static` -> PASS.
- `pnpm --dir "E:\IntRuoyi\IntRuoyiFronted" e2e:dcc:detail-training-summary:static` -> PASS.
- `node "E:\IntRuoyi\IntRuoyiFronted\tests\e2e\dcc-training-rules-context-static.spec.js"` -> PASS.
- `pnpm --dir "E:\IntRuoyi\IntRuoyiFronted" ts:check` -> PASS.
- Focused `git diff --check` for task-owned implementation paths -> PASS.
- GREEN: The task-specific contract and all focused adjacent checks passed after implementation.

## UX Checks

- Responsive: No layout-breaking fixed width; messages use existing Element Plus alert/card style.
- Accessibility: Status text is visible as plain text near action buttons; no color-only meaning.
- Loading/error: Existing load error path remains unchanged; new hints do not swallow API errors.
- Empty: The management summary explicitly reports no training recipients or no pending users.
- Permission: Formal release permission gap is displayed as guidance only; no role or backend bypass is added.

## E2E Or Component Verification Path

- Primary: task-specific static contract.
- Regression: targeted Vue/TypeScript check plus existing detail-summary and training-rule static contracts.
- Real browser rerun was not added to this UX-only task; the previously verified DCC training real path remains the runtime baseline, while this task verifies the new presentation and precheck contracts.

## Blockers And Follow-Up Skills

- Existing unrelated `e2e:dcc:training-summary:static` expects a historical table marker in an untouched page.
- Existing permission distribution training contracts reference a missing historical SQL path outside this task.
- The frontend has no existing recipient-permission inspection API, so ACC-5 provides an explicit precheck instruction rather than claiming automatic per-user validation.
- Current branch integration/push is blocked by mixed commits and unrelated workspace changes.
