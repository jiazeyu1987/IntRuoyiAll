# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: QA 页签只能由 QA 权限角色可见，同时保留 QA 角色/admin 的正常入口。
- Non-goals: 不改 QA 规程业务数据、不改 PQC 普通页签功能、不新增 mock 权限。

## Requirements And Acceptance IDs

- AC-QA-ROLE-1: QA 页签可见性必须绑定 QA 权限角色/权限标识。
- AC-QA-ROLE-2: 非 QA 角色不能看到 QA 页签。
- AC-QA-ROLE-3: admin 必须具备 QA 选线/QA 页签权限。

## UI Entry Points, Routes, Components, And Owned Files

- Route: `IntRuoyiFronted/src/router/modules/remaining.ts`, `/mes/pro/process-pool/qa-regulation`.
- Component: `IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue`.
- Tests: `IntRuoyiFronted/tests/e2e/qa-regulation-role-permission-static.spec.cjs`, `mes-edhr-qa-menu-static.spec.js`, `role-matrix-qa-regulation-tab-static.spec.cjs`.

## API Contracts And Data States

- QA page visibility permission: `mes:qa-inspection-regulation:query`.
- Existing QA save/publish and manual route binding backend permissions remain `mes:qc-template:query/update`; QA role receives those permission menu nodes through the migration.

## BDD Scenarios

- BDD: QA 角色可见 QA 页签 -> Given 用户拥有 QA 权限角色 When 打开 PQC 组长/规程相关页面 Then 可以看到 QA 页签并进入 QA 内容。
- BDD: 非 QA 角色不可见 QA 页签 -> Given 用户没有 QA 权限角色 When 打开同一页面 Then QA 页签不可见且不能通过前端普通页签入口进入。
- BDD: admin 具备 QA 选线能力 -> Given 系统缺少独立 QA 选线角色 When admin 登录 Then admin 仍拥有 QA 选线/QA 页签所需权限。

## RED Command And Expected Failure

- RED: `node tests\e2e\qa-regulation-role-permission-static.spec.cjs` -> FAIL, expected because the QA role migration did not exist and the QA route still used the shared team-leader permission.
- RED: `node tests\e2e\qa-regulation-role-permission-static.spec.cjs` -> FAIL, expected because tenant 1 admin was not explicitly included outside tenant-package scanning.
- RED: `node tests\e2e\qa-regulation-role-permission-static.spec.cjs` -> FAIL, expected because tenant 1 admin was not explicitly included outside tenant-package scanning.

## GREEN Command And Passing Result

- GREEN: `node tests\e2e\qa-regulation-role-permission-static.spec.cjs` -> PASS.
- GREEN: `node tests\e2e\mes-edhr-qa-menu-static.spec.js` -> PASS.
- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `node tests\e2e\qa-regulation-role-permission-static.spec.cjs` -> PASS after tenant 1 inclusion.
- GREEN: `node tests\e2e\qa-regulation-role-permission-static.spec.cjs` -> PASS after tenant 1 inclusion.

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Permission: QA route now requires `mes:qa-inspection-regulation:query`; non-QA role-menu grants for QA menu are soft-disabled by SQL migration.
- Responsive: No layout change expected.
- Accessibility: Existing tab semantics should be preserved.
- Loading/empty/error: No data-loading contract change expected.

## E2E Or Component Verification Path

- Static route/menu/role verification through `qa-regulation-role-permission-static.spec.cjs`.

## Blockers And Follow-Up Skills

- No implementation blocker.
- Closeout blocker: shared worktree contains unrelated dirty changes; commit/push not performed.
