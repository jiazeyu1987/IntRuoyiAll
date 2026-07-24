# Task: Report Management route sweep

## Goal

Route through every visible child tab under the Report Management menu in the frontend and verify the real user path does not break.

## Scope

- Use Playwright against the running frontend and backend.
- Navigate the Report Management top-level menu and each visible child route.
- Record which routes load successfully and which ones fail.
- If a route fails because of frontend code or missing frontend wiring, fix it and rerun the affected path.
- If a route fails because of backend/runtime prerequisites, record the exact blocker without adding fallback behavior.

## Previous Task Check

- Previous unfinished frontend task: `doc/tasks/20260512-infra-route-audit`.
- Status before this task: blocked because the user redirected the active sweep from Infrastructure to Report Management.
- Impact: Infrastructure route validation remains incomplete and is outside this Report Management route-sweep commit.

## Milestones

- [x] M1: Previous frontend task checked and explicitly blocked before new work.
- [x] M2: Report Management route-sweep documentation created before route audit work.
- [x] M3: Report Management route inventory collected and baseline navigation started.
- [x] M4: Broken routes fixed or blocked with exact evidence.
- [x] M5: Full Report Management route sweep rerun completed.
- [x] M6: Evidence updated and task finalized.
- [x] M7: Task changes committed separately after verification passes.

## Expected Verification

- Playwright can log in through the real frontend.
- Every visible child route under Report Management is opened at least once.
- For each child route, the result is recorded as pass or blocked with exact error evidence.
- Frontend verification commands pass after any code change.

## Current Status

Completed. Real browser verification now opens every visible Report Management child route successfully.

## Final Verification

- Frontend route audit summary: `total=3`, `passed=3`, `blocked=0`.
- Verified routes: `/report/jimu-report`, `/report/jimu-bi`, `/report/go-view`.
- Runtime used for verification: frontend `http://localhost:8081`, backend `http://localhost:48081`, GoView frontend `http://127.0.0.1:3000`.
