# Frontend Feature Evidence

## Scope

- Feature goal: personal workbench users can hide visible tasks and restore hidden tasks from a separate hidden view.
- Non-goals: hiding does not complete, cancel, claim, or mutate the underlying business task.
- Entry point: `src/views/Profile/components/ProfileWorkbench.vue`.
- API wrapper: `src/api/system/profileWorkbenchTaskVisibility/index.ts`.

## UI And Data States

- Default tab: `待办任务`, showing tasks whose row key is not in the persisted hidden-key set.
- Hidden tab: `已隐藏 N`, showing current loaded tasks whose row key is hidden.
- Visible-row actions: `进入/处理` and `隐藏`.
- Hidden-row actions: `恢复`.
- Error behavior: hidden-key load, hide, restore, and badge refresh failures are surfaced through the existing page alert or Element Plus message; no default-success path is added.

## Acceptance

- Users can hide a visible personal workbench task after confirmation.
- Users can switch to the hidden-task view and restore a hidden task.
- API failures remain visible and do not create a fake success state.

## API Contract

- `getProfileWorkbenchHiddenTaskKeys()` calls `/system/profile-workbench-task-visibility/hidden-keys`.
- `hideProfileWorkbenchTask(data)` calls `/system/profile-workbench-task-visibility/hide`.
- `restoreProfileWorkbenchTask(taskKey)` calls `/system/profile-workbench-task-visibility/restore`.

## BDD

- BDD: Hide personal workbench task -> Given 用户在个人工作台看到任务，When 点击隐藏并确认，Then 任务从默认待办列表移入已隐藏列表。
- BDD: Restore hidden personal workbench task -> Given 用户打开已隐藏列表，When 点击恢复，Then 任务从已隐藏列表回到默认待办列表。
- BDD: Error visibility -> Given 隐藏或恢复接口失败，When 用户执行操作，Then 页面展示失败信息且不修改为成功态。

## RED / GREEN

- RED: `node tests/e2e/profile-workbench-task-hide-restore-static.spec.js` -> FAIL，原因：缺少持久化隐藏 API、隐藏视图和恢复操作契约。
- GREEN: `node tests/e2e/profile-workbench-task-hide-restore-static.spec.js` -> PASS。
- GREEN: `node -e "const fs=require('fs');const { parse }=require('./node_modules/.pnpm/@vue+compiler-sfc@3.5.13/node_modules/@vue/compiler-sfc');..."` -> PASS，`SFC parse ok`。
- GREEN: `pnpm exec eslint --ext .ts,.vue src/views/Profile/components/ProfileWorkbench.vue src/api/system/profileWorkbenchTaskVisibility/index.ts` -> PASS。

## Verification

- Static contract, SFC parse, and narrow ESLint checks passed for the affected frontend scope.

## Regression Notes

- `pnpm ts:check` was attempted twice and timed out after 124s and 304s with no diagnostics. It is recorded as a broad-project verification gap, not as task-specific PASS evidence.
- Real Playwright path was not run because the task-specific static contract and backend JUnit cover the implemented API/UI contract, and no local runtime/login path was started for this task.

## Blockers

- No blocker for the task-specific frontend contract.
