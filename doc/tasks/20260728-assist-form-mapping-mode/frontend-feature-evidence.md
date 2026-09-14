# Frontend Feature Evidence

## Feature Goal and Non-goals

- Goal: 在现有“填写配置”弹窗中增加“辅助表单映射”模式，提供左侧原表单、中间辅助表单预览、右侧映射控制栏的直观配置体验。
- Non-goal: 不新增后端数据模型、不新增独立辅助草稿、不改变现有 `cell-rules` 与 `save-by-report` 保存合同。

## Requirements and Acceptance IDs

- R1: 默认仍进入“原表单配置”，用户可点击按钮切换到“辅助表单映射”。
- R2: 辅助映射模式必须展示 `source-form`、`assist-preview`、`mapping-control` 三个面板。
- R3: 辅助预览必须由现有 `assistRows`、`cellRules`、`fillAssignments` 实时计算，展示行描述、字段来源、字段类型和填写人摘要。
- R4: 保存仍复用现有接口，允许全部单元格切为不可填写后保存空规则集合。

## UI Entry Points, Routes, Components, and Owned Files

- Route: `/mes/pro/batch-record-form-list`。
- Entry: 批记录表单详情操作区的“填写配置”按钮。
- Component: `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue`。
- Tests: `IntRuoyiFronted/tests/e2e/edhr-visual-fill-config-static.spec.js`。

## API Contracts and Data States

- Read: `BatchRecordReportApi.getCellRules(reportId)` 读取 `assistRows`、`rules`、`suggestions` 与 `sheetLayoutJson`。
- Read: `EdhrProcessFormPermissionRuleApi.getByReport(reportId)` 读取辅助行填写人 `fillAssignments`。
- Save: `BatchRecordReportApi.saveCellRules(...)` 保存当前规则集合和规范化后的 `assistRows`。
- Save: `EdhrProcessFormPermissionRuleApi.saveByReport(...)` 按辅助行提交 `fillAssignments`。

## BDD Scenarios

- BDD: 辅助表单映射模式入口 -> Given 管理员打开批记录表单的“填写配置”弹窗 / When 点击“辅助表单映射” / Then 弹窗切换为原表单、辅助表单预览、映射控制栏三栏布局，原表单仍可点选单元格。
- BDD: 辅助表单实时预览 -> Given 管理员在辅助表单映射模式中新增辅助行并调整描述、字段类型、下拉选项或填写人 / When 控制栏状态变化 / Then 中间辅助表单预览实时更新行描述、字段标签、字段类型和填写人摘要。
- BDD: 保存合同不变 -> Given 管理员完成辅助行映射 / When 点击“保存填写配置” / Then 前端继续复用现有 `cell-rules` 与 `save-by-report` 保存 `assistRows`、`cellRules`、`fillAssignments`。

## RED Command and Expected Failure

- RED: `node tests\e2e\edhr-visual-fill-config-static.spec.js` -> FAIL, 预期失败于“辅助表单映射模式必须使用原表单、辅助预览、控制栏三栏布局”。

## GREEN Command and Passing Result

- GREEN: `node tests\e2e\edhr-visual-fill-config-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\batch-record-cell-rule-fillable-toggle-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\batch-record-cell-rule-dialog-size-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\batch-record-cell-rule-editor-mode-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-cell-rules-confirm-entry-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Responsive, Accessibility, Loading, Empty, Error, and Permission Checks

- Responsive: `@media (max-width: 1180px)` 将三栏收敛为单列，避免窄屏溢出。
- Accessibility: 模式切换使用 Element Plus radio group；原表单单元格按钮保留 `aria-pressed`。
- Loading/Error: 保留弹窗原有 `v-loading`、`errorMessage` 和 `sheetLayoutError`。
- Empty: 辅助预览在无辅助行时显示“暂无辅助行，请在右侧控制栏新增映射”。
- Permission: 不改菜单、路由或后端权限；只复用现有候选用户/角色和保存接口。

## E2E or Component Verification Path

- Static path completed: 聚焦静态合同和相邻合同均通过。
- Real smoke attempted: 本机 `8081/48081` 可监听，脚本使用本地前端默认登录配置且不输出密码；登录页点击后未发出登录请求，记录为本机登录自动化前置问题，未将该真实冒烟冒充通过。

## Blockers and Follow-up Skills

- No production blocker for the implemented frontend slice.
- Follow-up: 若需要完整写入型真实 E2E，应补齐任务自有 `CODX-VFC-*` 报表/路线/工单 fixture 的本地未跟踪配置，并通过正式页面执行清理闭环。
