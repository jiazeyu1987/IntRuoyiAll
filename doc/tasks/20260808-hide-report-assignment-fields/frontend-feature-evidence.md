# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: Hide the screenshot red-box fields in the production leader `分配报工` dialog while preserving the allocation table and actions.
- Non-goal: Redesign the workbench, change active-order allocation rules, or alter unrelated production/PQC tabs.

## Requirements And Acceptance

- The allocation dialog must not display `分配说明`, `复核签名ID`, `签名员工ID`, `签名快照`, or `可先按 FIFO 自动分配，再根据现场情况手动调整。`.
- The allocation dialog must still display `FIFO 自动分配`, `新增分配行`, and the allocation table.
- Hidden fields must not remain required by the allocation-only submit path.

## UI Entry Points And Owned Files

- Entry point: production leader workbench row operation `分配`.
- Components/files: `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`, `IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts`.

## API Contracts And Data States

- `confirmTeamLeaderReportAllocation` keeps `eventId`, `leaderType`, `allocationMode`, and `allocations` required.
- Review signature fields are optional for allocation confirmation; review-mode submission still builds and validates the signature payload.

## BDD

- BDD: 分配弹窗隐藏内部字段 -> Given 生产组长打开待复核报工的“分配”弹窗 When 弹窗展示活跃订单分配区域 Then 页面不显示分配说明、复核签名 ID、签名员工 ID、签名快照和 FIFO 说明提示，仅保留分配表与分配动作。

## RED And GREEN

- RED: `node tests\e2e\team-leader-report-allocation-dialog-hide-static.spec.cjs` -> FAIL because old dialog rendered hidden-target fields.
- GREEN: `node tests\e2e\team-leader-report-allocation-dialog-hide-static.spec.cjs` -> PASS.

## Responsive Accessibility Loading Empty Error Permission

- Responsive/accessibility: No layout expansion was introduced beyond removing internal form rows.
- Loading/empty/error: Existing FIFO preview loading, allocation table empty text, and error handling remain.
- Permission: Existing row visibility and route permission checks remain unchanged.

## E2E Or Component Verification

- Focused static contract passed.
- Real E2E was not run because this task did not start local services or create write-type allocation test data.

## Blockers And Follow-Up

- `pnpm ts:check` remains blocked by an unrelated existing type error in `FrontlineFixedTemplatePanel.vue`.
