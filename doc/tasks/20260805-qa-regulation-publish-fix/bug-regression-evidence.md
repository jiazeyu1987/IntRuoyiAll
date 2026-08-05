# Bug Regression Evidence

## Bug Summary And Expected Behavior

- Bug: QA 规程配置页顶部黄框只显示标题和接口提示，`DCC 项目代码` 选择框被拆到黄框下方独立卡片，用户反馈黄框里的内容不显示。
- Expected: 顶部黄框内同时显示 QA 标题、正式接口提示、必填 `DCC 项目代码` 选择框和加载失败重试区；选择项目后再显示 Tab 和规程内容。

## Reproduction Command Or Path

- Reproduction path: 打开 `/mes/pro/process-pool/qa-regulation`，观察顶部 QA 标题黄框与下方 DCC 项目选择卡片分离。
- RED: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，新增断言命中 `The DCC project selector must render inside the top yellow header panel instead of in a detached card.`

## Root Cause

- `QaRegulationPage.vue` 将顶部说明黄框和 `data-qa-regulation-dcc-project` 项目选择器拆成两个连续 `ContentWrap`，导致用户截图标注的黄框区域没有项目选择内容。
- 同时主 QA 静态合同跑到更深断言后发现手动绑定路线候选读取退成 `ProRouteApi.getRouteSimpleList()`，与正式产品绑定候选接口合同不一致。

## Regression Test Added Or Updated

- Updated `IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` to assert the DCC project selector renders inside the first/top yellow header `ContentWrap`.
- Existing adjacent contracts retained: `qa-regulation-manual-route-selectable-static.spec.cjs` and `qa-regulation-final-applicability-static.spec.cjs`.

## RED Command And Expected Failure

- RED: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，expected reason: 旧布局把 `DCC 项目代码` 选择框拆到顶部黄框外。

## GREEN Command And Passing Result

- GREEN: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/qa-regulation-manual-route-selectable-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/qa-regulation-final-applicability-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs doc/tasks/20260805-qa-regulation-publish-fix/task.md doc/tasks/20260805-qa-regulation-publish-fix/execution-log.md` -> PASS，仅有 Git CRLF 工作区提示。

## Verification

- The focused static contract covers the top yellow header layout and rejects a detached DCC project selector card.
- Adjacent QA contracts and `pnpm ts:check` confirm the layout change does not break manual route binding, final-inspection applicability, or Vue/TypeScript types.

## Risk And Regression Scope

- Scope is limited to `QaRegulationPage.vue` top layout, DCC project loading/error/retry placement, and formal manual-route candidate API selection.
- No backend contract, save/publish payload, route binding save endpoint, QA rule rows, inspection item seed data, or publish precheck behavior changed.

## Blockers And Follow-Up Actions

- `node tests/e2e/unified-list-template-empty-tabs-system-static.spec.js` is blocked by unrelated system count drift: current access points are 89 while the existing contract locks 88.
- The broader AC-M09 backend target JUnit remains blocked by the shared Maven target issue already recorded in the task log.
