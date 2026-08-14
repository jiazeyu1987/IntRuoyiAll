# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: align the real `PQC填写` tab with `output/frontline-pqc-operator-1920.html` for layout, inspection entries, three-action choice controls, piece dialog, and footer actions.
- Non-goal: do not change backend APIs, DTOs, route permissions, template payload contracts, database state, employee/process data sources, or PQC formal submission behavior.

## Requirements And Acceptance

- Acceptance: Given the user opens the real `PQC填写` tab, When the page loads, Then it shows `生产订单 / 工序 / 员工 / 主页`, `检验内容`, and `填检验` using the target HTML structure.
- Acceptance: Given the inspection item is length or pressure, When the item is opened with a formal process context, Then the piece dialog shows default values, minus/input/plus controls, units, and a five-column grid.
- Acceptance: Given the inspection item is appearance or seal, When the user selects bulk pass, bulk fail, or manual selection, Then the current quantity range updates and progress shows `已填 x/n`.
- Acceptance: Given PQC formal payload fields are still unavailable, When the user submits, Then the existing fail-fast message remains visible rather than returning default success.

## UI Entry Points And Owned Files

- UI entry point: `PQC填写` tab / `/mes/pro/feedback/edhr-batch-pqc-fill`.
- Component: `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`.
- Tests: `IntRuoyiFronted/tests/e2e/edhr-frontline-pqc-html-alignment-static.spec.cjs`.
- Real E2E probe: `IntRuoyiFronted/tests/e2e/edhr-frontline-pqc-html-alignment-real.e2e.cjs`.
- Task docs: `doc/tasks/20260731-frontline-pqc-html-alignment/`.

## API Contracts And Data States

- API contracts unchanged.
- Data source unchanged: process and employee options still come from the existing frontline device account APIs.
- Data states unchanged: piece inspection values are local page state only; formal PQC submission continues to fail fast until a real payload contract exists.
- Error behavior unchanged: missing process context and missing formal PQC payload surface explicit errors.

## BDD Scenarios

- BDD: PQC 主页面对齐 -> Given 用户进入真实 `PQC填写` 页签 / When 页面加载 / Then 顶部、左右主面板和底部操作与目标 HTML 使用相同结构，且真实工序、员工和订单上下文保持原数据源。
- BDD: 数值逐件检验 -> Given 当前检验数量大于 0 / When 用户点击长度或压力 / Then 打开逐件数值弹框，按 5 列网格展示每件默认值、减号、手工输入、加号和单位。
- BDD: 判断项目批量与逐件选择 -> Given 当前检验项目为外观或密封 / When 用户选择全部合格、全部不良或逐件选择 / Then 当前数量范围内的逐件状态正确更新并回显完成数量。
- BDD: 上下文隔离 -> Given 用户切换工序、首检/巡检/末检或巡检次数 / When 填写不同项目 / Then 本地逐件状态按工序、检验类型、巡检次数和项目隔离。
- BDD: 重填与提交边界 -> Given 当前 PQC 上下文已有逐件值 / When 用户点击重填 / Then 只清除当前上下文；When 用户点击提交 / Then 继续暴露现有正式 PQC payload 缺失错误，不返回默认成功。

## RED

- RED: `node tests/e2e/edhr-frontline-pqc-html-alignment-static.spec.cjs` -> FAIL.
- Expected reason: current PQC template did not expose `data-pqc-inspection-entry="length"` and still used batch-level bindings like `pqcDraft.lengthCm`.

## GREEN

- GREEN: `node tests/e2e/edhr-frontline-pqc-html-alignment-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs` -> PASS.
- GREEN: `node src/views/mes/pro/feedback/frontline-template-render.spec.cjs` -> PASS.
- GREEN: `node src/views/mes/pro/feedback/frontline-template-switch.spec.cjs` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `node --check tests/e2e/edhr-frontline-pqc-html-alignment-real.e2e.cjs` -> PASS.

## Responsive, Accessibility, Loading, Empty, Error, Permission Checks

- Responsive: target layout keeps the 780px left inspection panel, flexible right panel, five-column 1920px piece grid, and two-column smaller-width fallback.
- Accessibility: top cards, piece dialog controls, quantity buttons, and piece controls include visible labels or aria labels.
- Loading and empty states: unchanged for process and employee loading; if no process is available, piece entry fails fast with a visible message.
- Error states: PQC submit still raises `PQC 详细检验内容尚未纳入正式模板字段，无法按正式 payload 提交。`.
- Permission states: route permission and API authorization are unchanged.

## E2E Or Component Verification Path

- Real browser login preflight: PASS on `http://127.0.0.1:8081` with local default `芋道源码/admin` identity, password redacted.
- Real browser main layout: PASS, screenshot at `IntRuoyiFronted/output/playwright/20260731-frontline-pqc-html-alignment/pqc-main-1920.png`.
- Real piece interaction: BLOCKED because the formal `frontline/device-account/processes` API returned no selectable process for the local default identity.

## Blockers And Follow-Up Skills

- Blocker: provide or authorize a real test process/employee fixture for `芋道源码/admin` to run the piece dialog interaction end-to-end.
- Follow-up: once the formal PQC payload contract exists, route PQC submit through frontend/backend API delivery instead of keeping the current fail-fast submission gate.
