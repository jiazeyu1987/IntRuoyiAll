# Bug Regression Evidence: EDHR-STATIC-026

## Bug Summary And Expected Behavior

- Bug: `CONDITIONAL_REQUIRED` 损耗表在 DHR 完整性检查中只因 `requiredFlag=true` 被要求完成，未基于正式完工来源的 `hasActualLoss` 判断适用性。
- Expected: 当正式批次来源确认 `hasActualLoss=false` 时，条件损耗表不适用且不阻断最终放行；当 `hasActualLoss=true` 时，条件损耗表仍必须有 APPROVED 填写证据；无条件 `REQUIRED` 表单不受放松。

## Reproduction

- RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProductionReleaseBusinessReadinessServiceTest#skipsNoLossConditionalLossFormWhenFormalCompletionOriginHasNoActualLoss+keepsConditionalLossFormRequiredWhenFormalCompletionOriginHasActualLoss+keepsUnconditionalLossFormRequiredEvenWhenFormalCompletionOriginHasNoActualLoss" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`MesProductionReleaseBusinessReadinessService` 构造函数尚未接入 `MesProEdhrBatchExecutionOriginMapper`，新增回归测试无法编译，证明生产代码没有读取正式来源损耗事实。

## Root Cause

- `MesProductionReleaseBusinessReadinessService#ordinaryProcessFillEvidenceComplete` 原逻辑只筛选 `NODE_TYPE_ROUTE_FORM + requiredFlag=true` 并排除 `SKIPPABLE_CONTROLLED`，随后要求全部任务 APPROVED 且有填写签名证据。
- `CONDITIONAL_REQUIRED` 与 `REQUIRED` 在 DHR 阶段被等价处理，`requiredConditionJson` 和批次来源中的 `hasActualLoss` 没有参与适用性判断。

## Regression Test

- Added/updated: `MesProductionReleaseBusinessReadinessServiceTest#skipsNoLossConditionalLossFormWhenFormalCompletionOriginHasNoActualLoss`
- Added/updated: `MesProductionReleaseBusinessReadinessServiceTest#keepsConditionalLossFormRequiredWhenFormalCompletionOriginHasActualLoss`
- Added/updated: `MesProductionReleaseBusinessReadinessServiceTest#keepsUnconditionalLossFormRequiredEvenWhenFormalCompletionOriginHasNoActualLoss`

## Fix

- `MesProductionReleaseBusinessReadinessService` 注入 `MesProEdhrBatchExecutionOriginMapper`。
- DHR 普通工序完整性检查只对识别为 `CONDITIONAL_REQUIRED`、`LOSS_REPORT`、`{"type":"HAS_ACTUAL_LOSS"}` 的任务执行损耗适用性求值。
- 仅当正式来源存在明确 `hasActualLoss=false` 且不存在 `hasActualLoss=true` 时跳过该条件损耗表；来源缺失、条件未知、真实损耗存在时继续阻断，不引入默认成功或可选化 fallback。

## Verification / GREEN Evidence

- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProductionReleaseBusinessReadinessServiceTest#skipsNoLossConditionalLossFormWhenFormalCompletionOriginHasNoActualLoss+keepsConditionalLossFormRequiredWhenFormalCompletionOriginHasActualLoss+keepsUnconditionalLossFormRequiredEvenWhenFormalCompletionOriginHasNoActualLoss" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 tests, 0 failures, 0 errors。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProductionReleaseBusinessReadinessServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，6 tests, 0 failures, 0 errors。

## Risk And Regression Scope

- Scope: DHR 完整性检查中的 ROUTE_FORM 条件损耗表适用性；未修改建批任务生成、表单填写推进、四资料、库存追溯或 E2E 流程。
- Risk: 当前仅支持已明确验证的 `{"type":"HAS_ACTUAL_LOSS"}` 条件合同；其他条件配置会 fail fast，而不是静默跳过。

## Blockers And Follow-Up

- Blocker: none for implementation and targeted non-E2E verification.
- Follow-up: 共享缺陷总表建议将 EDHR-STATIC-026 从 `OPEN_STATIC_CONFIRMED` 更新为“targeted fixed / pending integrated E2E”，但本任务按范围不直接编辑共享缺陷总表。
