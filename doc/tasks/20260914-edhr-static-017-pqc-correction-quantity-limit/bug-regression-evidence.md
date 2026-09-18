# Bug Regression Evidence

## Bug

EDHR-STATIC-017: PQC 更正入口允许损耗/报废数量大于实际检验数量，且允许损耗/报废数量小于逐件不合格明细所需数量。

## Expected

PQC 更正后的损耗/报废数量不得大于实际检验数量，也不得小于逐件不合格/报废明细要求；非法数量关系必须在签名、修订日志、事件 payload、PQC 记录和逐件明细写入前 fail fast 拒绝。

## Reproduction

- `node IntRuoyiFronted\scripts\pqc-correction-quantity-limit-static.spec.cjs` initially failed because前端更正弹窗与请求构建缺少数量上限合同。
- `mvn "-Dtest=MesProcessPoolPqcInspectionCorrectionServiceTest" test` from `IntRuoyiBackend\yudao-module-mes` initially failed because `actualInspectionQuantity=5, scrapQuantity=10` and `actualInspectionQuantity=5, scrapQuantity=1` with two failed samples were not rejected.

## Root Cause

`MesProcessPoolPqcInspectionCorrectionService.validateCommand` only checked `actualInspectionQuantity > 0` and `scrapQuantity >= 0`; it did not compare scrap quantity to actual inspection quantity. The service also rebuilt piece details but did not require scrap quantity to cover distinct failed sample numbers before signature/revision/formal table writes. The frontend correction form had `:min="0"` for scrap quantity and only checked integer non-negative values when building the request.

## Fix

- Backend rejects `scrapQuantity > actualInspectionQuantity` during command validation.
- Backend counts distinct failed `sampleNo` values in updated piece details and rejects `scrapQuantity < failedSampleCount` before signature, revision, task, record, or piece-detail writes.
- Frontend caps the scrap input with the current actual inspection quantity and repeats both checks in request construction.
- Frontend carries PQC result type and numeric bounds into correction rows to count failed boolean/numeric sample positions before submit.

## RED:

- `node IntRuoyiFronted\scripts\pqc-correction-quantity-limit-static.spec.cjs` -> FAIL, expected frontend static contract missing.
- `mvn "-Dtest=MesProcessPoolPqcInspectionCorrectionServiceTest" test` -> FAIL, expected `ServiceException` was not thrown for the two invalid quantity relationships.

## GREEN:

- `node IntRuoyiFronted\scripts\pqc-correction-quantity-limit-static.spec.cjs` -> PASS.
- `mvn "-Dtest=MesProcessPoolPqcInspectionCorrectionServiceTest" test` -> PASS, 7 tests.
- `node --check IntRuoyiFronted\scripts\pqc-correction-quantity-limit-static.spec.cjs` -> PASS.
- `git diff --check -- <task-owned paths>` -> PASS.
- Bug regression evidence validator -> PASS.

## Verification

Verification was limited to static/source and targeted non-E2E checks per user instruction. After follow-up authorization, the EDHR-STATIC-017 slice was locally fused into `int_main` and re-verified there. No Playwright/E2E, database writes, service operations, remote operations, or git push were performed.

## Int Main Fusion Verification

- `node IntRuoyiFronted\scripts\pqc-correction-quantity-limit-static.spec.cjs` from `E:\IntRuoyi` -> PASS.
- `mvn "-Dtest=MesProcessPoolPqcInspectionCorrectionServiceTest,MesProcessPoolProductionReportCorrectionServiceTest,MesTeamLeaderActiveOrderReleaseLossSourceReaderTest" test` from `E:\IntRuoyi\IntRuoyiBackend\yudao-module-mes` -> PASS, 22 tests including 8 PQC correction tests.
- `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1` from `E:\IntRuoyi` -> PASS.
- `task-closeout-cleanup` preview/apply from `E:\IntRuoyi` -> PASS, delete/blocker/warnings none.

## Blockers

- Project closeout cannot be marked completed because `docs/task-closeout-rules.md` requires git push before completion, while this turn did not explicitly authorize `git push`.
- Source worktree cleanup remains blocked by detached HEAD plus mixed non-EDHR-STATIC-017 dirty paths; the mainline fusion used a task allow-list instead of a whole-worktree merge.
- Supplemental existing SFC static check is blocked by missing `postcss` because frontend dependencies are not installed in this worktree.
