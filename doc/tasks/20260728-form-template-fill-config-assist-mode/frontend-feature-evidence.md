# Frontend Feature Evidence

## Feature Goal and Non-Goals

- Goal: 表单模板页签下“填写配置”支持与批记录表单一致的“辅助表单映射”切换入口和 M×N 辅助格映射交互。
- Non-goal: 不把表单模板绑定到批记录表单数据链路，不新增后端 API，不改变模板列表三按钮领域边界。

## Requirements and Acceptance IDs

- `AC-1`: 填写配置弹窗显示“原表单配置 / 辅助表单映射”切换入口。
- `AC-2`: 辅助映射模式下可配置辅助表格行列、添加/删除/切换填写人。
- `AC-3`: 先点辅助格，再点原表单元格建立映射；已分配原表格灰化禁点，取消映射后释放。
- `AC-4`: 保存仍输出模板自身 `assistRows` 和 `fillAssignments`。

## UI Entry Points, Routes, Components, Owned Files

- Entry: `/mdm/form-center/template` 右侧模板操作区“填写配置”。
- Component: `src/views/form-center/template/components/FormTemplateFillConfigDialog.vue`。
- Tests: `tests/e2e/form-template-fill-config-assist-mode-static.spec.js`。

## API Contracts and Data States

- Reused data: `cellRules`, `signatureCellMarkers`, `assistRows`, `fillAssignments`, `sheetLayoutJson` from template `jimuSchemaJson`。
- No new API contract.
- Forbidden dependency checked: no `BatchRecordReportApi`, no `batchRecordReportId` in template auxiliary mapping contract.

## BDD Scenarios

BDD: 表单模板填写配置可切换辅助表单映射模式 -> Given 管理员在表单模板页签打开可编辑模板填写配置 When 点击“辅助表单映射” Then 进入辅助映射模式并展示辅助表格控制区。

BDD: 表单模板辅助格映射原表单元格 -> Given 辅助映射模式选中填写人和辅助格 When 点击未分配原表单元格 Then 建立映射并灰化该原表单元格。

BDD: 表单模板取消映射释放原表格 -> Given 原表单元格已映射 When 用户点击“取消映射” Then 该原表单元格恢复可分配。

## RED Command and Expected Failure

RED: `node tests\e2e\form-template-fill-config-assist-mode-static.spec.js` -> FAIL, missing `activeConfigMode`。

## GREEN Command and Passing Result

GREEN: `node tests\e2e\form-template-fill-config-assist-mode-static.spec.js` -> PASS。

## Responsive, Accessibility, Loading, Empty, Error, Permission Checks

- Responsive: 新增 `max-width: 1180px` 下辅助映射布局回退单列。
- Accessibility: 原表按钮在辅助映射模式使用动态 `aria-label`、`disabled` 和 `title`。
- Loading/Error: 保留现有 `v-loading`、`errorMessage`、`sheetLayoutError`。
- Empty: 无填写人时显示“请在右侧添加并选择填写人”。
- Permission/Readonly: 只读模板禁用行列、填写人、映射保存动作。

## E2E or Component Verification Path

- Static contract and TypeScript checks completed.

## Blockers and Follow-up Skills

- Blocker: none.
