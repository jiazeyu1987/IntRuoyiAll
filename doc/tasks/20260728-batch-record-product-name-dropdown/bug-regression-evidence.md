# Bug Regression Evidence

## Bug Summary

- 用户反馈产品名称快速筛选“没显示全”。截图显示快速过滤字段选择框与条件选择框被压缩，存在“产品名称”“包含”和较长产品名称被截断的风险。
- 用户明确指出点击“填写人”列应该显示“批记录表单填写人设置”小弹窗；当前存在 `fillAssignments` 时误打开全屏“填写配置”，导致不能直接更换填写人。

## Expected Behavior

- 字段选择框完整显示“产品名称”。
- 条件选择框完整显示“包含”。
- 产品名称输入框在常见桌面宽度下不被 flex 压缩，较长候选名称在下拉中可换行完整阅读。
- 点击“填写人”列始终打开 `批记录表单填写人设置` 小弹窗，用于更换填写人；全屏 `填写配置` 只通过右侧“填写配置”动作进入。
- 不通过 tooltip、mock 数据、默认成功或吞异常掩盖展示问题。

## Root Cause

- `TableQuickFilter` 的 `el-select` / `el-autocomplete` 只设置 `width`，作为 flex 子项时仍可收缩；autocomplete 下拉也沿用默认输入框宽度和单行候选展示，长名称会被截断。
- `openBatchRecordFormPermissionDialog` 在检测到 `rule.fillAssignments?.length` 后直接 `openCellRulesDialog(row)` 并 `return`，把填写人列入口误分流到了全屏填写配置。

## Reproduction

- Reproduction: 用户截图中快速过滤第一列显示为“产...”，条件显示为“包”，说明 flex 布局压缩后无法完整展示“产品名称”和“包含”。
- Reproduction command: `node tests/e2e/edhr-batch-record-form-list-product-filter-autocomplete-static.spec.js` -> FAIL，新增完整显示合同命中缺失的 popper/no-shrink 宽度约束。
- Reproduction: 点击截图中的“已配置 个人：贾泽宇...”填写人列会打开全屏“填写配置”，而不是可更换填写人的小弹窗。
- Reproduction command: `node tests/e2e/edhr-batch-record-form-list-static.spec.js` -> FAIL，新增合同命中 `openBatchRecordFormPermissionDialog` 中 `openCellRulesDialog(row)` 分流。

## Regression Test

- 更新 `IntRuoyiFronted/tests/e2e/edhr-batch-record-form-list-product-filter-autocomplete-static.spec.js`，新增固定宽度、不收缩、候选 popper 可换行完整显示的静态合同。
- 更新 `IntRuoyiFronted/tests/e2e/edhr-batch-record-form-list-static.spec.js`，锁定填写人列必须打开 `permissionDialogVisible` 小弹窗，且不得因 `fillAssignments` 分流到 `openCellRulesDialog(row)`。

## RED

- RED: `node tests/e2e/edhr-batch-record-form-list-product-filter-autocomplete-static.spec.js` -> FAIL，expected reason: `TableQuickFilter` 尚未设置 autocomplete 专用 popper 样式，字段/条件/输入宽度也未锁定不收缩。
- RED: `node tests/e2e/edhr-batch-record-form-list-static.spec.js` -> FAIL，expected reason: 填写人列处理函数仍在 `fillAssignments` 存在时调用 `openCellRulesDialog(row)`。

## GREEN

- GREEN: `node tests/e2e/edhr-batch-record-form-list-product-filter-autocomplete-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-record-form-list-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/batch-record-form-latest-version-switch-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/batch-record-force-unbind-delete-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Verification

- Verification: 静态合同确认 `TableQuickFilter` 使用 autocomplete 专用 popper、字段选择框 `120px` 不收缩、条件选择框 `92px` 不收缩、产品名称输入区 `clamp(280px, 32vw, 420px)` 不收缩，候选项支持 `white-space: normal` 与 `overflow-wrap: anywhere`。
- Verification: 静态合同确认 `openBatchRecordFormPermissionDialog` 设置 `permissionDialogVisible.value = true`，且函数内不再包含 `openCellRulesDialog(row)` 或 `fillAssignments?.length` 分流；右侧“填写配置”动作仍保留全屏配置入口。
- Verification: `pnpm ts:check` 通过，确认新增 `popperClass?: string` 类型定义和组件模板无 TypeScript 回归。

## Risk And Regression Scope

- 变更集中在通用 `TableQuickFilter` 展示层和类型定义，不改变查询参数、API 请求、选择后自动查询或手动查询按钮行为。
- 通用宽度变更会影响复用快速过滤组件的页面；通过不收缩和可换行候选解决压缩问题，窄屏时由外层查询表单换行承载。
- 填写人列点击行为变更仅影响 `openBatchRecordFormPermissionDialog` 分流；全屏填写配置仍通过右侧 `填写配置` 按钮进入，不删除单元格/辅助表单配置能力。

## Blockers

- 真实页面候选选择 E2E 仍受既有本地 `48081` 后端未加载新增 endpoint 阻塞；本次视觉修复已通过静态合同和类型检查验证。
