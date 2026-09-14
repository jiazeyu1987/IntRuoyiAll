# Frontend Feature Evidence

## Feature Goal and Non-Goals

- Goal：将批记录填写配置的“辅助表单映射”改为按填写人切换的 M*N 辅助表格映射；已分配原表单元格灰化禁点，取消映射后释放。
- Non-goal：不新增后端接口、不改执行页保存字段模型、不引入角色级辅助格分配。

## Requirements and Acceptance IDs

- A1：右侧蓝色控制栏可以设置辅助表格行数、列数。
- A2：右侧蓝色控制栏可以增加、删除、切换填写人。
- A3：每个填写人有独立 M*N 辅助表格。
- A4：点击辅助格后点击原表单元格建立映射；原表内容是映射，不复制。
- A5：同一个原表单元格全局只能分配一次；已分配后灰化且不可点击。
- A6：取消辅助格映射后，原表单元格恢复可点击。

## UI Entry Points, Routes, Components, and Owned Files

- Entry：批记录表单列表中的“填写配置”弹窗。
- Component：`IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue`
- Tests：`IntRuoyiFronted/tests/e2e/assist-grid-per-user-mapping-static.spec.js`
- Updated contracts：`edhr-visual-fill-config-static.spec.js`、`batch-record-cell-rule-editor-mode-static.spec.js`

## API Contracts and Data States

- Reused API：`BatchRecordReportApi.saveCellRules` 保存 `assistRows`。
- Reused API：`EdhrProcessFormPermissionRuleApi.saveByReport` 保存 `fillAssignments`。
- Data contract：每个辅助格保存为一个 `assistRows` 记录，`rowKey = ASSIST_GRID_U{userId}_R{row}_C{column}`。
- Assignment contract：每个辅助格保存一个 `fillAssignment`，`candidateSourceType = USERS`，`candidateSourceIds = [userId]`。

## BDD Scenarios

- `BDD: 配置辅助表格尺寸 -> Given 管理员打开填写配置并切到辅助表单映射 When 设置行列数 Then 黄色辅助表单实时显示 M*N 表格`
- `BDD: 按填写人维护独立表格 -> Given 管理员添加 A、B 两个填写人 When 切换填写人 Then 每个人显示自己的 M*N 辅助表格`
- `BDD: 点击辅助格再点原表格建立映射 -> Given 管理员先点击某辅助格 When 点击未分配原表单元格 Then 该原表单元格映射到当前填写人的当前辅助格`
- `BDD: 原表单元格全局唯一分配 -> Given 原表单元格已映射 When 切换到其他填写人 Then 该原表单元格灰化禁点，不能再次分配`
- `BDD: 取消映射释放原表单元格 -> Given 辅助格已有映射 When 点击取消映射 Then 原表单元格恢复可点击`

## RED Command and Expected Failure

- `RED: node tests/e2e/assist-grid-per-user-mapping-static.spec.js -> FAIL, 当前组件缺少 assistGridRowCount、assistFillerUserIds、selectedAssistGridCellKey、灰化禁点和唯一分配索引`

## GREEN Command and Passing Result

- `GREEN: node tests/e2e/assist-grid-per-user-mapping-static.spec.js -> PASS`
- `GREEN: node tests/e2e/edhr-visual-fill-config-static.spec.js -> PASS`
- `GREEN: node tests/e2e/batch-record-cell-rule-editor-mode-static.spec.js -> PASS`
- `GREEN: node tests/e2e/batch-record-cell-rule-fillable-toggle-static.spec.js -> PASS`
- `GREEN: node tests/e2e/batch-record-cell-rule-dialog-size-static.spec.js -> PASS`
- `GREEN: node tests/e2e/batch-record-cell-rule-default-fullscreen-static.spec.js -> PASS`
- `GREEN: node tests/e2e/edhr-assist-fill-mode-static.spec.js -> PASS`
- `GREEN: pnpm ts:check -> PASS`

## Responsive, Accessibility, Loading, Empty, Error, and Permission Checks

- Responsive：保留原有三栏布局和 `@media (max-width: 1180px)` 单栏收敛。
- Accessibility：原表单元格按钮继续提供动态 `aria-label`；辅助模式中已分配原表按钮使用 `disabled` 阻止重复点击。
- Empty：未选择填写人时显示“请在右侧添加并选择填写人”。
- Error：保存前 fail-fast 校验旧版映射、重复原表单元格、未添加填写人和未覆盖可填写规则。
- Permission：未变更权限入口或后端接口权限。

## E2E or Component Verification Path

- 本轮使用静态合同覆盖配置页交互结构、保存契约和类型门禁。
- 未执行真实浏览器写入 E2E；本任务未新增后端契约，且当前工作区存在大量并行脏改动，避免对共享租户制造未隔离写入数据。

## Blockers and Follow-Up Skills

- Closeout blocker：工作区存在大量并行改动，提交/推送需要单独选择性暂存本任务文件。
- Follow-up：如需真实浏览器验证，建议使用任务自有报表夹具并在运行前确认本地前后端入口、租户和清理路径。
