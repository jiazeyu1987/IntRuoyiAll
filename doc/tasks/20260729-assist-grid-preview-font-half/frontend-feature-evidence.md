# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: eDHR 填写辅助模式辅助网格每个单元格内文字字号减小为当前的 1/2。
- Non-goals: 不修改字段解析、字段值保存、提交、权限、工序切换、填写人切换、`assistRows`、`ASSIST_GRID_U` rowKey 或后端接口。

## Requirements And Acceptance

- R1: 辅助网格单元格继承字号为 `50%`。
- R2: 字段名从 `15px` 缩小为 `7.5px`。
- R3: 输入文字和单位文字约从 `14px` 缩小为 `7px`。
- R4: 校验提示从 `12px` 缩小为 `6px`。
- R5: 目标静态合同、相邻回归和 `pnpm ts:check` 通过。

## UI Entry Points Routes Components Owned Files

- Entry: eDHR 执行填写页辅助模式。
- Component: `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`
- Static contract: `IntRuoyiFronted/tests/e2e/edhr-fill-workspace-card-density-static.spec.js`

## API Contracts And Data States

- No API contract changes.
- No state model changes.
- Existing draft values, validation messages, save and submit paths remain unchanged.

## BDD Scenarios

- BDD: 辅助网格单元格字号减半 -> Given 用户打开 eDHR 填写辅助模式并查看辅助网格单元格 / When 单元格展示字段名、输入内容、单位或校验提示 / Then 单元格内文字字号均缩小为当前的 1/2，字段值、校验、保存和提交逻辑不变。

## RED

- RED: `node tests/e2e/edhr-fill-workspace-card-density-static.spec.js` -> FAIL，当前样式缺少辅助网格字段名/输入/单位/校验字号减半规则。

## GREEN

- GREEN: `node tests/e2e/edhr-fill-workspace-card-density-static.spec.js` -> PASS
- GREEN: `node tests/e2e/edhr-fill-workspace-hide-side-panels-static.spec.js` -> PASS
- GREEN: `node tests/e2e/edhr-assist-fill-mode-static.spec.js` -> PASS
- GREEN: `node tests/e2e/edhr-fill-workspace-static.spec.js` -> PASS
- GREEN: `node tests/e2e/edhr-assist-fill-mode-configured-grid-static.spec.js` -> PASS
- GREEN: `pnpm ts:check` -> PASS

## Responsive Accessibility Loading Empty Error Permission

- Responsive: 使用现有辅助网格布局，仅缩小单元格内文字字号。
- Accessibility: 字段名称、输入值、单位和真实校验错误保持可见。
- Loading/empty/error: 未修改加载态、空态或错误链路。
- Permission: 未修改保存、提交、切换或只读权限。

## E2E Or Component Verification

- 本轮使用静态合同验证目标样式和相邻辅助模式回归；未启动本地服务或运行真实 Playwright。

## Blockers And Follow-Up Skills

- None for this task.
