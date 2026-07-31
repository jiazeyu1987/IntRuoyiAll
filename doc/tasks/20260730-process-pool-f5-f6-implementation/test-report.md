# Test Report

## Status

completed

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

- None for the F5/F6 scope. The previous `214274437` byte blob was removed from pending history, and final `git push origin int_main` succeeded.
