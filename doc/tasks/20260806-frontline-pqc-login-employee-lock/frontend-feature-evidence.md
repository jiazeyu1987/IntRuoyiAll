# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 一线 PQC 页面继续使用正式活跃订单池和订单路线工序，并将员工固定为当前登录账号本人。
- Non-goal: 不调整一线生产员工切换、PQC 组长工作台、QA 规程项目级检验布局。

## Requirements And Acceptance IDs

- AC-FE-1: PQC 顶部生产订单选择器保留活跃订单池来源。
- AC-FE-2: PQC 工序选择器保留所选活跃订单路线工序来源。
- AC-FE-3: PQC 员工卡片只读显示当前登录人，不打开员工 picker。
- AC-FE-4: PQC 初始化和切换工序后只选择当前登录人对应 PQC 人员。

## UI Entry Points, Routes, Components, And Owned Files

- Route: `/mes/pro/feedback/edhr-batch-pqc-fill`
- Component: `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`
- State helper: `IntRuoyiFronted/src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts`
- Static contract: `IntRuoyiFronted/tests/e2e/mes-frontline-pqc-login-employee-lock-static.spec.cjs`

## API Contracts And Data States

- Existing active order API remains `/mes/pro/feedback/frontline/device-account/pqc/active-orders`.
- Existing active order process API remains `/mes/pro/feedback/frontline/device-account/pqc/active-order/processes`.
- PQC personnel loading must be constrained to current login account by the backend response and frontend selection helper.

## BDD Scenarios

- BDD: PQC 员工锁定登录人 -> Given PQC 员工或 PQC 组长登录一线 PQC 填写页 When 页面加载、切换订单或切换工序 Then 员工卡只显示当前登录人且不可打开员工选择器。
- BDD: PQC 活跃订单与工序正式来源 -> Given 一线 PQC 打开填写页 When 选择生产订单 Then 订单候选来自活跃订单池，工序候选来自所选活跃订单对应工艺路线。

## RED Command And Expected Failure

- RED: `node tests/e2e/mes-frontline-pqc-login-employee-lock-static.spec.cjs`
- Expected failure before fix: `PQC 员工卡必须标记为登录员工只读卡。`

## GREEN Command And Passing Result

- GREEN: `node tests/e2e/mes-frontline-pqc-login-employee-lock-static.spec.cjs` -> PASS.
- `node tests/e2e/mes-frontline-pqc-active-order-switching-static.spec.js` -> PASS.
- `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs` -> PASS, output: `PASS: eDHR frontline fill tabs static contract`.

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Accessibility: PQC 员工卡使用 `disabled` 和 `aria-disabled="true"`，并增加 `data-pqc-login-employee-card` 标记。
- Error state: PQC 模式若候选员工不是当前登录人，前端抛出 `一线PQC员工已锁定为当前登录账号，不能切换。`，不继续调用切换接口。
- Permission/data state: PQC 模式忽略路由 `actualEmployeeId`，只从当前登录用户匹配 PQC 人员候选。

## E2E Or Component Verification Path

- Verification: Static contract first; real E2E requires existing local runtime, login accounts, and task-owned active order data.

## Blockers And Follow-Up Skills

- Existing dirty worktree limits clean commit/push completion until baseline or user coordination is resolved.
- Full real-path E2E was not run in this continuation; current evidence is static contract and source-level behavior verification.
