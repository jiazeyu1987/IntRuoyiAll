# Frontend Backend Dict Timeout

## Task Goal

Fix the frontend startup/navigation timeout where Axios aborts after 30000ms while loading system dictionary data from the backend.

## Milestones

- [x] Capture the reported frontend Axios timeout and identify the suspected API.
- [ ] Reproduce the timeout with real frontend/backend runtime context.
- [ ] Isolate the backend, proxy, authentication, or database root cause.
- [ ] Add or update regression coverage before the production fix.
- [ ] Implement the minimal fix.
- [ ] Verify frontend can load without the Axios timeout.

## Expected Verification

- BDD/RED evidence identifies the failing frontend-to-backend path.
- The fixed path returns before the 30000ms Axios timeout.
- Targeted tests pass.
- Real frontend navigation no longer reports the uncaught Axios timeout.

## Current Status

in_progress
