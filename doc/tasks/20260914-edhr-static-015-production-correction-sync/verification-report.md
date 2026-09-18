# EDHR-STATIC-015 Verification Report

## Bug Summary

生产组长签名更正生产报工损耗数量后，原逻辑只更新事件 rawPayload、修订记录和管理摘要，未同步正式 `MES_PRO_FEEDBACK` 与物料事实，导致完工损耗条件和损耗 writer 继续读取旧损耗数量或旧 NO_LOSS 决策。

## Expected Behavior

已签核且明细完整的生产更正必须在同一事务内同步事件 payload、正式反馈、物料事实、完工损耗条件和损耗 writer 所消费的损耗事实；未签核、缺正式反馈来源、缺物料事实或损耗明细不完整时必须 fail fast，不得用旧值兜底。

## Reproduction

RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-015-production-correction-sync-static.spec.cjs` -> FAIL, expected reason: correction service lacked formal `MES_PRO_FEEDBACK` synchronization and material fact locking/update contract.

## Root Cause

`MesProcessPoolProductionReportCorrectionService.correct` 调用 revision 服务更新已签名事件后，没有锁定并更新 `MesProFeedbackDO`；有物料事实的报工也没有同步 `MesProFeedbackMaterialDO` 的损耗数量、损耗明细和设备 JSON。下游完工与损耗资料读取正式反馈表，因此会继续消费旧数量。

## Fix Summary

- `MesProcessPoolProductionReportCorrectionService` 在签名修订接受后同步正式反馈来源，只接受 `MES_PRO_FEEDBACK` 且要求唯一锁定目标反馈。
- `MesProFeedbackMapper.updateCorrectedProductionReport` 显式更新 corrected total、qualified、loss、scrap bucket 和 nullable loss reason snapshot。
- `MesProFeedbackMaterialMapper.updateCorrectedMaterialFact` 按 feedback id 锁定物料事实，并按 material id 更新 output/loss/lossDetails/device JSON；缺失或不完整时明确失败。
- `MesTeamLeaderActiveOrderReleaseLossSourceReaderImpl` 将 `hasActualLoss`、`zeroLossConfirmed`、`lossDecision` 纳入正式损耗来源，并与反馈损耗数量核对。

## Verification

GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-015-production-correction-sync-static.spec.cjs` -> PASS.

GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolProductionReportCorrectionServiceTest,MesTeamLeaderActiveOrderReleaseLossSourceReaderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS; Tests run: 14, Failures: 0, Errors: 0, Skipped: 0.

Non-E2E scope honored: no Playwright/E2E, database writes, service start/stop/restart, remote server operations, git commit, or git push were performed.

## Blockers

Closeout cannot be marked `completed` under project rules because `docs/task-closeout-rules.md` requires commit and push before completion, while this task scope explicitly prohibits git commit/push. Cleanup preview passed with keep=`task.md/execution-log.md/verification-report.md`, delete=[], blocked=[], warnings=[]; final task status is `blocked`.

## Risk And Regression Scope

The targeted verification covers signed zero-to-loss correction, formal feedback/material synchronization, stale NO_LOSS rejection, Mapper null-update contract, and loss source reader consumption. Full eDHR browser flow, live database persistence, and release/runtime validation remain outside the approved scope for this task.
