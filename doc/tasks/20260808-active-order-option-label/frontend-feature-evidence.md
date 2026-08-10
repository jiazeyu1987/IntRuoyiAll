# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 活跃订单下拉展示订单编号、产品、数量，避免用户看到内部生产订单 ID / 活跃池 ID。
- Non-goal: 不调整活跃订单新增/移除/分配提交身份、权限、路由或加载流程。

## Requirements And Acceptance IDs

- A1: 候选可见主信息必须包含订单编号。
- A2: 候选可见信息必须包含产品名称或产品编码。
- A3: 候选可见信息必须包含数量。
- A4: 提交身份仍保留正式 ID 字段。

## UI Entry Points, Routes, Components, And Owned Files

- Component: `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`
- Tests: `IntRuoyiFronted/tests/e2e/team-leader-active-order-option-label-static.spec.js`

## API Contracts And Data States

- Active order response now exposes `id`, `workOrderId`, `workOrderCode`, `productName`, `productCode`, `quantity`.
- Display reads formal `workOrderCode`, product name/code and quantity; request payload contracts still use `workOrderId` for add candidates and `activeOrderId` for manual allocation.

## BDD Scenarios

- BDD: 活跃订单候选显示业务信息 -> Given/When/Then recorded in `execution-log.md`.
- BDD: 选择身份字段保持不变 -> Given/When/Then recorded in `execution-log.md`.

## RED Command And Expected Failure

- RED: `node tests/e2e/team-leader-active-order-option-label-static.spec.js` -> FAIL, frontend active-order type / formatter did not expose formal product and quantity display fields.

## GREEN Command And Passing Result

- GREEN: `node tests/e2e/team-leader-active-order-option-label-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-process-pool-team-leader-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `git diff --check` -> PASS with CRLF conversion warnings only.

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Responsive: 候选使用两行短文本，不扩大成表格。
- Accessibility: 行内 label 使用明确中文字段名。
- Loading/empty/error: 不修改现有加载、空态、错误处理。
- Permission: 不修改权限判断。

## E2E Or Component Verification Path

- Static contract first; real E2E not required because this is a display-only UI text structure change and no local runtime/login/data path is started.

## Blockers And Follow-Up Skills

- Backend target JUnit is blocked until same-module Maven concurrency clears: `yudao-module-mes` testCompile read stale/intermediate classes and failed before Surefire.
