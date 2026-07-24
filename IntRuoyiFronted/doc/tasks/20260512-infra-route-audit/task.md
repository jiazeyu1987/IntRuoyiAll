# Task: Infrastructure route audit

## Goal

Route through every visible child tab under the Infrastructure menu in the frontend and verify the real user path does not break.

## Scope

- Use Playwright against the running frontend and backend.
- Navigate the Infrastructure top-level menu and each visible Infrastructure child route.
- Record which routes load successfully and which ones fail.
- If a route fails because of frontend code or missing frontend wiring, fix it and rerun the affected path.
- If a route fails because of backend/runtime prerequisites, record the exact blocker without adding fallback behavior.

## Milestones

- [x] M1: Previous frontend task checked and explicitly blocked before new work.
- [x] M2: Infrastructure route audit documentation created before route audit work.
- [x] M3: Infrastructure route inventory collected and baseline navigation started.
- [x] M4: Broken routes fixed or blocked with exact evidence.
- [x] M5: Full Infrastructure route audit rerun completed.
- [x] M6: Evidence updated and task finalized.
- [x] M7: Task changes committed separately after verification passes.

## Expected Verification

- Playwright can log in and open the Infrastructure top-level menu.
- Every visible child route under Infrastructure is opened at least once.
- For each child route, the result is recorded as pass or blocked with exact error evidence.
- Frontend verification commands pass after any code change.

## Current Status

Completed on 2026-05-12. All 21 visible Infrastructure child routes pass in the local environment.

## Blocker and Impact

- Blocker: none.
- Impact: none.
