# Feature

## Feature Goal

- “历史批记录”不再作为独立用户入口显示。
- “表单追溯”详情提供可点击的历史批记录可视化只读详情。
- “表单追溯”列表行点击“详情”后，详情弹窗内直接出现“批记录表单”页签。

## Non-Goals

- 不删除历史批记录后端数据。
- 不新增 mock 数据或默认成功态。
- 不放宽批次执行填写权限。

## Acceptance

- A1: 独立“历史批记录”入口隐藏。
- A2: 表单追溯详情点击后打开可视化只读表单。
- A3: 可视化详情使用历史快照字段，不依赖当前活动 BATCH 配置。
- A4: “电子批记录变更详情”弹窗内必须有“批记录表单”页签，不只是在“追溯”抽屉里显示。

## UI Entry Points

- `/mes/pro/feedback/edhr-form-trace` 表单追溯页面。
- 表单追溯列表行的“追溯”按钮打开 `BatchExecutionTraceDrawer`。
- 表单追溯列表行的“详情”按钮打开 `FormTraceChangeTab.vue` 内的“电子批记录变更详情”弹窗，默认显示“批记录表单”页签。
- 批次执行页签、批次详情关联引用和批记录页面关系图不再显示独立“历史批记录”入口。

## API Contracts

- 继续使用 `getEdhrBatchReviewTimeline(batchExecutionId)`。
- 可视化表单使用 `executionReviews[].formViewModel.executionSnapshotJson`、`sheetLayoutJson`、`cellValuesJson` 和 `signatureRecords`。
- 不新增写接口，不调用活动 BATCH 配置推导，不新增 mock 或默认成功态。

## BDD

- `BDD: 隐藏独立历史批记录入口 -> Given 用户进入 eDHR 批次相关页面 When 页面渲染追溯信息 Then 页面不再展示独立“历史批记录”入口或页签`
- `BDD: 表单追溯打开可视化批记录详情 -> Given 表单追溯存在历史批记录项 When 用户点击详情 Then 打开只读可视化批记录表单，复用 eDHR 执行表单样式和快照数据，而不是纯文字`
- `BDD: 详情弹窗直接显示批记录表单页签 -> Given 表单追溯列表存在变更记录 When 用户点击该行“详情” Then “电子批记录变更详情”弹窗默认显示“批记录表单”页签，并能打开可视化只读批记录`
- `BDD: 历史快照只读展示 -> Given 历史执行已归档且当前 BATCH 配置可能缺失 When 打开追溯详情 Then 页面使用持久化快照展示，不触发活动批记录配置门禁`

## RED

- `RED: node tests\e2e\edhr-trace-visual-record-detail-static.spec.js -> FAIL, expected reason: 旧代码仍展示“历史批记录”页签和独立路由入口`
- `RED: node tests\e2e\edhr-trace-visual-record-detail-static.spec.js -> FAIL, expected reason: “电子批记录变更详情”弹窗没有“批记录表单”页签`

## GREEN

- `GREEN: node tests\e2e\edhr-trace-visual-record-detail-static.spec.js -> PASS`
- `GREEN: node tests\e2e\edhr-batch-history-static.spec.js -> PASS`
- `GREEN: node tests\e2e\edhr-batch-history-evidence-layout-static.spec.js -> PASS`
- `GREEN: node tests\e2e\edhr-form-trace-batch-execution-trace-actions-static.spec.js -> PASS`
- `GREEN: node tests\e2e\edhr-form-trace-tabs-static.spec.js -> PASS`
- `GREEN: node tests\e2e\edhr-trace-drawer-four-tabs-standard-list-static.spec.js -> PASS`
- `GREEN: pnpm ts:check -> PASS`

## Verification

- Responsive: 新增表单追溯只读表单布局在 960px 以下切为单列。
- Accessibility: 工序导航保留 `aria-label="表单追溯批记录工序"`，按钮可聚焦。
- Loading/Error: 复用原抽屉 loading 和 `loadError`，不吞接口错误。
- Empty: 缺少执行快照时显示明确空状态或只读表单解析错误。
- Permission: 未新增权限放宽；追溯抽屉仍只读，不包含保存、签名、放行、作废动作。

## Blockers

- 提交/推送未执行：当前共享工作区已有大量无关修改，且 `int_main` 落后 `origin/int_main` 2 个提交，直接基线提交/推送存在混入并发任务风险。
