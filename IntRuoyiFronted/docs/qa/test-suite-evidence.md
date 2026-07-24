# QA Test Suite Evidence

## Scope And Target

- Feature under test: DCC distribution real-content E2E coverage.
- Runtime target: `http://127.0.0.1:8081` frontend plus `http://127.0.0.1:48081`
  backend.
- Task package:
  `doc/tasks/20260516-dcc-distribution-real-content-e2e/`

## Requirement To Test Matrix

- Requirement: the `DCC下发` page must be verified through the real frontend
  path.
  Test: the Playwright script logs in through the real login page, opens
  `/dcc/controlled-file/distribution`, selects a real category, and waits for
  the live rule table.
- Requirement: the page must show real category and distribution-rule content,
  not only shell text.
  Test: the script compares the visible category selection, warning state, rule
  row count, department label, and active switch with live responses from
  `/admin-api/dcc/file-categories`,
  `/admin-api/dcc/file-categories/{id}/distribution-rules`, and
  `/admin-api/system/dept/simple-list`.
- Requirement: missing live prerequisites must fail fast.
  Test: the first RED run stops with the exact blocker when all active
  categories expose zero distribution rules.

## Test Types

- E2E: applicable and required for the requested real user path.
- Regression: applicable because the repository previously lacked a
  content-level DCC distribution browser verification.
- Accessibility: not part of this task scope.
- Compatibility: not part of this task scope.
- Performance: not part of this task scope.

## Test Data And Fixtures

- Real admin login on `http://127.0.0.1:8081`.
- Real active DCC category `产品技术要求` (`id=1`).
- Real DCC distribution page:
  `http://127.0.0.1:8081/dcc/controlled-file/distribution`
- Real live rule row:
  - department id: `100`
  - department label: `瑛泰源码`
  - active: `true`

## RED:

- Pre-task coverage gap -> FAIL, because the repository had no Playwright E2E
  that proved the `DCC下发` page was rendering real category and distribution
  rule content.
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-distribution-real-content-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-distribution-real-content-e2e\scripts\verify-dcc-distribution-real-content-e2e.mjs`
  -> FAIL, `no_active_category_with_distribution_rules:activeCategoryCount=48:sample=1:产品技术要求|2:生产用设备清单|3:检验用设备清单|4:说明书|5:包装设计`.
- `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat`
  -> FAIL, the standard workspace restart path failed fast at the missing Docker
  Desktop engine, so backend recovery had to be performed manually while MySQL
  `23306` and Redis `26379` remained reachable.

## GREEN:

- Manual backend recovery -> PASS, a runtime copy of
  `ruoyi-vue-pro\yudao-server\target\yudao-server.jar` was launched with the
  standard local MySQL/Redis arguments and
  `http://127.0.0.1:48081/v3/api-docs` returned HTTP `200`.
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-distribution-real-content-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-distribution-real-content-e2e\scripts\verify-dcc-distribution-real-content-e2e.mjs`
  -> PASS, the real browser flow selected `产品技术要求`, confirmed the warning
  state, and matched the visible rule row to backend department `瑛泰源码`
  with `active=true`.

## Verification

- Implemented script:
  `doc/tasks/20260516-dcc-distribution-real-content-e2e/scripts/verify-dcc-distribution-real-content-e2e.mjs`
- Final verified page:
  `http://127.0.0.1:8081/dcc/controlled-file/distribution`
- Final verified category:
  - id: `1`
  - name: `产品技术要求`
  - `distributionRequired=false`
- Final verified rule count: `1`
- Screenshot artifact:
  `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\dcc-distribution-real-content-e2e-20260516.png`

## Blockers

- none
