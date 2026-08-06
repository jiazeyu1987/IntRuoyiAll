# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: PQC 点击“工序”后的弹框尺寸、布局和选项卡片尺寸与一线生产工序弹框一致。
- Non-goal: 不改接口、后端、活跃订单池、工艺路线工序来源、员工锁定逻辑或一线生产业务逻辑。

## Requirements And Acceptance IDs

- AC-FE-1: PQC 工序弹框使用与一线生产相同的弹框卡片宽度、内边距和圆角。
- AC-FE-2: PQC 工序选项区域使用与一线生产相同的 6 列网格、间距和滚动策略。
- AC-FE-3: PQC 工序子卡片使用与一线生产相同的高度、边框、字体和选中态。
- AC-FE-4: PQC 订单弹框和员工锁定逻辑不被本次样式调整破坏。

## UI Entry Points, Routes, Components, And Owned Files

- Route: `/mes/pro/feedback/edhr-batch-pqc-fill`
- Component: `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`
- Static contract: `IntRuoyiFronted/tests/e2e/mes-frontline-pqc-process-picker-production-layout-static.spec.cjs`
- Adjacent contract compatibility: `IntRuoyiFronted/tests/e2e/mes-frontline-pqc-order-picker-production-layout-static.spec.cjs`

## API Contracts And Data States

- No API contract changes.
- PQC process candidates continue to use existing active-order process state and handler.

## BDD Scenarios

- BDD: PQC 工序弹框复用生产布局 -> Given 一线 PQC 页面打开 When 点击顶部“工序” Then 工序弹框容器和子卡片布局与一线生产工序弹框一致。

## RED Command And Expected Failure

- RED: `node tests/e2e/mes-frontline-pqc-process-picker-production-layout-static.spec.cjs` -> FAIL, expected reason: `PQC process picker template must include: data-pqc-process-picker`.

## GREEN Command And Passing Result

- GREEN: `node tests/e2e/mes-frontline-pqc-process-picker-production-layout-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/mes-frontline-pqc-order-picker-production-layout-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/mes-frontline-pqc-login-employee-lock-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/mes-frontline-pqc-active-order-switching-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/edhr-frontline-production-pixel-parity-static.spec.cjs` -> PASS.
- GREEN: `git diff --check` -> PASS with CRLF warnings only.

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Layout: PQC 工序弹框卡片使用 `width: min(96%, 1770px)`、`aspect-ratio: 1920 / 1080`、`grid-template-rows: auto minmax(0, 1fr) auto`、`padding: 32px`。
- Child cards: PQC 工序选项使用 6 列网格、12px gap、16:9 子卡片、30px 字号和居中文本，与一线生产工序弹框一致。
- Accessibility: 未改变原有 picker 关闭、选中态和按钮语义；PQC 员工锁定合同仍通过。
- Data/permission: 未修改 API、候选项来源或选择流程；活跃订单和工序来源合同仍通过。

## E2E Or Component Verification Path

- Verification: 已完成任务专用静态合同和相邻静态回归；真实浏览器截图未在本轮执行。

## Blockers And Follow-Up Skills

- Current task has no functional blocker after static verification.
- Closeout blocker: 当前工作区仍有大量既有脏改动；提交/推送需先按项目规则处理基线与选择性暂存。
