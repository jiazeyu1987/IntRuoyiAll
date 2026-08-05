# Bug Regression Evidence

## Bug Summary And Expected Behavior

- Bug: QA 规程配置页仍显示截图黄色框内的“工艺路线来源”说明块，红框基础字段区排版和间距不统一，蓝框手动工艺路线选择框没有按上次正式绑定关系默认选中。
- Expected: 黄色说明块不渲染；基础字段区使用统一网格间距；选择 DCC 项目或手动绑定后，蓝框下拉按正式 `getRouteProductByItem` 返回的 `routeProduct.routeId` 默认选中。

## Reproduction Command Or Path

- Reproduction path: 打开 `/mes/pro/process-pool/qa-regulation`，选择 DCC 项目代码，观察适用范围卡片中的黄色说明块、基础字段布局和手动工艺路线默认值。
- RED: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，新增断言命中黄色说明块仍存在、基础字段区缺少统一网格、蓝框未从正式 `routeProduct.routeId` 回填默认绑定。

## Root Cause

- `QaRegulationPage.vue` 的适用范围区保留了说明型 `el-alert`，基础字段仍混用直排 form item 与 `el-row/el-col`，间距来源不统一。
- 产品已有正式路线绑定时，页面只把绑定用于路线范围解析，没有同步赋给 `manualQaRouteBinding.routeId`，导致蓝框下拉不能默认显示上一次绑定。

## Regression Test Added Or Updated

- Updated `IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` to assert the hidden yellow explanatory alert, dedicated basic-field grid, and formal route-product binding preselection.
- Existing adjacent contracts retained: `qa-regulation-manual-route-selectable-static.spec.cjs` and `qa-regulation-final-applicability-static.spec.cjs`.

## RED Command And Expected Failure

- RED: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，expected reason: 旧页面仍显示黄色说明块，基础字段未统一网格，手动工艺路线下拉未默认回填正式绑定。

## GREEN Command And Passing Result

- GREEN: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/qa-regulation-manual-route-selectable-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/qa-regulation-final-applicability-static.spec.cjs` -> PASS。
- REGRESSION: `pnpm ts:check` -> prior FAIL in unrelated `TeamLeaderWorkbenchPage.vue`; latest rerun PASS after the parallel type blocker was resolved.
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs doc/tasks/20260805-qa-regulation-publish-fix/task.md doc/tasks/20260805-qa-regulation-publish-fix/execution-log.md` -> PASS，仅有 Git CRLF 工作区提示。

## Verification

- The focused static contract covers the latest screenshot requirements: no yellow route-source explanatory alert, dedicated basic-field grid spacing, and manual route binding defaulting from the formal product-route relation.
- Adjacent QA contracts confirm the layout/state change does not break manual route selectability or final-inspection applicability; latest full `pnpm ts:check` passes.

## Risk And Regression Scope

- Scope is limited to `QaRegulationPage.vue` applicable-scope layout, formal route-product binding readback, and the static contract that guards those behaviors.
- No backend contract, save/publish payload, route binding save endpoint, QA rule rows, inspection item seed data, or publish precheck behavior changed.

## Blockers And Follow-Up Actions

- `node tests/e2e/unified-list-template-empty-tabs-system-static.spec.js` is blocked by unrelated system count drift: current access points are 91 while the existing contract locks 88.
- The broader AC-M09 backend target JUnit remains blocked by the shared Maven target issue already recorded in the task log.

## Follow-Up: Publish Verification Tab

- Bug summary and expected behavior: QA 顶部仍显示“发布检查”页签；用户要求顶部只显示总览、检验规则和检验项目，现有发布校验与保存发布代码保持不变。
- Reproduction command or path: 打开 `/mes/pro/process-pool/qa-regulation`，选择任一 DCC 项目代码后观察顶部 QA 页签。
- Root cause: `QaRegulationPage.vue` 仍直接声明 `<el-tab-pane label="发布检查" name="verification" />`。
- Regression tests: `IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` 明确禁止该页签声明；`IntRuoyiFronted/tests/e2e/qa-regulation-publish-tab-hidden-static.spec.cjs` 隔离断言三个正式页签并确认现有保存/发布实现仍保留。
- RED: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，预期命中旧“发布检查”页签声明。
- GREEN: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/qa-regulation-publish-tab-hidden-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/qa-regulation-manual-route-selectable-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/qa-regulation-final-applicability-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: 本机真实只读 Playwright -> PASS，`芋道源码/admin` 选择 `IDI` 后页签为 `["总览","检验规则","检验项目"]`，`writeRequests=[]`、`pageErrors=[]`。
- Risk and regression scope: 仅删除顶部页签声明；不修改后端、发布校验方法、保存/发布 API、正式路线范围或检验项目数据。
- Remaining blocker: 系统级标准列表合同因并行接入点计数 `91 != 88` 失败；完整 AC-M09 后端目标 JUnit 仍沿用任务已有阻塞。
- Git ownership blocker: 并行脏工作区基线提交 `f6ea8f545` 已包含本次实现且混入大量其它任务文件，当前分支 `ahead 1`；本任务不将其冒充独立提交或直接推送。
