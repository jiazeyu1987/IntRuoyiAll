# Frontend Feature Evidence

## Feature Goal

批记录表单“填写配置”弹窗顶部化操作按钮，并提供同产品同版本表单间的上一张/下一张切换。

## Non-Goals

- 不修改后端接口。
- 不改变 cell-rules、fillAssignments 或 assistRows 保存 payload。
- 不引入表单槽位、FormCenter 动态表单或 `formBindings` 作为批记录导航来源。

## Entry Points And Owned Files

- UI entry: `MES / eDHR批记录 / 批记录表单` 右侧预览区 `填写配置`。
- Component: `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue`。
- Parent page: `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue`。
- Static contracts: `IntRuoyiFronted/tests/e2e/batch-record-cell-rule-navigation-static.spec.js` plus adjacent existing static tests.

## API Contracts

- Read list: `BatchRecordReportApi.getGeneratedReportPage({ pageNo, pageSize: 200, productName, versionNo })` with optional client-side `batchRecordVersionId` exact filter.
- Read rules: `BatchRecordReportApi.getCellRules(reportId)`.
- Save rules: `BatchRecordReportApi.saveCellRules(...)` and `EdhrProcessFormPermissionRuleApi.saveByReport(...)`.

## BDD Scenarios

- `BDD: 顶部操作区 -> Given 用户打开批记录表单填写配置弹窗 / When 弹窗渲染 / Then 关闭、重新读取、保存填写配置位于顶部右侧操作区，弹窗不再使用全宽 footer。`
- `BDD: 同版本导航 -> Given 当前表单属于某一产品和版本 / When 用户点击上一张或下一张 / Then 弹窗切换到同一产品同一版本的相邻表单并重新读取该表单 cell-rules。`
- `BDD: 未保存变更保护 -> Given 当前填写配置有未保存修改 / When 用户点击上一张或下一张 / Then 页面先确认是否放弃未保存修改；取消时保持当前表单。`
- `BDD: 导航候选加载失败显式暴露 -> Given 同产品同版本候选列表接口失败或当前表单缺少产品/版本 / When 用户打开填写配置 / Then 导航按钮禁用并显示真实阻塞原因，不返回默认成功或 mock 候选。`

## Verification Log

- RED: pending.
- GREEN: pending.
