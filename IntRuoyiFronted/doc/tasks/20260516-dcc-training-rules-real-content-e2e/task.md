# Task: DCC Training-Rules Real-Content E2E

## Goal

Add and run a real end-to-end verification for the DCC training-rules page so
the test proves the page is rendering live category scope, live training-rule
rows, and live department names from the runtime backend instead of only
showing a non-empty shell.

## Scope

- Check the latest frontend task in this repository and confirm it is completed
  before starting this new QA task.
- Create this task directory and task documents before adding the new
  verification script.
- Use the real frontend entry `http://127.0.0.1:8081`, real login, real browser
  path, and real backend APIs for DCC training-rules verification.
- Verify real content from:
  - `/admin-api/dcc/file-categories`
  - `/admin-api/dcc/file-categories/{id}/training-rules`
  - `/admin-api/system/dept/simple-list`
- If the runtime currently has no training-rule rows, allow the script to add a
  minimal real department rule through the same training page, verify the live
  persisted content, and then restore the original page state.
- Fail fast if the runtime has no active category, no department choices, or
  the page cannot persist and re-render the real training-rule content.
- Default to test-only delivery unless the E2E exposes a minimal frontend defect
  that must be fixed for the requested verification to become truthful.

## Previous Task Check

- Previous frontend task:
  `doc/tasks/20260516-sidebar-brand-logo-replace/task.md`
- Status before this task: completed for implementation and verification.
- Impact: no unfinished frontend task blocks this isolated DCC training-rules
  E2E coverage work.

## Milestones

- [x] M1: Create this task package and execution log before writing the new
  verification script.
- [x] M2: Record BDD scenarios and RED evidence for the missing real-content
  verification coverage plus runtime prerequisites.
- [x] M3: Implement the Playwright verification that compares the training page
  with live API data.
- [x] M4: Run GREEN verification and update QA evidence.
- [ ] M5: Preview closeout and commit only the files produced by this task if
  verification fully passes.

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-training-rules-real-content-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-training-rules-real-content-e2e\scripts\verify-dcc-training-rules-real-content-e2e.mjs`
- `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-training-rules-real-content-e2e\qa-test-suite-evidence.md`

## Cleanup Keep

- `doc/tasks/20260516-dcc-training-rules-real-content-e2e/qa-test-suite-evidence.md`
- `doc/tasks/20260516-dcc-training-rules-real-content-e2e/scripts/verify-dcc-training-rules-real-content-e2e.mjs`

## Current Status

Completed for the requested E2E coverage. The real Playwright verification
passed against live DCC category `产品技术要求`, confirmed the warning state for
`trainingRequired=false`, and matched one real persisted training-rule row for
department `瑛泰源码`.

## Residual Runtime Note

- During RED debugging the runtime initially had zero training-rule rows across
  all 48 active categories, so a minimal real rule was created through the same
  training page to make row-level verification truthful.
- A later attempt to restore category `1` back to an empty rule list through
  `PUT /admin-api/dcc/file-categories/1/training-rules` with `[]` failed with
  `code=500` and `NoClassDefFoundError:
  org/hibernate/validator/internal/engine/ValidatorImpl$CascadingValueReceiver`.
- Impact: the runtime now retains one real training-rule row that the final
  GREEN verification reads successfully. Cleanup-to-empty needs a separate
  backend fix if the environment must return to its original zero-row state.

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-training-rules-real-content-e2e\scripts\verify-dcc-training-rules-real-content-e2e.mjs`
  -> PASS, returned `{"targetCategory":{"id":1,"name":"产品技术要求","trainingRequired":false},"needsSetup":false,"createdRuleDepartmentName":null,"cleanupPerformed":false,"ruleCountVerified":1,"rowAssertions":[{"index":0,"departmentId":100,"departmentName":"瑛泰源码","active":true}]}` and captured
  `output/playwright/dcc-training-rules-real-content-e2e-20260516.png`.
- `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-training-rules-real-content-e2e\qa-test-suite-evidence.md`
  -> PASS.
