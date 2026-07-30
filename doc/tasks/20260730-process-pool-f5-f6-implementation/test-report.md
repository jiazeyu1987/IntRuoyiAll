# Test Report

## Status

ready_for_closeout_pending_push_retry

## Passed

- Maven F5/F6/timeline targeted regression: PASS, 33 tests.
- Maven F5/F6 post-UI focused regression: PASS, 30 tests.
- F5 API static contract: PASS.
- F6 API static contract: PASS.
- F5/F6 combined static contract: PASS.
- F5/F6 frontend write-path static contract: PASS.
- F5/F6 frontend write-path Playwright E2E: PASS, 2 tests against real `8081/48081`.
- F5/F6 DB verification: PASS, RUN3 review copy clamps `pressure=50` to `40`; RUN3 original record revision updates event `6` `outputQuantity=91`.
- Timeline mapper static contract: PASS.
- Timeline frontend static contract: PASS.
- Frontend TypeScript check: PASS.
- Branch runtime port guard: PASS.
- Acceptance plan validation: PASS.
- `git diff --check`: PASS, CRLF conversion warnings only.

## Not Passed Or Not Run

- Final push has not yet succeeded after the frontend write-path closeout changes. Previous `git push origin int_main` and `git ls-remote --heads origin int_main` failed with `Recv failure: Connection was reset`.
- The previous `214274437` byte blob `doc/tasks/20260729-local-scheduler-tenant-copy/source-tenant-1-full-config.json` has been removed from pending history after user confirmed it must not be committed to Git; post-cleanup largest pending blob is about `2 MB`.
