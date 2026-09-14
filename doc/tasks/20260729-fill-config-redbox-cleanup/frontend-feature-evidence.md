# Frontend Feature Evidence

## Feature Goal

删除填写配置弹窗截图红框内的冗余展示内容：顶部汇总栏中的表单名、规则数量、待确认数量、后端待确认数量，以及辅助表单映射模式的右上提示文案。

## Non-goals

- 不修改批记录单元格规则、辅助表格映射、责任主体、字段类型或保存接口契约。
- 不修改后端 API、权限规则、批记录表单正式绑定或表单槽位数据链路。

## Requirements

- AC1: 填写配置弹窗不再渲染顶部红框汇总栏。
- AC2: 辅助表单映射模式不再渲染右上提示文案 `辅助表单映射：先选辅助格，再点未分配原表格`。
- AC3: 原表单配置/辅助表单映射切换入口、原表单预览、右侧配置面板和保存填写配置入口仍保留。

## Acceptance

- AC1: `batch-record-cell-rules-editor__summary` 不再存在于批记录填写配置弹窗源码。
- AC2: `辅助表单映射：先选辅助格，再点未分配原表格` 不再存在于批记录填写配置弹窗源码。
- AC3: `batch-record-cell-rules-editor__mode-switch`、`data-fill-config-panel="source-form"`、`data-fill-config-panel="assist-preview"`、`batch-record-cell-rules-editor__side-panel` 和 `保存填写配置` 仍保留。

## UI Entry Points

- 页面入口：批记录表单列表的 `填写配置` 动作。
- 组件：`src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue`。
- 同步脚本：`tests/e2e/edhr-visual-fill-config-real-flow.e2e.js`。

## API Contracts And Data States

- 保留 `BatchRecordReportApi.getCellRules` 和 `BatchRecordReportApi.saveCellRules`。
- 保留 `EdhrProcessFormPermissionRuleApi.getByReport` 和 `EdhrProcessFormPermissionRuleApi.saveByReport`。
- 未新增 mock、fallback、默认成功或错误吞掉逻辑。

## BDD Scenarios

BDD: 删除红框标注内容 -> Given 用户打开填写配置页面 When 页面渲染顶部配置区和辅助表单映射区 Then 红框标注的顶部状态内容和辅助表单映射说明文案不再显示，其他配置入口仍保留。

## RED

RED: `node tests\e2e\batch-record-cell-rule-summary-hidden-static.spec.js` -> FAIL，旧实现仍包含 `batch-record-cell-rules-editor__summary`。

## GREEN

GREEN: `node tests\e2e\batch-record-cell-rule-summary-hidden-static.spec.js` -> PASS。
GREEN: `node tests\e2e\batch-record-cell-rule-editor-mode-static.spec.js` -> PASS。
GREEN: `node tests\e2e\batch-record-cell-rule-side-helper-hidden-static.spec.js` -> PASS。
GREEN: `node tests\e2e\batch-record-cell-rule-dialog-size-static.spec.js` -> PASS。
GREEN: `node tests\e2e\edhr-visual-fill-config-static.spec.js` -> PASS。
GREEN: `node tests\e2e\assist-grid-per-user-mapping-static.spec.js` -> PASS。
GREEN: `node tests\e2e\edhr-cell-rules-confirm-entry-static.spec.js` -> PASS。
GREEN: `pnpm ts:check` -> PASS。

## Responsive, Accessibility, Loading, Empty, Error, Permission Checks

- 响应式：保留 `@media (max-width: 1180px)` 工作区单列布局。
- Accessibility：保留单元格按钮 `aria-label` 和 `aria-pressed` 语义。
- Loading/Error：未修改 `v-loading`、错误提示或读取失败处理。
- Empty：保留无布局和未选择单元格空状态。
- Permission：未修改入口权限或保存权限链路。

## E2E Or Component Verification Path

- 静态合同覆盖截图红框删除与相邻编辑功能保留。
- 真实 E2E 脚本等待点已从已删除提示文案改为辅助表单预览面板。

## Blockers And Follow-up Skills

- 无阻塞。
