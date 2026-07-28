# Bug Regression Evidence

## Bug Summary

主端口 `int_main` 复验发现 eDHR 执行页仍调用 `/batch-record-cell-link/prefill` 并把未落库值注入本地草稿，违反“创建/打开执行记录时自动落库预填值”的正式语义。

## Expected Behavior

执行页只从执行详情已保存的 `detail.cellValues` / `cellValuesJson` hydrate 草稿状态；如果后端没有落库，前端不得调用 `/prefill` 兜底展示成功态。

## Reproduction

- RED: `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js` -> FAIL。

## Root Cause

执行页仍保留自动落库改造前的前端预填路径：加载 DRAFT 执行详情后调用 `BatchRecordCellLinkApi.getPrefill`，再把接口返回的 `prefills` 注入本地草稿。该路径会让页面显示未落库值，和后端创建/打开执行记录自动落库并由执行详情返回已保存值的新语义冲突。

## Regression Test

- `IntRuoyiFronted/tests/e2e/edhr-cell-link-auto-persist-static.spec.js`

## RED

- RED: `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js` -> FAIL，执行页仍保留旧 `/prefill` 草稿注入路径。

## GREEN

- GREEN: `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-pre-release-editable-submit-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-fill-workspace-worktask-permission-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-execution-real-flow.e2e.js` -> PASS，已在 `测试租户/codexedhrcell01` 通过真实前端批次详情“打开填写”路径断言执行 `1579` 的 `1:5` 单元格显示已落库批号 `EDHR-CELL-20260728-104808`。
- BLOCKED: `node tests/e2e/mes/batch-record-cell-link-static.spec.js` -> FAIL，当前失败点为并行新增的 `templateId?: number` API 合同断言，不属于本次执行页草稿预填回归。

## Verification

- Static regression confirms the execution page no longer imports or calls `BatchRecordCellLinkApi.getPrefill`.
- Adjacent eDHR contracts confirm pre-release editable submit and work task permission behavior remain covered.
- Type verification confirms removing the draft prefill state leaves no Vue/TypeScript compile errors.
- The broader batch-record cell-link static contract is not used as this task's GREEN gate because it is currently blocked by an unrelated form-template API assertion.
- Real Playwright E2E passed on `int_main` main runtime after authorized test-tenant fixture repair; `task/open` returned `cellLinkAutoPersist.status=NO_CHANGE_ALREADY_APPLIED`, and both execution detail and original-form page input showed `EDHR-CELL-20260728-104808`.

## Risk And Regression Scope

风险集中在 eDHR 执行页草稿 hydrate；修复不得改变已保存执行详情、字段审计、附件和只读追踪模式的既有读取链路。

## Blockers

- 本次回归和真实 E2E 无剩余 blocker；并行宽合同 `node tests/e2e/mes/batch-record-cell-link-static.spec.js` 仍阻塞在非本任务表单模板 API 断言。
