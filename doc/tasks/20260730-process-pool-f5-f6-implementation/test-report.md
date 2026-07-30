# Test Report

## Status

blocked_on_remote_push_with_e2e_prereq_gap

## Passed

- Maven F5/F6/timeline targeted regression: PASS, 33 tests.
- F5 API static contract: PASS.
- F6 API static contract: PASS.
- F5/F6 combined static contract: PASS.
- Timeline mapper static contract: PASS.
- Timeline frontend static contract: PASS.
- Frontend TypeScript check: PASS.
- Branch runtime port guard: PASS.
- Acceptance plan validation: PASS.
- `git diff --check`: PASS, CRLF conversion warnings only.

## Not Passed Or Not Run

- Real Playwright write-path E2E for `process-pool-review-copy-and-revision.spec.ts` was not run successfully because the current frontend has no `test:e2e` script, the named `test` runner does not recognize that target, and the spec file is absent.
