# QA Test Suite Evidence

## Scope

Codex 自动测试管理功能，覆盖迁移合同、后端服务编排、前端静态合同和 Runner 脚本语法。

## Requirement-To-Test Matrix

- 测试管理员菜单与权限：`test_codex_test_management_migration.py` verifies menu permissions, role code, admin assignment, and tenant package menu merge.
- 自然语言测试项和任意检查点：`CodexTestCaseServiceImplTest` verifies method text, user-written data, and checkpoint persistence/replacement.
- 顺序/并行执行编排：`CodexTestExecutionServiceImplTest` verifies execution snapshots, Runner offline rejection, and unsafe parallel rejection.
- Runner 回写与失败证据：`CodexTestRunnerServiceImplTest` verifies claim, checkpoint failure schema, mismatch description, and rollup failure.
- 前端入口与展示契约：`system-codex-test-management-static.spec.js` verifies endpoint strings, permissions, tenant selector, pass/fail text, screenshots, and Runner protocol keywords.

## RED Evidence

- RED: `python -X utf8 -m pytest script\tests\test_codex_test_management_migration.py -q` -> FAIL, migration missing.
- RED: `node tests\e2e\system-codex-test-management-static.spec.js` -> FAIL, API wrapper/page/Runner missing.
- RED: Maven CodexTest service tests -> FAIL, backend services missing.

## GREEN Evidence

- GREEN: `python -X utf8 -m pytest script\tests\test_codex_test_management_migration.py -q` -> 2 passed.
- GREEN: `mvn -pl yudao-module-system -am "-Dtest=CodexTestCaseServiceImplTest,CodexTestExecutionServiceImplTest,CodexTestRunnerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> 6 passed.
- GREEN: `node tests\e2e\system-codex-test-management-static.spec.js` -> passed.
- GREEN: `node --check scripts\codex-test-runner.mjs` -> passed.

## Verification

- Migration, backend service, frontend static, and Runner syntax gates pass for this feature scope.
- Full frontend type-check remains blocked by unrelated DCC browser file type errors.

## Blocked Tests

- `pnpm ts:check` / `vue-tsc` fails on pre-existing DCC browser type errors in `src/views/dcc/controlled-file/browser/index.vue`.
- Real E2E is blocked until local runtime, Runner token, Codex CLI, Playwright/browser, target test tenant, credential mapping, and cleanup ownership are confirmed.

## Blockers

- Resolve unrelated DCC type errors before claiming global frontend `ts:check` pass.
- Confirm Runner and tenant credentials before running real Playwright execution.

## Release Recommendation

Feature-level static/backend/migration verification passes. Do not mark full release readiness until unrelated DCC type-check blocker is resolved and real Runner E2E prerequisites are confirmed.
