# Execution Log

## User Intent

- 用户确认：不要显示独立“历史批记录”；在“表单追溯”的详情里点击查看详细信息。
- 用户要求：详情不是纯文字，而是像批次执行填写页那样直观展示历史批记录内容。
- 用户反馈：没有看到“批记录表单”页签；确认之前页签加在“追溯”抽屉，而不是用户点击的“详情”弹窗。

## BDD

- `BDD: 隐藏独立历史批记录入口 -> Given 用户进入 eDHR 批次相关页面 When 页面渲染追溯信息 Then 页面不再展示独立“历史批记录”入口或页签`
- `BDD: 表单追溯打开可视化批记录详情 -> Given 表单追溯存在历史批记录项 When 用户点击详情 Then 打开只读可视化批记录表单，复用 eDHR 执行表单样式和快照数据，而不是纯文字`
- `BDD: 详情弹窗直接显示批记录表单页签 -> Given 表单追溯列表存在变更记录 When 用户点击该行“详情” Then “电子批记录变更详情”弹窗默认显示“批记录表单”页签，并能打开可视化只读批记录`
- `BDD: 历史快照只读展示 -> Given 历史执行已归档且当前 BATCH 配置可能缺失 When 打开追溯详情 Then 页面使用持久化快照展示，不触发活动批记录配置门禁`

## Milestone Updates

- completed: 已定位表单追溯页 `FormTracePage.vue`、追溯抽屉 `BatchExecutionTraceDrawer.vue`、旧历史页签入口和现有 `EdhrExecutionReadonlyForm`。
- completed: 已新增静态契约 `edhr-trace-visual-record-detail-static.spec.js`，并让旧实现先 RED。
- completed: 已从 eDHR 页签、路由、批次详情关联引用和页面关系图移除独立历史批记录入口。
- completed: 已在表单追溯抽屉新增默认“批记录表单”页签，使用 `review-timeline.executionReviews[].formViewModel` 和 `EdhrExecutionReadonlyForm` 做只读可视化展示。
- completed: 已在“电子批记录变更详情”弹窗新增默认“批记录表单”页签，并提供“查看批记录表单”动作打开同一只读可视化抽屉。
- completed: 已保留单元责任、操作审计、电子签名、放行事件页签，并补充附件证据技术校验。

## Verification Evidence

- `RED: node tests\e2e\edhr-trace-visual-record-detail-static.spec.js -> FAIL, expected reason: 旧代码仍展示“历史批记录”页签并跳转独立 edhr-batch-history 页面`
- `RED: node tests\e2e\edhr-trace-visual-record-detail-static.spec.js -> FAIL, expected reason: 用户点击“详情”的弹窗没有 el-tabs，也没有“批记录表单”页签`
- `GREEN: node tests\e2e\edhr-trace-visual-record-detail-static.spec.js -> PASS`
- `GREEN: node tests\e2e\edhr-batch-history-static.spec.js -> PASS`
- `GREEN: node tests\e2e\edhr-batch-history-evidence-layout-static.spec.js -> PASS`
- `GREEN: node tests\e2e\edhr-form-trace-batch-execution-trace-actions-static.spec.js -> PASS`
- `GREEN: node tests\e2e\edhr-form-trace-tabs-static.spec.js -> PASS`
- `GREEN: node tests\e2e\edhr-trace-drawer-four-tabs-standard-list-static.spec.js -> PASS`
- `GREEN: pnpm ts:check -> PASS`
- `GREEN: rg -n "历史批记录|历史同工序|edhr-batch-history|MesProEdhrBatchHistory" <visible eDHR entry files> -> no matches`

## Blockers

- 当前工作区已有大量无关修改，且分支 `int_main` 落后 `origin/int_main` 2 个提交；本任务只改动任务自有文件，提交/推送需后续单独处理，避免混入并发任务。
