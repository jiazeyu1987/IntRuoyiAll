# Task: DCC Distribution Real-Content E2E

## Goal

Add and run a real end-to-end verification for the `DCC下发` page so the test
proves the page is rendering real category and distribution-rule content from
the live backend, not only an empty shell or static labels.

## Scope

- Check the previous DCC real-content frontend task in this repository and
  explicitly record whether it blocks this task.
- Create this task package before adding any new verification script.
- Use the real frontend entry `http://127.0.0.1:8081`, real login, real browser
  path, and real backend APIs for DCC distribution verification.
- Verify real content from:
  - `/admin-api/dcc/file-categories`
  - `/admin-api/dcc/file-categories/{id}/distribution-rules`
  - `/admin-api/system/dept/simple-list`
- Fail fast if the runtime has no active category or no category with real
  distribution rules, and record the exact blocker instead of masking it.
- Default to test-only delivery unless the E2E exposes a minimal frontend
  defect that must be fixed for the requested verification to become truthful.

## Previous Task Check

- Previous frontend task:
  `doc/tasks/20260516-dcc-access-rules-real-content-e2e/task.md`
- Status before this task: blocked.
- Impact: the access-rules real-content task remains blocked by missing live
  rule data, but that blocker does not prevent this distribution-page
  verification from proceeding independently.

## Milestones

- [x] M1: Create this task package and record the live-path verification goal.
- [x] M2: Record BDD scenarios and RED evidence for the missing distribution
  real-content verification coverage.
- [x] M3: Implement the Playwright verification that compares live page content
  with backend category and distribution-rule data.
- [x] M4: Run GREEN verification and update QA evidence with the exact result or
  blocker.
- [x] M5: Commit only the files produced by this task if verification fully
  passes and the write set is clean.

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-distribution-real-content-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-distribution-real-content-e2e\scripts\verify-dcc-distribution-real-content-e2e.mjs`
- `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\docs\qa\test-suite-evidence.md`

## Current Status

Completed. The real-content Playwright verification is green against the live
runtime: `DCC下发` proves one real category (`产品技术要求`) and one real active
distribution rule (`瑛泰源码`) through the browser path plus backend contract
comparison, the QA evidence validates successfully, and the task-only keep set
passed closeout preview.

## Verification Summary

- First RED result: the initial read-only verifier failed because all 48 active
  categories returned zero distribution-rule rows.
- Runtime path used by final GREEN:
  - real login on `http://127.0.0.1:8081`
  - real DCC page `http://127.0.0.1:8081/dcc/controlled-file/distribution`
  - backend comparison against `/admin-api/dcc/file-categories`,
    `/admin-api/dcc/file-categories/1/distribution-rules`, and
    `/admin-api/system/dept/simple-list`
- Final GREEN result:
  - target category id: `1`
  - target category name: `产品技术要求`
  - warning visible: `true`
  - rule count: `1`
  - verified rule row: `瑛泰源码`, `active=true`
  - screenshot:
    `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\dcc-distribution-real-content-e2e-20260516.png`

## Cleanup Keep

- `doc/tasks/20260516-dcc-distribution-real-content-e2e/scripts/verify-dcc-distribution-real-content-e2e.mjs`
- `docs/qa/test-suite-evidence.md`

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-distribution-real-content-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-distribution-real-content-e2e\scripts\verify-dcc-distribution-real-content-e2e.mjs` -> PASS
- `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\docs\qa\test-suite-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260516-dcc-distribution-real-content-e2e --mode preview` -> PASS
