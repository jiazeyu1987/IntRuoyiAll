# INT12 Frontend Feature Evidence

## Scope

Wire the frontline PQC page to the formal activeOrderId projection and formal submit response while removing the old workOrderId+routeId selection identity.

## Acceptance

- BDD: 人员状态无持久化 -> Given 任务A已选择人员并填写草稿, When 切换订单、QA工序、task或刷新, Then 清空实际人员和草稿，重新 switch 成功前禁止提交。
- BDD: 快速切换不串数据 -> Given 订单A请求慢于订单B, When 用户切换到B, Then A响应不得覆盖B的工序、任务、人员或草稿。
- RED: node tests/e2e/mes-frontline-pqc-qa-process-runtime-static.spec.cjs -> FAIL, old runtime switch identity still used workOrderId/routeId instead of activeOrderId and formal stale token.
- RED: node tests/e2e/frontline-pqc-formal-submit-static.spec.js -> FAIL, formal PQC submit payload did not prove activeOrderId/task/rule identity and receipt handling.
- GREEN: node tests/e2e/mes-frontline-pqc-qa-process-runtime-static.spec.cjs -> PASS, frontline PQC QA process runtime contract.
- GREEN: node tests/e2e/frontline-pqc-formal-submit-static.spec.js -> PASS, frontline PQC formal submit static contract.
- GREEN: pnpm ts:check -> PASS, exit 0 at 2026-08-14T12:04+08:00.

## Verification

- Page selection, cache key, stale response guard, submit eligibility, and receipt state use activeOrderId plus formal PQC task identity.
- No compatibility path was added for old workOrderId+routeId selection.

## Blockers

- Real browser write-path E2E is still blocked by missing confirmed runtime, test tenant/account, permissions, and traceable task data.
