# Execution Log

## User Intent

用户指出当前候选行显示为“订单 980022 / 活跃池 35”，需要改成类似图 2 的订单编号、产品、数量展示。

## BDD

- BDD: 活跃订单候选显示业务信息 -> Given 生产组长打开活跃订单相关候选列表 When 候选行渲染 Then 行内显示订单编号、产品、数量三项业务信息，不再把生产订单 ID 或活跃池 ID 作为主展示文本。
- BDD: 选择身份字段保持不变 -> Given 用户选择一个活跃订单候选 When 提交新增或分配动作 Then 请求仍使用正式 `workOrderId` / `activeOrderId` 等身份字段，不因展示文案调整丢失提交身份。

## Evidence

- Task directory created: `doc/tasks/20260808-active-order-option-label/`
- Read rules: `docs/task-closeout-rules.md`, `docs/frontend-development.md`, `docs/powershell-encoding.md`
- Read rules for backend / verification: `docs/backend-development.md`, `docs/powershell-memory.md`
- Read skill: `frontend-feature-delivery`
- Read closeout experience skill: `project-experience-consolidation`; no new durable lesson was added because existing `docs/powershell-memory.md` already covers same-module Maven concurrency / stale target blockers.
- Read experience index: `docs/experience-index.md`

## RED / GREEN / Regression

- RED: `node tests/e2e/team-leader-active-order-option-label-static.spec.js` -> FAIL, active-order frontend type and visible option formatter did not expose/use formal `productName` / `quantity` fields.
- GREEN: `node tests/e2e/team-leader-active-order-option-label-static.spec.js` -> PASS, active-order option uses `workOrderCode`, product name/code and quantity, and formatter no longer reads `workOrderId` / `id` for visible text.
- GREEN: `node tests/e2e/mes-process-pool-team-leader-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `git diff --check` -> PASS; only CRLF conversion warnings were emitted.
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260808-active-order-option-label/frontend-feature-evidence.md` -> PASS.
- BLOCKED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before Surefire in `testCompile`; concurrent same-module Maven wrote/read stale `target\classes` while active-order progress fields / mapper methods were mid-update. Observed active Maven processes included PID 68152, 66348, 66136, 63468 after this task's Maven failed. Impact: backend JUnit did not produce a valid PASS/FAIL for the order-code/product/quantity assertions and must be rerun after `yudao-module-mes` Maven is idle.

## Current Status

blocked：前端静态合同、相邻合同、TypeScript 和 diff check 已通过；后端定向 JUnit 等待同模块 Maven 空闲后复跑。
