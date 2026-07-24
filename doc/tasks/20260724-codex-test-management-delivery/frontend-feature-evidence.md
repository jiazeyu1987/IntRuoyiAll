# Frontend Feature Evidence

## Goal

Add `系统管理 > 测试管理` page for test administrators to manage natural-language Codex/Playwright test cases, choose a top-level test tenant, run selected cases sequentially or in parallel, and view pass/fail checkpoint evidence with screenshots.

## Owned Files

- `IntRuoyiFronted/src/api/system/codexTestManagement/index.ts`
- `IntRuoyiFronted/src/views/system/codex-test-management/index.vue`
- `IntRuoyiFronted/scripts/codex-test-runner.mjs`
- `IntRuoyiFronted/tests/e2e/system-codex-test-management-static.spec.js`
- `IntRuoyiFronted/package.json`

## UI Behavior

- Tenant selector is top-level.
- Test case form supports free natural-language method text and user-written test data.
- Checkpoint editor supports arbitrary add/delete/update.
- Sequential and parallel execution buttons call backend orchestration.
- Execution drawer shows pass/fail/block status, mismatch description, and failure screenshot preview.

## Acceptance

- Test administrators can maintain test cases from `系统管理 > 测试管理`.
- The page exposes `测试租户`, `自然语言测试方法`, `检查点`, `顺序执行`, `并行执行`, `通过`, `失败`, and `失败截图`.
- Requests failures remain visible through message errors; no empty `catch {}` is used.

## BDD

- BDD: 测试管理员页面操作 -> Given 用户拥有测试管理权限 / When 打开页面 / Then 可选择测试租户、维护检查点、启动顺序或并行执行并查看结果。
- BDD: 失败截图展示 -> Given 失败检查点含截图 artifact / When 打开执行详情 / Then 页面显示红色失败、差异描述和截图预览入口。

## Verification

- RED: static test failed before API wrapper/page/Runner existed.
- GREEN: `node tests\e2e\system-codex-test-management-static.spec.js` passed.
- GREEN: `node --check scripts\codex-test-runner.mjs` passed.
- Type check: `vue-tsc` no longer reports errors in `src/views/system/codex-test-management/index.vue`; current failure is unrelated existing DCC file `src/views/dcc/controlled-file/browser/index.vue`.

## Blockers

- Real Playwright E2E was not run because Runner token, Codex CLI, browser, target tenant credentials, and test data ownership were not confirmed.
