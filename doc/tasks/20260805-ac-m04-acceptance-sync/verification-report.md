# Verification Report

## Objective

核对并修复 `AC-M04 / 加入活跃订单池` 本机 P0 backfill 后的真实验收闭环，判断 AC-M04 是否可以从 `PASS_ACTION_NOT_ACCEPTED` 提升为 `ACCEPTED`。

## Final Decision

结论：`ACCEPTED / LOCAL_REAL_E2E_PASS`。

截至 2026-08-06 13:30 +08:00，当前本机 `result.json` 为 `status=PASS`、`mode=real`、`phaseEvidence=6`、`actionEvidence=22`、`gateEvidence=2`、`blockers=0`，coverage ledger 为 `total=62`、`accepted=62`、`pending=0`。AC-M04 的加入活跃订单、同一 `activeOrderId`、重复/并发/冲突路线、调拨追溯只读、跨角色只读、错误角色写入拒绝和最终清理闭环均已通过真实 E2E 与后端回归验证。

## Requirement Checklist

| 检查项 | 结论 | 证据 |
|---|---|---|
| Excel 原始需求 | PASS | `岗位需求分解矩阵.xlsx` 第 8 行要求生产班组长把候选生产订单加入活跃订单池，并验证该订单能出现在活跃订单列表、PQC 任务来源和报工分配候选中。 |
| 本机 P0 backfill | PASS | 用户授权范围仅限本机 Docker MySQL；88 行历史 P0 repair manifest、备份、rollback、apply、preflight/source/runtime migration verifier 均已 PASS。 |
| Runtime | PASS | `output/runtime/int_main/backend-runtime-acm04-formal-active-order-20260806-130259.jar` SHA256 `D3CB7E27188198816C2385095EA82950B7E1BE5DDDD8C186D02BB10E1C46C223`；`48081` PID `36924`；health `UP`。 |
| 当前真实 E2E 脚本 | PASS | `role-requirement-matrix-real-flow.e2e.js` 已要求 `RRM_TRANSFER_IDS`，覆盖冲突路线拒绝、同一 `activeOrderId`、调拨追溯、最终清理、QA 发布版本页面证据、AC-M19 gate 和 coverage capstone acceptance。 |
| 当前 on-disk `result.json` | PASS | `IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json` 为 `status=PASS`、`blockers=0`、`accepted=62/62`。 |
| QA 发布版本页面证据 | PASS | `qaRegulationPublishedVersionReadOnly` 已在真实 QA 页面观察发布版本、路线版本、工序、首检/巡检/末检、逐工序批记录绑定和发布不可变证据。 |
| AC-M19 / M6 门禁 | PASS | `m6ConcurrencyGateVerified=true`、`m6PerformanceGateVerified=true`，coverage 接受条件同时要求 M0-M5、M6、最终清理和 required phases 全部 PASS。 |
| 是否可提升为 `ACCEPTED` | YES | full real E2E 无 blocker，62 项 coverage 全部 ACCEPTED，AC-M04 不再停留在 action-only 状态。 |

## Commands Run

| 命令 | 结果 |
|---|---|
| `node IntRuoyiFronted\tests\e2e\role-requirement-matrix-preflight-static.spec.cjs` | PASS，`PASS role-requirement-matrix preflight static contract`。 |
| `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` | PASS，脚本语法有效。 |
| `node IntRuoyiFronted\tests\e2e\role-requirement-matrix-local-wrapper-static.spec.cjs` | PASS，`PASS role-requirement-matrix local wrapper static contract`。 |
| `doc\tasks\20260805-ac-m04-acceptance-sync\run-rrm-real-e2e-local.ps1 -Mode Real -BackendJar output\runtime\int_main\backend-runtime-acm04-formal-active-order-20260806-130259.jar` | PASS，`PASS role-requirement-matrix full real E2E`，`RRM_ACCOUNT_RESTORE=PASS`。 |
| `node` parsed `IntRuoyiFronted\test-results\role-requirement-matrix-real-flow\result.json` | PASS，`phaseEvidence=6`、`actionEvidence=22`、`gateEvidence=2`、`blockers=0`、coverage `accepted=62/62`。 |
| `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am -Pmes-ac-m04-active-order-targeted-tests "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest,MesActiveOrderTransferTraceServiceTest,MesActiveOrderTransferTraceSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | PASS，30 tests / 0 failures / 0 errors / 0 skipped，`BUILD SUCCESS`。 |
| `Get-FileHash output\runtime\int_main\backend-runtime-acm04-formal-active-order-20260806-130259.jar -Algorithm SHA256` | PASS，hash `D3CB7E27188198816C2385095EA82950B7E1BE5DDDD8C186D02BB10E1C46C223`。 |
| `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` | PASS，`UP`。 |
| `task-closeout-cleanup --mode preview` | PASS，补充 `Cleanup Keep` 后 `delete=<none>`、`blocked=<none>`、`warnings=<none>`。 |
| `task-closeout-cleanup --mode apply` | PASS，`deleted_paths=<none>`，审计证据已保留。 |
| `python -X utf8` 读取三份任务文档 | PASS，`task.md`、`execution-log.md`、`verification-report.md` 均 UTF-8 可读。 |

## Current AC-M04 State

- 已通过：生产班组长真实页面加入活跃订单池，提交 payload 包含 `workOrderId`、`routeId`、`routeVersionId`、`transferIds`。
- 已通过：冲突路线加入被后端 fail-fast 拒绝，重复加入和并发加入保持同一 active order 语义。
- 已通过：PQC 检验员跨角色只读同一 `activeOrderId=12`，调拨追溯只读返回 2 条正式 `TRANSFER` 来源。
- 已通过：最终清理将本轮活跃订单移出，`activeOrderCleanupCompleted=true`。
- 已通过：PQC 正式提交、组长复核/重复终态/自我复核、AC-M21 汇集只读、QA 发布版本页面证据、eDHR 放行准备和追溯只读均 PASS。
- 已通过：62 项角色需求矩阵 coverage ledger 全部 `ACCEPTED`，无 `E2E_COVERAGE` 残留 blocker。

## Remaining Items

代码、本机验证和 cleanup 层无剩余 blocker。项目 Git 集成层仍需单独处理：当前仓库存在大量非本任务脏改动，且 `int_main...origin/int_main [behind 5]`；本轮未回滚、未提交、未推送无关改动。
