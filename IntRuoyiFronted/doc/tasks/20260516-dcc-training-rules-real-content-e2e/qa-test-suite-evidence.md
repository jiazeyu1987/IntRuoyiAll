# QA Test Suite Evidence

## Scope And Target

- Feature under test: DCC training-rules real-content E2E coverage.
- Runtime target: `http://127.0.0.1:8081` frontend plus
  `http://127.0.0.1:48081` backend.
- Task package:
  `doc/tasks/20260516-dcc-training-rules-real-content-e2e/`

## Requirement To Test Matrix

- Requirement: the DCC training page must be verified through the real frontend
  path.
  Test: Playwright logs in through the real login page, opens the real DCC
  training page, and selects a live category with training rules.
- Requirement: the page must show the real selected category and requirement
  warning state.
  Test: the script compares the selected category and warning visibility with
  live `/admin-api/dcc/file-categories` data.
- Requirement: the page must show real training-rule rows.
  Test: if the selected category has no existing rules, the script adds one
  real training department through the page, compares row count, department
  names, and active switches with live
  `/admin-api/dcc/file-categories/{id}/training-rules` plus
  `/admin-api/system/dept/simple-list`, then restores the original page state.
- Requirement: missing runtime prerequisites must fail fast.
  Test: the run stops with the exact blocker if active categories, training
  rules, or department lookup data are missing.

## Test Types

- E2E: applicable and required for the requested real user path.
- Regression: applicable because the repository currently lacks a dedicated
  real-content browser verification for the DCC training page.
- Accessibility: not part of this task scope.
- Compatibility: not part of this task scope.
- Performance: not part of this task scope.

## Test Data And Fixtures

- Real admin login on `http://127.0.0.1:8081`.
- Real tenant selected by the live login flow.
- Real DCC training page at `http://127.0.0.1:8081/dcc/controlled-file/training`.

## RED:

- Pre-task coverage gap -> FAIL, because
  `doc/tasks/20260516-dcc-training-rules-real-content-e2e/scripts/verify-dcc-training-rules-real-content-e2e.mjs`
  did not exist before this task.
- Direct runtime probe with a fresh real admin login -> FAIL for preexisting
  row-level content, because the live runtime currently returns
  `activeCategoryCount=48`, `deptCount=12`, `matchingCategoryCount=0`, and
  `trainingRequiredTrueCount=0`.
  The page therefore has real categories and departments available, but no
  existing training-rule rows to compare, so the E2E must create minimal real
  content through the same page before verifying persisted content.

## GREEN:

- `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-training-rules-real-content-e2e\scripts\verify-dcc-training-rules-real-content-e2e.mjs`
  -> PASS, returned one verified live row for category `产品技术要求` and
  department `瑛泰源码`, with warning visibility matching
  `trainingRequired=false`.

## Verification

- Implemented script:
  `doc/tasks/20260516-dcc-training-rules-real-content-e2e/scripts/verify-dcc-training-rules-real-content-e2e.mjs`
- Real page path:
  `http://127.0.0.1:8081/dcc/controlled-file/training`
- Screenshot artifact:
  `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\dcc-training-rules-real-content-e2e-20260516.png`
- Evidence validation:
  `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-training-rules-real-content-e2e\qa-test-suite-evidence.md`
  -> PASS.

## Blockers

- Requested read-side E2E coverage: none.
- Residual runtime cleanup gap: attempting to replace category `1` training
  rules with an empty array currently returns `code=500` and
  `NoClassDefFoundError:
  org/hibernate/validator/internal/engine/ValidatorImpl$CascadingValueReceiver`,
  so the live environment retains one real training-rule row after task
  debugging.
