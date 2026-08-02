# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: Improve DCC training/read-confirmation UX by exposing countability, acknowledgement eligibility, training completion summaries, pending users, and manual-release permission gaps.
- Non-goal: Do not change backend contracts, roles, permissions, training completion logic, release APIs, or unrelated DCC pages.

## Requirements And Acceptance IDs

- ACC-1: Training task page shows countability state and focus/preview/timing reason.
- ACC-2: Acknowledgement button disabled state is explained near the action.
- ACC-3: DCC detail training section summarizes completion count and pending users.
- ACC-4: DCC detail page explains missing `DISTRIBUTE` permission when manual release cannot be performed.

## UI Entry Points, Routes, Components, Owned Files

- Route: `/dcc/controlled-file/training-task/:progressId`
- Component: `IntRuoyiFronted/src/views/dcc/controlled-file/training/task/index.vue`
- Route: `/dcc/controlled-file/detail/:id`
- Component: `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue`
- Presentation helper: `IntRuoyiFronted/src/views/dcc/controlled-file/detail/presentation.ts`
- Static contract: `IntRuoyiFronted/tests/e2e/dcc-training-ux-prechecks-static.spec.cjs`

## API Contracts And Data States

- Existing APIs only: training task detail, preview, view-session start/heartbeat/stop, acknowledge, controlled file detail.
- No new API field required for this iteration.
- Permission gap is inferred from existing page state: file waits for manual distribution while the formal release action is unavailable.

## BDD Scenarios

- See `execution-log.md`.

## RED Command And Expected Failure

待补充。

## GREEN Command And Passing Result

待补充。

## UX Checks

- Responsive: No layout-breaking fixed width; messages use existing Element Plus alert/card style.
- Accessibility: Status text is visible as plain text near action buttons; no color-only meaning.
- Loading/error: Existing load error path remains unchanged; new hints do not swallow API errors.
- Permission: Formal release permission gap is displayed as guidance only; no role or backend bypass is added.

## E2E Or Component Verification Path

- Primary: task-specific static contract.
- Optional regression: targeted Vue/TypeScript or existing nearby static contracts if current workspace state permits.

## Blockers And Follow-Up Skills

暂无。
