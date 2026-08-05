# Bug Regression Evidence

## Bug Summary And Expected Behavior

- Bug: QA 规程配置页顶部仍显示副标题、绿色正式接口提示，并在项目选择与页签、页签与表格之间留下红框标注的空白带，用户要求红框里的内容不显示。
- Expected: 顶部区域只保留 QA 标题、DRAFT 状态、必填 `DCC 项目代码` 选择框和加载失败重试区；副标题、绿色接口提示和红框空白带不渲染；选择项目后紧凑显示 Tab 和规程内容。

## Reproduction Command Or Path

- Reproduction path: 打开 `/mes/pro/process-pool/qa-regulation`，观察顶部副标题、绿色提示和两个红框空白带仍可见。
- RED: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，新增断言命中 compact wrapper / subtitle / API-ready banner 要求。

## Root Cause

- `QaRegulationPage.vue` 顶部区域保留了上轮接口说明和绿色成功提示，Element Plus 空 tab content 与 `ContentWrap` 默认底部间距叠加，形成截图红框中的说明区和空白带。
- 前一次顶部合并修复保证了 DCC 选择框位置，但没有按新截图反馈隐藏说明和空白区域。

## Regression Test Added Or Updated

- Updated `IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` to assert the compact project/tabs wrappers, removal of subtitle/API banner, and hidden empty Element Plus tab content.
- Existing adjacent contracts retained: `qa-regulation-manual-route-selectable-static.spec.cjs` and `qa-regulation-final-applicability-static.spec.cjs`.

## RED Command And Expected Failure

- RED: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，expected reason: 旧页面仍显示副标题、绿色接口提示和红框空白带。

## GREEN Command And Passing Result

- GREEN: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/qa-regulation-manual-route-selectable-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/qa-regulation-final-applicability-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs doc/tasks/20260805-qa-regulation-publish-fix/task.md doc/tasks/20260805-qa-regulation-publish-fix/execution-log.md` -> PASS，仅有 Git CRLF 工作区提示。

## Verification

- The focused static contract covers the red-box hidden-content requirement: no subtitle, no green API-ready banner, compact project/tabs wrappers, and no empty tabs content.
- Adjacent QA contracts and `pnpm ts:check` confirm the layout change does not break manual route binding, final-inspection applicability, or Vue/TypeScript types.

## Risk And Regression Scope

- Scope is limited to `QaRegulationPage.vue` top layout, DCC project loading/error/retry placement, and formal manual-route candidate API selection.
- No backend contract, save/publish payload, route binding save endpoint, QA rule rows, inspection item seed data, or publish precheck behavior changed.

## Blockers And Follow-Up Actions

- `node tests/e2e/unified-list-template-empty-tabs-system-static.spec.js` is blocked by unrelated system count drift: current access points are 89 while the existing contract locks 88.
- The broader AC-M09 backend target JUnit remains blocked by the shared Maven target issue already recorded in the task log.
