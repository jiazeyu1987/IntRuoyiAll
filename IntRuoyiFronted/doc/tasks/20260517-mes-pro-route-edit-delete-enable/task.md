# Task: MES 工艺流程编辑/删除按钮可用

## Goal

让 `MES / 生产管理 / 工艺流程` 列表里 active 路线的 `编辑` 和 `删除` 按钮可点击可用，不再因为状态被前端禁用。

## Scope

- 只修改 `MES 工艺流程` 列表页及其必要测试。
- 保留现有权限码判断，不引入 fallback。
- 仅移除“active 路线按钮禁用”的前端限制。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260516-dcc-upload-approval-persistence-real-e2e/task.md`
- Status before this task: completed.
- Impact: no unfinished prior task blocks this frontend change.

## BDD

BDD: active route edit button is clickable -> Given the MES 工艺流程列表 contains an active route, When the operator focuses the `编辑` action, Then the button is enabled and opens the route form.

BDD: active route delete button is clickable -> Given the MES 工艺流程列表 contains an active route, When the operator focuses the `删除` action, Then the button is enabled and can open the delete confirmation flow.

## Milestones

- [x] M1: Add a targeted regression that fails on the current active-status button lock.
- [x] M2: Remove the active-status disabled guard from edit/delete actions in the route list page.
- [x] M3: Run the new regression and required verification commands to confirm the buttons are usable.
- [x] M4: Update the task log with RED/GREEN evidence and current status.

## Expected Verification

- `node tests/e2e/mes-pro-route-actions.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; npm run ts:check`
- `python -c "import requests; text = requests.get('http://localhost:8081/src/views/mes/pro/route/index.vue', timeout=10).text; print(':disabled=\"scope.row.status !== CommonStatusEnum.DISABLE\"' in text, '仅停用状态，才可以操作' in text)"`

## Current Status

Completed. The MES 工艺流程列表 no longer blocks `编辑` and `删除` on active routes, and the existing permission gates remain intact.

## Final Verification

- `node tests/e2e/mes-pro-route-actions.spec.js` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; npm run ts:check` -> PASS
- live dev-server source probe -> PASS, the served `src/views/mes/pro/route/index.vue` no longer contains the status-disabled guard or the obsolete tooltip copy

## Blockers

None.
