# Task: CRM route audit

## Goal

Route through every child tab under the CRM system in the frontend and verify the real user path does not break.

## Scope

- Use Playwright against the running frontend and backend.
- Navigate the CRM top-level menu and each visible CRM child tab.
- Record which routes load successfully and which ones fail.
- If a route fails because of frontend code or missing frontend wiring, fix it and rerun the affected path.
- If a route fails because of backend/runtime prerequisites, record the exact blocker without adding fallback behavior.

## Milestones

- [x] M1: Previous frontend task checked and confirmed completed.
- [x] M2: Frontend task documentation created before route audit work.
- [x] M3: CRM route inventory collected and baseline navigation started.
- [x] M4: Broken routes fixed or blocked with exact evidence.
- [x] M5: Full CRM route audit rerun completed.
- [x] M6: Evidence updated and task finalized.
- [x] M7: Task changes committed separately after verification passes.

## Expected Verification

- Playwright can log in and open the CRM top-level menu.
- Every visible child route under CRM is opened at least once.
- For each child route, the result is recorded as pass or blocked with exact error evidence.

## Current Status

Completed. The frontend dev server on `http://127.0.0.1:8081` and the backend on `http://127.0.0.1:48081` were used for a real Playwright route audit. All 20 visible CRM child routes loaded successfully without frontend runtime errors, backend API failures, or missing-schema notifications.

## Blocker and Impact

- Blocker: None.
- Impact: No remaining CRM route blocker was found in this audit scope.
