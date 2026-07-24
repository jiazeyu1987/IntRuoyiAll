# Task: DCC Access-Rules Real-Content E2E

## Goal

Add and run a real end-to-end verification for the DCC access-rules page so the
test proves the page is rendering real directory and access-rule content from
the live backend, not only a non-empty shell or styling-level output.

## Scope

- Check the latest frontend task in this repository and explicitly block it if a
  newer user request supersedes it.
- Create this task directory and task documents before writing the new
  verification script.
- Use the real frontend entry `http://127.0.0.1:8081`, real login, real browser
  path, and real backend APIs for DCC access-rules verification.
- Verify real content from:
  - `/admin-api/dcc/directories/tree`
  - `/admin-api/dcc/directories/{id}/access-rules`
  - `/admin-api/system/user/simple-list`
  - `/admin-api/system/dept/simple-list`
  - `/admin-api/system/role/simple-list`
  - `/admin-api/system/post/simple-list`
- Fail fast if the runtime has no visible directory or no directory with access
  rules, and record the exact blocker instead of masking it.
- Default to test-only delivery unless the E2E exposes a minimal frontend defect
  that must be fixed for the requested verification to become truthful.

## Previous Task Check

- Previous frontend task:
  `doc/tasks/20260516-pro-schedule-calendar-summary-card-detail/task.md`
- Status before this task: blocked.
- Impact: the previous task remains documentation-only after user
  reprioritization and does not block this DCC access-rules verification task.

## Milestones

- [x] M1: Block the unfinished previous frontend task and create this task
  package.
- [x] M2: Record BDD scenarios and RED evidence for the missing real-content
  verification coverage.
- [x] M3: Implement the Playwright verification that compares real page content
  with live API data.
- [ ] M4: Run GREEN verification and update QA evidence.
- [ ] M5: Commit only the files produced by this task if verification fully
  passes.

## Expected Verification

- `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\docs\qa\test-suite-evidence.md`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-access-rules-real-content-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-access-rules-real-content-e2e\scripts\verify-dcc-access-rules-real-content-e2e.mjs`

## Current Status

Blocked. The content-level Playwright verification is implemented, but the real
runtime currently exposes 2699 visible directories and zero visible directories
with non-empty access-rule rows, so GREEN verification cannot complete.

## Blocker And Impact

- Blocker: every visible directory returned by `/admin-api/dcc/directories/tree`
  currently returns an empty array from `/admin-api/dcc/directories/{id}/access-rules`.
- Impact: the requested real-content E2E cannot truthfully verify row-level
  content until at least one visible directory has real access-rule data; under
  the repository fail-fast policy this task must stay blocked and uncommitted.
