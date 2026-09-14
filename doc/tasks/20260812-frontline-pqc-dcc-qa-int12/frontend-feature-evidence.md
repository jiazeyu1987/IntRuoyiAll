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
- GREEN: post-restart runtime/static submit contracts and `pnpm ts:check` -> PASS at 2026-08-14T16:43+08:00.
- GREEN: 2026-08-15 closeout recheck ran `pnpm install --frozen-lockfile` first and received exit 0, then `pnpm ts:check`, runtime static contract, and formal submit static contract all received exit 0.
- GREEN: after backend response-contract remediation, dependency state was reconfirmed and `pnpm ts:check`, runtime static contract, and formal submit static contract again exited 0 in the required order.

## Verification

- Page selection, cache key, stale response guard, submit eligibility, and receipt state use activeOrderId plus formal PQC task identity.
- No compatibility path was added for old workOrderId+routeId selection.
- The 2026-08-15 dependency refresh did not change the lockfile and the requested frontend static-contract sequence passed from `E:\IntRuoyi\IntRuoyiFronted`.
- The frontend-feature evidence validator and its self-test passed. The frontend portion of the precise forbidden scan found no legacy helper, legacy endpoint, process-level task compatibility field/read, `formBindings`, or `workOrderId + routeId` selection identity.
- The final precise response-contract scan again passed with 0 violations after backend remediation; the frontend process response remains strict and consumes task identity only through `pqcTaskOptions`.

## Blockers

- Real browser write-path E2E is still blocked by missing confirmed runtime, test tenant/account, permissions, and traceable task data.
