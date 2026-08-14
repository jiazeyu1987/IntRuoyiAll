# Issue 2：完成状态筛选命名

## Bug

- 排产工单的完成状态条件显示为“完成筛选”，无法明确表达字段是完成状态、完成时间还是筛选动作。

## Expected

- 快速筛选和多维筛选都显示“完成状态”，查询参数和选项语义保持不变。

## Reproduction

- 运行 `node tests\e2e\mes-schedule-order-completion-status-label-static.spec.js`；修复前在快速筛选标签断言处失败。

## Feature Goal

- 将排产工单页面含义不清的“完成筛选”统一改为“完成状态”。
- 保持 `completionFilter` 查询参数、选项值、筛选行为和接口契约不变。
- 不修改其它页面结构、样式、权限或数据逻辑。

## Acceptance

- ACC-2.1：快速筛选 `completionFilter` 标签为“完成状态”。
- ACC-2.2：多维筛选 `completionFilter` 标签为“完成状态”。
- ACC-2.3：`completionFilter` 参数和 `INCOMPLETE / ALL / COMPLETED` 选项保持不变。

## BDD

- BDD: 排产工单完成状态筛选名称清晰 -> Given 用户打开排产工单页面 / When 用户查看快速筛选或多维筛选中的 `completionFilter` 条件 / Then 两处均显示“完成状态”，并继续提交原有 `completionFilter` 参数与正式选项值。

## TDD Evidence

- RED: `node tests\e2e\mes-schedule-order-completion-status-label-static.spec.js` -> FAIL，快速筛选仍使用“完成筛选”，符合预期失败原因。
- GREEN: `node tests\e2e\mes-schedule-order-completion-status-label-static.spec.js` -> PASS。

## Root Cause

- `completionFilter` 在快速筛选和多维筛选定义中都使用了“完成筛选”作为字段名。该文案描述的是筛选动作，不清楚字段表达的是工单完成状态。

## Implementation

- 将两套筛选定义的可见标签统一改为“完成状态”。
- 保留 `completionFilter`、`INCOMPLETE / ALL / COMPLETED` 选项以及原有查询处理逻辑。
- 同步更新排产页面静态合同、统一列表合同和人工完成真实路径中的可见标签定位器。

## Verification

- `node tests\e2e\mes-schedule-order-completion-status-label-static.spec.js` -> PASS。
- `node tests\e2e\unified-list-template-static.spec.js` -> PASS。
- `node tests\e2e\mes-schedule-order-sync-tab-static.spec.js` -> PASS。
- 对 7 个本次修改或新增的 JavaScript 测试文件运行 `node --check` -> PASS。
- `git diff --check -- <本问题文件>` -> PASS；只有 Git 的 LF/CRLF 工作区提示，无空白错误。
- `rg -n "完成筛选" src\views\mes\pro\scheduleorder\index.vue tests\e2e` -> 页面和既有合同无旧标签残留；仅聚焦合同保留用于禁止旧文案回归的负向断言。
- UTF-8：文档和源码均通过 UTF-8 方式读取，中文内容正常。

## Regression Results

- `node tests\e2e\schedule-order-main-multi-filter-static.spec.js` -> FAIL 于页面不存在旧合同要求的 `scheduleOrderMultiFilter.setCondition(...)` 默认条件；字段标签与 `queryParamKey` 断言已通过，失败与本次文案修改无关。
- `node tests\e2e\mes-pro-schedule-order-manual-finish-static.spec.js` -> FAIL 于旧合同要求默认 `completionFilter: 'INCOMPLETE'`，与本次文案修改无关。
- `node tests\e2e\mes-pro-schedule-order-pool-static.spec.js` -> FAIL 于缺少 `src/views/mes/pro/route/RouteFlowConfigPanel.vue`，测试未进入本次标签断言。
- `node tests\e2e\mes-pro-schedule-order-toolbar-layout-static.spec.js` -> FAIL 于旧版 `UnifiedListTemplate` DOM 断言，与本次标签修改无关。
- `node tests\e2e\mes-schedule-order-tab-controls-toolbar-static.spec.js` -> FAIL 于同步工单工具栏旧合同要求“重置”动作，与本次标签修改无关。
- `pnpm ts:check` -> FAIL 于 `TeamLeaderWorkbenchPage.vue` 的 `openAbnormalDialog`、`resetAbnormalForm` 不存在；排产工单文件未报告类型错误。
- 人工完成真实 E2E 未运行：需要专用登录态和任务自有数据；本问题只改变可见文案，已同步真实路径定位器并完成语法检查。

## Owned Files

- `IntRuoyiFronted/src/views/mes/pro/scheduleorder/index.vue`
- `IntRuoyiFronted/tests/e2e/mes-schedule-order-completion-status-label-static.spec.js`
- 与旧文案直接相关的既有排产工单静态契约。
- `doc/tasks/20260807-smart-scheduling-issue-fixes/issue-2-completion-status-label.md`

## Design Checks

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；统一两套筛选定义及对应合同，保留正式查询参数。
- 是否存在临时补丁或绕过：否。

## Blockers

- 本问题自身无阻塞；聚焦合同已通过。
- 上述相邻大合同和全量类型检查存在与本问题无关的共享工作区阻塞，需由对应任务处理，未在本问题内扩大修改范围。

## Status

- completed
