# Verification Report

## Current Result

READY_FOR_CLOSEOUT

## Completed Evidence

- C00：只接受批准 activeOrderId manifest；不再从 `UNIQUE_TASK_VERSION` 推算活跃订单 QA 快照。
- DF06：创建活跃订单锁定 DCC/QA 三快照并生成 FIRST、PATROL_AM、PATROL_PM、FINAL 四类任务；removed 重新激活保留历史锁定。
- 后端定向回归：C00 + DF06 组合 Maven 45 tests PASS，0 failures/errors/skips。
- VAL13 后端聚合：17 个冻结测试类存在，127 tests PASS，0 failures/errors/skips。
- 前端静态合同：6 个一线 PQC / QA / DCC 合同 PASS。
- TypeScript：`pnpm ts:check` PASS。
- Evidence validators：database、backend、frontend 三类 evidence validator PASS，self-test PASS。
- 合并前端口门禁：`scripts\preflight\branch-runtime-port-guard.ps1` PASS，frontend 8155 / backend 48155。
- 静态门禁：UTF-8、冲突标记、C00 禁止项、PQC resultType 禁止项、QA-DCC 生产源码禁止项、`git diff --check` 均 PASS。

## Pending Gates

- 独立验收记录。
- Git 提交与快进合并。
- 用户要求保留 worktree，因此不执行 worktree 删除。

## Verification Commands

- `mvn.cmd -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaPqcSchemaTest,MesTeamLeaderActiveOrderServiceTest,MesProcessPoolActiveOrderMapperTest,MesTeamLeaderActiveOrderErpPlannedStartTest,MesTeamLeaderActiveOrderManualSortTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，45 tests。
- `mvn.cmd -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaPqcSchemaTest,MesQaInspectionRegulationServiceTest,MesFrontlineActiveOrderControllerTest,MesFrontlinePqcContextServiceTest,MesFrontlineActiveOrderSnapshotResolverTest,MesRouteDccProjectBindingServiceTest,MesRouteDccProjectBindingControllerTest,MesFrontlineDccProjectResolverTest,MesTeamLeaderActiveOrderServiceTest,MesProcessPoolActiveOrderMapperTest,MesFrontlinePqcTaskOverlayTest,MesFrontlineProductionSubmitCandidateTest,MesFrontlinePqcEmployeeSwitchServiceTest,MesProFrontlineFeedbackSubmitServiceTest,MesFrontlinePqcSubmissionConcurrencyTest,MesProcessPoolPqcInspectionCorrectionServiceTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，127 tests。
- `node tests/e2e/mes-route-dcc-project-binding-static.spec.cjs` -> PASS。
- `node tests/e2e/qa-regulation-dcc-direct-contract-static.spec.cjs` -> PASS。
- `node tests/e2e/dcc-project-code-qa-status-column-static.spec.cjs` -> PASS。
- `node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs` -> PASS。
- `node tests/e2e/mes-frontline-pqc-qa-process-runtime-static.spec.cjs` -> PASS。
- `node tests/e2e/frontline-pqc-formal-submit-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS。
- `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS。
- `git diff --check` -> PASS，仅 CRLF 提示。

## Verification Exception

- 真实写入 Playwright 路径：用户在 2026-08-15 明确说“不用测试，继续推进”，因此未运行；记录为用户批准的验收例外，不写成 PASS。

## Final Status

ready_for_closeout
