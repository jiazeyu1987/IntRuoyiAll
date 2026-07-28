# Frontend Feature Evidence

## Feature Goal

批记录表单字段明细只展示批记录表单自身关联项，避免右侧详情面板混入过程检验记录等其它路线表单。

## Non-Goals

- 不调整批记录表单编辑器布局。
- 不修改保存接口契约。
- 不新增 mock、默认成功或兼容降级分支。

## Entry Points And Owned Files

- UI entry: 工艺路线批记录配置画布右侧 `data-flow-panel="selected-field-detail"`。
- Component: `IntRuoyiFronted/src/views/mes/pro/route/RouteFlowGraphDesigner.vue`。
- Test: `IntRuoyiFronted/tests/e2e/mes-route-flow-batch-record-detail-slot-filter-static.spec.js`。

## API Contracts And Data States

前端仍使用 `ProRouteFlowFormBindingVO.formSlotType` / `ProRouteFlowBatchRecordVO.formSlotType` 区分 `MAIN`、`PROCESS_INSPECTION`、`LOSS_REPORT`、`PARAMETER_RECORD`。右侧显示层只接受显式槽位，不把缺失槽位默认降级为 `MAIN`。

## Acceptance

- 批记录表单字段值只合并显式 `MAIN` 槽位的动态绑定和历史报表。
- 批记录表单字段链接只打开显式 `MAIN` 槽位的目标。
- 工序节点红绿边框只根据显式 `MAIN` 绑定判断，不被过程检验等其它表单影响。

## BDD

BDD: 批记录表单字段明细仅显示自身表单 -> Given 用户在批记录配置画布中选中字段“批记录表单”, When 右侧详情面板展示字段关联的表单信息, Then 面板只显示“批记录表单”相关信息, And 不显示“过程检验记录”等其它路线表单。

## Verification

- RED: `node tests/e2e/mes-route-flow-batch-record-detail-slot-filter-static.spec.js` -> FAIL。
- GREEN: `node tests/e2e/mes-route-flow-batch-record-detail-slot-filter-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-flow-clickable-detail-values-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-flow-batch-record-panel-visible-static.spec.js` -> PASS。

## Blockers

工作区存在并发任务改动；既有 legacy 静态合同失败在非本任务保存载荷断言上，未纳入本次修复范围。
