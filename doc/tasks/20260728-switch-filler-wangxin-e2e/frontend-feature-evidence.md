# Frontend Feature Evidence

## Feature

## Goal

让 eDHR 辅助填写模式“切换填写人”弹窗使用执行详情快照渲染候选，并允许 wangxin 选择其他后端可打开候选。

## Non-Goals

- 不新增 mock 数据。
- 不改变后端授权规则。
- 不重设计弹窗 UI。

## UI Entry

- Page: `/mes/pro/feedback/edhr-execution/form`
- Component: `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`
- Dialog: `.edhr-fill-workspace__assist-switch-dialog`
- Filler menu: `[data-assist-switch-menu="filler"]`

## API Contract

- Execution detail includes `assistSwitchTasks?: EdhrBatchExecutionTaskRespVO[]`.
- Filler switching calls `openEdhrBatchTask({ batchExecutionId, taskId, workTaskId, assistUserId })`.
- Successful open response includes confirmed `assistUserId`, which is written to route query and the assist rows context key.

## BDD

- BDD: wangxin 可选择其他填写人 -> Given/When/Then recorded in `execution-log.md`.
- BDD: 表单随选择的填写人变化 -> Given/When/Then recorded in `execution-log.md`.
- BDD: 候选来自执行详情快照 -> Given/When/Then recorded in `execution-log.md`.

## Acceptance

- A1: 非 wangxin 的后端可打开候选不再被禁用。
- A2: 点击其他填写人后正式 `openTask` 带所选 `assistUserId`。
- A3: 路由和重开弹窗 active 状态跟随后端确认的 `assistUserId`。
- A4: 弹窗打开期间不调用全量批次详情。

## RED:

- `node IntRuoyiFronted\tests\e2e\edhr-switch-filler-selectability-static.spec.js` -> FAIL，旧可选态硬锁当前登录人。

## GREEN:

- `node IntRuoyiFronted\tests\e2e\edhr-switch-filler-selectability-static.spec.js` -> PASS。
- `pnpm exec eslint src/views/mes/pro/edhr/ExecutionPage.vue src/api/mes/pro/feedback/index.ts src/api/mes/pro/edhr/batchExecution.ts tests/e2e/edhr-switch-filler-selectability-static.spec.js --format stylish` -> PASS。
- `pnpm ts:check` -> FAIL，阻塞在无关既有文件 `BatchRecordCellRulesConfirmDialog.vue` 与 `BatchExecutionDetailPage.vue` 的缺失属性；当前 `ExecutionPage.vue` 无新增类型错误。
- `node doc\tasks\20260728-switch-filler-wangxin-e2e\e2e-artifacts\switch-filler-wangxin-real.e2e.cjs` -> PASS。

## Checks

- Permission: frontend no longer disables other users solely by current login user ID.
- Error: backend `openTask` errors remain visible in the current dialog.
- Loading: filler dialog no longer calls full batch execution detail.
- E2E: `芋道源码/wangxin` 真实路径 PASS；弹窗候选 `王歆/任丹`，任丹 enabled；点击后 URL 和 `task/open` 均携带 `assistUserId=910181`，顶部填写人显示 `任丹`，辅助填写行保持 `87` 行，重开弹窗高亮任丹。

## Verification

- Static contract, ESLint and real wangxin Playwright E2E passed; full `pnpm ts:check` is blocked by unrelated existing files; see `verification-report.md`.

## Blockers

- Full `pnpm ts:check` remains blocked by unrelated existing files: `BatchRecordCellRulesConfirmDialog.vue` and `BatchExecutionDetailPage.vue`.
