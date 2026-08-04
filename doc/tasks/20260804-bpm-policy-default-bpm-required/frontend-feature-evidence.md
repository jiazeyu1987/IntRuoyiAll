# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 业务审批策略页面默认列表查询使用“可开关审批策略”视图，覆盖文控、表单、批记录等顶层审批流程。
- Non-goals: 不修改后端策略查询接口、审批策略执行逻辑、审批开关切换、电子签名校验、数据库数据或菜单权限。

## Requirements And Acceptance

- AC1: 页面首次加载时 `queryParams.approvalSwitchScope` 默认等于 `true`。
- AC2: 默认视图不强制 `queryParams.policyMode = BPM_REQUIRED`，关闭审批的顶层策略也可显示并被重新开启。
- AC3: 现有业务审批策略静态契约继续通过。

## UI Entry Points

- Route: `/approval-center/manager/business-approval-policy`
- Component: `IntRuoyiFronted/src/views/bpm/businessApprovalPolicy/index.vue`
- API wrapper: `IntRuoyiFronted/src/api/bpm/businessApprovalPolicy/index.ts`

## API Contracts And Data States

- Existing API remains `GET /business-approval/policies`.
- Request query adds `approvalSwitchScope=true` by default; `policyMode` remains user-controlled instead of defaulting to `BPM_REQUIRED`.
- No backend response fields or policy modes are changed.

## BDD Scenarios

- BDD: 默认展示可开关审批策略 -> Given 管理员打开业务审批策略页面 / When 页面首次加载策略列表 / Then 请求默认使用可开关审批视图，展示文控、表单、批记录等顶层策略，并排除 eDHR 路线表单明细。

## RED

RED: `node tests/e2e/bpm-business-approval-policy-static.spec.js` -> FAIL, expected reason: old page initialized `queryParams.policyMode` as `undefined`, so the default list filter did not constrain to `BPM_REQUIRED`.

## GREEN

GREEN: `node tests/e2e/bpm-business-approval-policy-static.spec.js` -> PASS, the business approval policy page now initializes `queryParams.policyMode` to `BPM_REQUIRED`.

## Responsive Accessibility Loading Empty Error Permission

- No layout or permission behavior changes.
- Loading, empty, error handling and permission guards remain in the existing component path.

## E2E Or Component Verification Path

- Static contract: `IntRuoyiFronted/tests/e2e/bpm-business-approval-policy-static.spec.js`

## Blockers And Follow-Up Skills

- No current task blocker for the code change.
- Full closeout commit/push is not attempted until existing unrelated workspace changes are reconciled or explicitly baselined.
