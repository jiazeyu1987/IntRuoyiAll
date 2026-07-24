# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: hide the extra right-rail “填写人 / 提交时间” metadata block from eDHR batch detail.
- Non-goal: do not redesign the rail, change backend contracts, change permissions, or alter the per-document form card actions.

## Requirements And Acceptance

- Acceptance: Given the eDHR batch detail right rail renders process form cards, When the current process has form tasks, Then the independent red-box metadata block is absent.
- Acceptance: Given each form task card renders, When the metadata block is hidden, Then card-level filler display and open action remain present.

## UI Entry Points And Owned Files

- UI entry point: eDHR batch execution detail page.
- Component: `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`。
- Tests: `IntRuoyiFronted/tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js`。

## API Contracts And Data States

- API contracts unchanged.
- Data states unchanged: task cards continue using existing selected process/task data and `resolveTaskCardFillersText` for per-card filler display.

## BDD Scenarios

- BDD: 隐藏右侧红框元信息 -> Given 用户打开 eDHR 批次执行详情页并查看右侧单据列表 / When 右侧栏渲染当前工序单据卡片 / Then 不渲染独立的“填写人 / 提交时间”元信息块，单据卡片自身信息和打开入口保持可见。

## RED

- RED: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` -> FAIL，现有实现仍包含 `edhr-batch-detail__primary-fill-meta`。

## GREEN

- GREEN: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-companion-forms-right-panel-static.spec.js` -> PASS。

## Responsive, Accessibility, Loading, Empty, Error, Permission Checks

- Responsive: no new layout surface added; removing the extra block reduces right-rail vertical content.
- Accessibility: removed the obsolete `aria-label="表单填写元信息"` region; form card buttons and labels remain unchanged.
- Loading and empty states: unchanged.
- Error and permission states: unchanged.

## E2E Or Component Verification Path

- Static contract verification covers the affected component structure without requiring data writes or environment login.
- Full `pnpm ts:check` is blocked by unrelated DCC type errors listed in the task verification report.

## Blockers And Follow-Up Skills

- No blocker for the requested eDHR fix.
- Existing DCC type-check failures remain outside this task scope.
