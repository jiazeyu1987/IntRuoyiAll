# Task: MES route sweep

## Goal

Use real frontend navigation to route through every subpage under the MES system menu and fix any real frontend or backend error discovered during the sweep.

## Scope

- Discover the MES menu routes from the running admin system or frontend route/menu definitions.
- Use Playwright against the real frontend entry point, not API-only shortcuts.
- Capture browser console errors, failed network requests, route load failures, and backend SQL or system exceptions.
- Fix confirmed MES route failures without fallback, mock data, or hidden downgrade behavior.
- Leave unrelated BPM/CRM task files untouched and uncommitted.

## Previous Task Check

- Previous unfinished task: `doc/tasks/20260512-bpm-schema-repair`.
- Status before this task: blocked / paused due the newer MES route sweep request.
- Impact: BPM schema work remains incomplete and is outside this MES route sweep commit.

## Milestones

- [x] M1: Task document and previous unfinished-task status recorded before route sweep work.
- [x] M2: Discover all MES child routes to be tested.
- [x] M3: Run Playwright real-user navigation across all MES child routes and collect errors.
- [x] M4: Fix confirmed MES route failures and update verification evidence.
- [x] M5: Re-run route sweep cleanly and prepare only current task files for commit.

## Expected Verification

- Playwright logs in through the real frontend and navigates each MES child route.
- Every visited MES route loads without unhandled frontend errors.
- Network responses for MES route initialization do not return missing-route, schema-not-imported, or system-exception responses.
- Backend log tail shows no new MES route SQL syntax or system exceptions during the sweep.

## Current Status

Completed. The real frontend sweep covered 61 MES leaf routes. The first sweep found 54 failing routes caused by missing local `mes_*` tables. After generating and importing a complete MySQL MES base schema covering 133 MES tables, the second sweep completed with 0 failing routes.
