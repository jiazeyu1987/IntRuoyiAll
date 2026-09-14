# Frontend Feature Evidence

## Feature Goal And Non-goals

- Goal: eDHR 填写页辅助网格卡片内输入提示词和后置单位字号从 7px 增大一倍到 14px。
- Non-goals: 不调整字段数据来源、保存接口、校验规则、填写权限、原表模式或其它页面布局。

## Requirements And Acceptance

- Requirement: 用户截图红框中的提示词与单位显示过小，需增大一倍。
- Acceptance: 辅助网格内输入提示词、选择/文本域提示词和单位样式均为 `font-size: 14px`，静态合同锁定该口径。

## UI Entry Points And Owned Files

- Entry point: eDHR 填写页的“填写辅助模式”网格卡片。
- Component: `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`。
- Static contract: `IntRuoyiFronted/tests/e2e/edhr-fill-workspace-card-density-static.spec.js`。
- Task docs: `doc/tasks/20260729-edhr-fill-prompt-unit-font-size/`。

## API Contracts And Data States

- No API contract changes.
- No data state changes.
- Verification-only type correction keeps preview-mode cell values and route task id aligned with existing TypeScript contracts.

## BDD Scenarios

- `BDD: 提示词与单位字号增大一倍 -> Given` eDHR 填写页字段使用输入框提示词和后置单位展示；`When` 页面渲染这些输入控件；`Then` 提示词与后置单位的 CSS 字号应为原基准字号的 2 倍，且不改变字段值、保存链路或单位内容。

## RED / GREEN Evidence

- RED: `node tests/e2e/edhr-fill-workspace-card-density-static.spec.js` -> FAIL because `.el-input__inner` still had `font-size: 7px`.
- GREEN: `node tests/e2e/edhr-fill-workspace-card-density-static.spec.js` -> PASS.

## Responsive, Accessibility, Loading, Empty, Error, Permission

- Responsive: preserves existing grid card sizing and control height; only target prompt/unit font size changes.
- Accessibility: larger prompt and unit text improves readability; no interactive semantics changed.
- Loading/empty/error/permission: no logic changes and no fallback paths added.

## Verification Path

- `node tests/e2e/edhr-fill-workspace-card-density-static.spec.js` -> PASS.
- `node tests/e2e/edhr-assist-fill-mode-static.spec.js` -> PASS.
- `node tests/e2e/edhr-assist-fill-mode-configured-grid-static.spec.js` -> PASS.
- `node tests/e2e/edhr-cell-rules-static.spec.js` -> PASS.
- `pnpm ts:check` -> PASS after same-file type correction.

## Blockers And Follow-up Skills

- No active blocker for implementation or verification.
