# INT12 Final Frontline PQC Integration

## Task Goal

Connect the already verified DF01-DF11 capabilities into the formal frontline PQC controller, page, employee-switch, equipment, submission, signature, correction, release-writer, idempotency, and production-event flow. DF10/DF11 contracts are authoritative.

## Milestones

- [x] M1: Inspect the integrated baseline and record executable BDD scenarios.
- [x] M2: Run the specified backend/frontend tests and capture a real RED for missing integration behavior.
- [x] M3: Implement the smallest formal integration without fallback or compatibility branches.
- [x] M4: Run GREEN, adjacent regression, TypeScript, static-contract, and validator gates.
- [ ] M5: Run real Playwright path with confirmed local test tenant/account/data and record read-only final verification.
- [ ] M6: Independent verification, supervisor review, fast-forward to int_main, and closeout.

## Expected Verification

- Backend focused Maven command from the supervisor dev plan.
- `node tests/e2e/mes-frontline-pqc-qa-process-runtime-static.spec.cjs`.
- `node tests/e2e/frontline-pqc-formal-submit-static.spec.js`.
- `pnpm ts:check`.
- Real Playwright flow: active-order selection, QA process/items, employee switch, equipment/result entry, signed submission, separate PATROL_AM/PATROL_PM state.
- Backend/frontend evidence validators and `git diff --check`.

## Current Status

ready_for_closeout

Implementation and local non-E2E verification are complete. Real write-path Playwright E2E remains blocked until a confirmed local runtime, test tenant/account, permissions, and traceable active-order/PQC task data are supplied; no mock/API-only substitute was used.

## Authority And Ownership

- Authority: `E:\IntRuoyi\doc\tasks\20260811-frontline-pqc-dcc-qa-agent-design\common-background.md`, `interface-contracts.md`, `agent-tasks\INT12-final-integration.md`, plus the supervisor PRD/dev-plan/test-plan in this worktree.
- Baseline: `int_main` commit `333029852`; INT12 branch contains DF10/DF11 round-4 PASS contracts and merge commit `cf23d6111`.
- Ownership: only controller/personnel-switch/submission/correction/release consumer/page/final integration tests and this task directory.
- Forbidden: upstream schema, route-DCC mapping, QA locked-version assembly, DF11 API contracts, product/route/MES-process QA inference, fallback/compat/default-success branches.

## Experience Gate

- `docs/experience-index.md` exists.
- Applicable gates: frontend picker immediate response and stale-request isolation; submit-only validation separated from draft calculation; PQC published-item/equipment snapshot authority; actual employee and signature authority; worktree compile-baseline blocker must be evidenced instead of bypassed.

## Design Constraint Check

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；只连接正式 DF01-DF11 服务合同并删除旧身份链路，不复制上游解析。
- 是否存在临时补丁或绕过：否；合并后发现的旧 `workOrderId + routeId` 产品推算入口已删除，没有通过补测试桩保留旧链路。
