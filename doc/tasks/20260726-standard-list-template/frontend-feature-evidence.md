# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: Convert the screenshot's execution record list to the project standard list template, move it to a dedicated “测试记录” page, and place that menu entry between “测试管理” and “备份计划”.
- Non-goals: Change backend API contracts, test execution semantics, or introduce mock data.

## Requirements And Acceptance

- Acceptance: Test management keeps test item maintenance and execution actions, but no longer renders the embedded execution-record block.
- Acceptance: Test record uses the standard list template with stable table key, quick filter, column settings, table slot, row actions, and pagination.
- Acceptance: Menu migration creates “测试记录” with component `system/codex-test-record/index`, component name `SystemCodexTestRecord`, and sort order between test management and backup plan.

## UI Entry Points

- Test management page: `src/views/system/codex-test-management/index.vue`.
- Test record page: `src/views/system/codex-test-record/index.vue`.
- Menu migration: `IntRuoyiBackend/sql/mysql/20260726_system_codex_test_record_menu.sql`.
- Current tests: `tests/e2e/system-codex-test-management-static.spec.js` and `tests/e2e/system-codex-test-management-real.e2e.js`.

## API Contracts And Data States

- Existing APIs in `src/api/system/codexTestManagement/index.ts` remain unchanged.
- The conversion is UI structure only for execution records: query params, tenant selection, execution records, cancel action, detail drawer, and artifact preview keep the existing API state model.

## BDD Scenarios

- BDD: test-record-standard-list-template -> Given the user opens the system test record page, When execution records are rendered, Then filters, toolbar actions, table columns, row operations, pagination and spacing follow the project's standard list template while existing record actions remain available.
- BDD: test-record-menu-order -> Given the system management menu contains Test Management and Backup Plan, When the menu migration is applied, Then the Test Record menu appears between Test Management and Backup Plan with the correct route component and query permission.
- BDD: test-management-record-list-removed -> Given the user opens the test management page, When the page renders the test item list, Then the old embedded execution-record block is no longer rendered under the test item table.

## Verification

- RED: pending.
- GREEN: pending.
- Regression: pending.

## Responsive, Accessibility, Loading, Empty, Error, Permission Checks

- Pending implementation and verification.

## Blockers And Follow-Up Skills

- None currently.
