# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 删除文件类别管理“培训规则”编辑页顶部的权限预检提示区。
- Non-goals: 不修改错误提示、列表查询、规则保存、API、权限判断、路由或培训任务只读页。

## Requirements And Acceptance IDs

- AC-1：目标编辑页不包含 `dcc-training-rule-permission-precheck`。
- AC-2：目标编辑页不显示“发布前权限预检”及 `dcc:controlled-file:training:mine` 说明。
- AC-3：`TrainingRulesReadonlyTab.vue` 继续保留原权限预检提示。

## UI Entry Points And Owned Files

- Entry: DCC 文件类别管理 -> 培训规则。
- Component: `src/views/dcc/controlled-file/categories/components/CategoryTrainingRulesTab.vue`。
- Test: `tests/e2e/dcc-training-ux-prechecks-static.spec.cjs`。

## API Contracts And Data States

- 本任务不修改 API、请求参数、响应模型、加载态、空态或错误态。
- 原 `errorMessage` 错误提示继续保留。

## BDD Scenarios

- Given 类别培训规则页正常渲染，When 用户查看列表，Then 权限预检提示不显示。
- Given 培训任务只读规则页渲染，When 用户查看映射，Then 原权限预检提示继续显示。

## RED

- Command: `node tests/e2e/dcc-training-ux-prechecks-static.spec.cjs`。
- Result: FAIL；编辑页仍渲染稳定权限预检 marker，符合预期 RED。

## GREEN

- Command and result: 待执行。

## UI State Checks

- Responsive: 删除整块提示，不新增布局约束。
- Accessibility: 删除冗余信息提示，不改变可操作控件。
- Loading/empty/error: 列表及错误提示逻辑不变。
- Permission: 不修改权限校验或权限数据源，仅删除目标编辑页说明。

## E2E Or Component Verification Path

- 聚焦静态合同区分编辑页与只读页；运行 TypeScript 检查覆盖 Vue 编译契约。

## Blockers And Follow-Up Skills

- 当前无阻塞；收尾前运行 evidence validator。
