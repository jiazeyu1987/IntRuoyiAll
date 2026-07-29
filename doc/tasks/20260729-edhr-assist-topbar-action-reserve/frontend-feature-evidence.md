# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: eDHR 填写辅助模式顶部栏左侧 3 个切换按钮占据 2/3 宽度，右侧 1/3 作为后续操作按钮预留区域。
- Non-goals: 不新增实际操作按钮；不改变任务/批次、工序、填写人切换行为；不改变 API、权限、保存、提交、FormCenter 或批记录数据契约。

## Requirements And Acceptance IDs

- `REQ-1`: 顶部 3 个切换按钮收敛到左侧 2/3。
- `REQ-2`: 顶部右侧 1/3 存在独立预留区域，当前为空但稳定占位。
- `REQ-3`: 既有 3 个切换按钮和切换弹窗行为保持。

## UI Entry Points, Routes, Components, And Owned Files

- Route: `/mes/pro/feedback/edhr-execution/form` 的填写辅助模式。
- Component: `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`.
- Test: `IntRuoyiFronted/tests/e2e/edhr-assist-topbar-action-reserve-static.spec.js`.

## API Contracts And Data States

- No API contract changes.
- No loading, empty, error, permission, save, submit, or task state changes.

## BDD Scenarios

- `BDD: assist topbar leaves action reserve -> Given/When/Then` recorded in `execution-log.md`.

## RED Command And Expected Failure

- Pending.

## GREEN Command And Passing Result

- Pending.

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Responsive: desktop/tablet keeps 2/3 + 1/3 split; narrow viewport stacks reserve below the switch cards to avoid unreadable controls.
- Accessibility: existing buttons remain semantic `button` elements with unchanged click handlers.
- Loading/empty/error/permission: no behavior changes; no fallback or hidden error paths introduced.

## E2E Or Component Verification Path

- Static contract for template/CSS structure.
- TypeScript check for compile safety.

## Blockers And Follow-Up Skills

- None.
