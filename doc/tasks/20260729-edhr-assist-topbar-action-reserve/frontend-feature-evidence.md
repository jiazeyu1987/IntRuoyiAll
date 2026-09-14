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
- Components: `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`; `IntRuoyiFronted/src/views/form-center/business-action/ActionFormPanel.vue` for required type-check unblock only.
- Test: `IntRuoyiFronted/tests/e2e/edhr-assist-topbar-action-reserve-static.spec.js`.

## API Contracts And Data States

- No API contract changes.
- No loading, empty, error, permission, save, submit, or task state changes.

## BDD Scenarios

- `BDD: assist topbar leaves action reserve -> Given` eDHR 填写页处于填写辅助模式且顶部栏展示任务/批次、工序、填写人 3 个切换按钮；`When` 页面渲染顶部栏；`Then` 3 个切换按钮位于左侧 2/3 宽度的上下文区域，右侧 1/3 保留为空白操作按钮区域，且 3 个切换按钮仍可点击打开各自切换弹窗。
- `BDD: embedded action form template keeps required metadata -> Given` FormCenter 业务动作面板使用嵌入模板快照渲染动态表单；`When` 前端执行类型检查；`Then` 嵌入模板对象满足正式 `FormTemplateListItemVO` 必填字段契约，且不放宽类型或替换数据源。

## RED Command And Expected Failure

- `RED: node tests/e2e/edhr-assist-topbar-action-reserve-static.spec.js -> FAIL, expected reason: 顶部 3 个切换按钮尚未包在左侧 2/3 上下文区域。`

## GREEN Command And Passing Result

- `GREEN: node tests/e2e/edhr-assist-topbar-action-reserve-static.spec.js -> PASS`
- `GREEN: pnpm ts:check -> PASS`

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Responsive: desktop/tablet keeps 2/3 + 1/3 split; narrow viewport stacks reserve below the switch cards to avoid unreadable controls.
- Accessibility: existing buttons remain semantic `button` elements with unchanged click handlers.
- Loading/empty/error/permission: no behavior changes; no fallback or hidden error paths introduced.

## E2E Or Component Verification Path

- Static contract for template/CSS structure.
- Adjacent regression: `node tests/e2e/edhr-assist-process-switch-dialog-grid-static.spec.js -> PASS`.
- TypeScript check: initial `pnpm ts:check -> FAIL` in `src/views/form-center/business-action/ActionFormPanel.vue(257,3)`; after adding required `updatedTime` metadata to the embedded template object, `pnpm ts:check -> PASS`.

## Blockers And Follow-Up Skills

- Blockers: none.
