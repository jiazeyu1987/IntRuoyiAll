# Task: Fix Frontend Build Blocker For Publish Script Reverify

## Goal

Remove the current frontend lint/build blocker that prevents the real IntRuoyi test-server publish script from completing end to end.

## Scope

- Confirm the latest same-repository frontend task is explicitly completed before starting this blocker-fix task.
- Record BDD for build-gate behavior before changing production code.
- Apply only the minimal frontend fix needed for the current `pnpm exec vite build --mode test` failure.
- Run the real frontend build command used by the publish script to prove the blocker is gone.

## Previous Task Check

- Previous frontend task: `doc/tasks/20260518-workorder-row-freeze-toggle-action/task.md`
- Status before this task: completed.
- Impact: the latest same-repository frontend task is already closed, so this build-blocker fix can proceed.

## Milestones

- [x] M1: Confirm the previous frontend task is closed and create this blocker-fix task package.
- [x] M2: Record BDD and RED evidence for the current build blocker.
- [x] M3: Implement the minimal frontend fix.
- [x] M4: Run the real build verification used by the publish script.

## Expected Verification

- `pnpm exec vite build --mode test`

## Current Status

Completed on 2026-05-18. The frontend build blocker in `src/views/ai/music/manager/TtsTestPane.vue` was removed and the publish-script build command now passes again.

## Final Verification Result

- PASS: `pnpm exec vite build --mode test` with the same runtime overrides used by the publish script

## Blocker And Impact

- Blocker: current frontend test build fails on `vue/html-self-closing` for the `audio` element in `TtsTestPane.vue`.
- Impact: resolved. The publish script no longer stops on this frontend lint error.
