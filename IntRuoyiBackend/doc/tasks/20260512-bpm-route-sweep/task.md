# Task: BPM route sweep

## Goal

Use real frontend navigation to route through every visible child page under the workflow menu and fix any real frontend or backend error discovered during the sweep.

## Scope

- Discover the workflow/BPM child routes from the running admin system.
- Use Playwright against the real frontend entry point, not API-only shortcuts.
- Capture browser console warnings/errors, failed network requests, route load failures, and backend SQL or system exceptions.
- Fix confirmed workflow route failures without fallback, mock data, or hidden downgrade behavior.
- Leave unrelated MES, CRM, Report, AI, and other task files untouched and uncommitted.

## Previous Task Check

- Previous unfinished task: `doc/tasks/20260512-mes-route-sweep`.
- Status before this task: blocked because the user redirected the active sweep from MES to workflow/BPM.
- Impact: MES route validation is outside this BPM route-sweep commit.

## Milestones

- [x] M1: Task document and previous unfinished-task status recorded before route sweep work.
- [x] M2: Discover all workflow/BPM child routes to be tested.
- [x] M3: Run Playwright real-user navigation across all workflow/BPM child routes and collect errors.
- [x] M4: Fix confirmed workflow/BPM route failures and update verification evidence.
- [x] M5: Re-run route sweep cleanly and commit only current BPM route-sweep files.

## Route Set

Visible workflow/BPM child routes discovered from the authenticated admin menu:

- `/bpm/manager/model`
- `/bpm/manager/form`
- `/bpm/manager/category`
- `/bpm/manager/user-group`
- `/bpm/manager/process-listener`
- `/bpm/manager/process-expression`
- `/bpm/manager/process-instance/manager`
- `/bpm/manager/process-tasnk`
- `/bpm/task/create`
- `/bpm/task/my`
- `/bpm/task/todo`
- `/bpm/task/done`
- `/bpm/task/copy`
- `/bpm/oa/leave`

## Expected Verification

- Playwright logs in through the real frontend and navigates each workflow/BPM child route.
- Every visited workflow/BPM route loads without unhandled frontend warnings/errors.
- Network responses for route initialization do not return disabled-module, schema-not-imported, missing-route, or system-exception responses.
- Backend log tail shows no new BPM SQL or system exceptions during the final clean sweep.

## Current Status

Completed. All 14 visible workflow/BPM child routes were reached through real frontend navigation. One real frontend warning was found on `/bpm/task/my`, fixed, and the full route set was rerun cleanly.

## Blocker And Impact

- Blocker: none.
- Impact: the workflow menu now routes cleanly across the verified child pages in this local environment.

## Final Verification Result

- Route discovery confirmed 14 visible workflow/BPM child routes from the authenticated admin menu.
- Full Playwright rerun across all 14 routes returned `warningCount=0`, `errorCount=0`, and `adminIssueCount=0` for every route.
- Backend log review during the final sweep showed BPM requests completing without new BPM SQL or system exceptions.
- Related frontend fix commit: `bea756e3 任务: 修复流程页签状态告警`.
