# Frontend Feature Evidence

## Feature Goal And Non-goals

- Goal: 批记录填写配置顶部红框位置显示当前表单名称和版本。
- Non-goals: 不改变填写配置保存 payload、不改变同产品同版本上一张/下一张候选、不恢复已隐藏的说明型红框标题。

## Requirements And Acceptance IDs

- AC-1: 打开填写配置弹窗时，顶部黄色导航条左侧显示当前表单名称和版本。
- AC-2: 标题来自当前报表上下文 `reportName + versionNo`，不得从同产品同版本导航标签、`formBindings` 或表格单元格文本推导。
- AC-3: 保留原表格、辅助表格、右侧映射控制栏、保存、重读、关闭和上一张/下一张能力。

## UI Entry Points And Owned Files

- Entry: `/mes/pro/batch-record-form-list`，点击预览区“填写配置”。
- Component: `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue`
- Test: `IntRuoyiFronted/tests/e2e/edhr-fill-config-current-form-title-static.spec.js`

## API Contracts And Data States

- Uses existing `BatchRecordReportVO.reportName`, `batchRecordName`, `reportId`, and `versionNo`.
- No backend contract changes.
- No write API should be called during read-only title verification.

## BDD Scenarios

- BDD: 当前表单名称版本展示 -> Given 用户打开辅助表单映射配置界面，When 页面顶部显示当前正在配置的表单上下文，Then 红框位置应显示当前表单名称和版本。

## RED Command And Expected Failure

- RED: `node tests/e2e/edhr-fill-config-current-form-title-static.spec.js` -> FAIL，当前组件缺少 `data-fill-config-current-form="name-version"`。

## GREEN Command And Passing Result

- GREEN: `node tests/e2e/edhr-fill-config-current-form-title-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-fill-config-redbox-hide-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-visual-fill-config-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: Node REPL Playwright real read-only check -> PASS，`芋道源码/admin`，`产品信息 / V1.0`，MES 写请求数 `0`。

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Responsive: 当前表单标题使用固定 class 和 `text-overflow: ellipsis`，窄屏下导航条改为单列布局。
- Accessibility: 当前表单标题提供 `title` 属性，导航按钮仍使用原有按钮文本。
- Loading/Error: 未改变 `loading`、`navigationLoading`、`navigationErrorMessage` 处理。
- Empty: 缺少版本时显示表单名；缺少名称时使用 reportId 或 `-`。
- Permission: 未新增权限判断或写入能力。

## E2E Or Component Verification Path

- Static contracts cover DOM/data-source/layout.
- Real read-only Playwright opened `/mes/pro/batch-record-form-list`, clicked “填写配置”, asserted current form title and no MES write requests.

## Blockers And Follow-up Skills

- No completion blocker.
- Playwright bundled Chromium was missing; verification used installed local Chrome executable for the same Playwright path.
