# Task: Fix missing tenant-id during login

## Goal

Ensure the real local login flow resolves a valid tenant before calling `/system/auth/login`, and block invalid tenant input on the frontend instead of continuing into a misleading backend error.

## Scope

- Reproduce the real local login flow.
- Verify tenant resolution behavior before `/system/auth/login`.
- Verify that invalid tenant input is blocked before login submission.
- Record BDD, RED, and GREEN evidence for the real frontend path.

## Milestones

- [x] M1: Review the prior frontend task state and record any blocker before starting.
- [x] M2: Create the task document and execution log.
- [x] M3: Finish real-environment reproduction and confirm the exact failing breakpoint.
- [x] M4: Record RED evidence.
- [x] M5: Confirm the minimum frontend fix present in the branch for tenant resolution and safe loading cleanup.
- [x] M6: Record GREEN verification.
- [x] M7: Prepare a scoped frontend commit.

## Expected Verification

- Opening `http://localhost:8081/login` resolves a valid tenant before `/system/auth/login`.
- Invalid tenant input is blocked before `/system/auth/login` is submitted.
- Successful login no longer triggers the backend error about a missing tenant identifier.
- `execution-log.md` contains BDD, RED, and GREEN evidence.

## Current Status

Completed.

## Final Verification

- Real local login verification passed in a fresh Playwright browser session:
  - `GET /admin-api/system/tenant/get-id-by-name?name=芋道源码` returned `200`
  - `POST /admin-api/system/auth/login` returned `200`
  - the browser landed on `http://localhost:8081/index`
- After restarting the local `8081` Vite dev process to load the current shared `DocAlert` implementation, a normal browser click on the visible `登录` button also completed successfully and landed on `http://localhost:8081/index`
- Real invalid-tenant verification passed in a separate fresh Playwright browser session:
  - `GET /admin-api/system/tenant/get-id-by-name?name=不存在租户` returned `200`
  - no `/admin-api/system/auth/login` request was sent afterward

## Residual Notes

- The earlier Vite ESLint overlay on the login page came from a stale local `8081` dev process rather than current repository code. Restarting the frontend dev server cleared the overlay, and the normal button click path then worked as expected.
