# Bug Regression Evidence: AC-M22 Release Completeness Repair

## Bug Summary And Expected Behavior

- AC-M22 放行完整性预检必须按正式来源判断：activeOrder 需匹配 `workOrderId + routeId + routeVersionId`，库存追溯需具备 `TRANSFER / SHIPMENT / BATCH_TRACE`，trace 数量、正式来源对象和 movement 状态必须闭环，PQC 组长审核通过后任务必须进入 `CONFIRMED`。
- 缺项、数量非正、来源未闭环或 PQC 仍为 `SUBMITTED` 时必须返回 blocker，不得默认通过、fallback 到旧 activeOrder 或吞异常。

## Reproduction Command Or Path

- Target command: `mvn -pl yudao-module-mes -am "-Dtest=MesOrderReleaseCompletenessServiceTest,MesPqcProcessInspectionAggregationServiceTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Adjacent PQC command: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcProcessInspectionAggregationServiceTest,MesTeamLeaderSubmissionReviewServiceTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Root Cause

- 放行预检旧 activeOrder 查询只按 `workOrderId + routeId`，没有按批次冻结的 `routeVersionId` 精确匹配。
- 库存一致性旧逻辑只检查是否存在部分 trace 和库存冻结/负数，没有验证必备来源类型、trace 数量、正式来源对象和 sourceStatus 闭环。
- PQC 过程检验聚合旧逻辑只更新 PQC record 聚合状态和明细，没有把已审核通过的 PQC task 从 `SUBMITTED` 更新为放行预检要求的 `CONFIRMED`。

## Regression Test Added Or Updated

- `MesOrderReleaseCompletenessServiceTest`: 覆盖 routeVersion-scoped activeOrder、缺必备来源、trace 数量非正、sourceStatus 未闭环和完整来源 PASS。
- `MesPqcProcessInspectionAggregationServiceTest`: 覆盖审核聚合后更新 task `CONFIRMED`，以及更新失败时 fail fast 且不写聚合明细。
- `MesFrontlinePqcContextServiceTest`: 覆盖 PQC 提交数量必须等于任务计划数量，逐项 sampleValues 数量必须等于实际检验数量。

## RED Evidence

- RED: continuation-limited -> 接手时目标测试已在工作区新增，且不能回滚既有脏改动重放真正 RED。
- RED: expected from task log -> 旧代码未按 `routeVersionId` 匹配 activeOrder，旧库存检查不校验必备 source type / trace 数量 / sourceStatus，旧 PQC 聚合不回写 `CONFIRMED`。

## GREEN Evidence

- GREEN: target -> `mvn -pl yudao-module-mes -am "-Dtest=MesOrderReleaseCompletenessServiceTest,MesPqcProcessInspectionAggregationServiceTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` PASS，合计 41 tests / 0 failures / 0 errors。
- GREEN: targeted reports -> `MesOrderReleaseCompletenessServiceTest` 8/0/0、`MesPqcProcessInspectionAggregationServiceTest` 7/0/0、`MesProEdhrReleaseServiceImplTest` 24/0/0。
- GREEN: adjacent prior reports -> `MesTeamLeaderSubmissionReviewServiceTest` 6 tests / 0 failures / 0 errors、`MesFrontlinePqcContextServiceTest` 13 tests / 0 failures / 0 errors。
- GREEN: static -> `git diff --check -- <AC-M22/PQC paths>` 通过，仅 CRLF warning。

## Verification

- Verification: `validate_bug_regression.py --evidence doc\tasks\20260805-role-matrix-code-repair\bug-regression-evidence.md` -> PASS。
- Verification: AC-M22 target JUnit PASS after same-repo Maven blocker released and the adjacent release-audit test boundary was isolated.

## Blockers And Follow-Up Actions

- CLOSEOUT_BLOCKED: 当前共享工作区 `int_main...origin/int_main [ahead 13]` 且存在大量并行脏改动，尚未执行实现提交、收尾提交或 push。
- Follow-up: 需要用户确认共享工作区提交策略，或等待并行任务完成基线/推送后，再选择性提交 AC-M22 实现与收尾记录。

## Risk And Regression Scope

- 本次修复收紧正式来源，不引入 fallback、默认成功或吞异常。
- 未覆盖真实页面 M6 `ACCEPTED` 验收；当前已完成后端 AC-M22/PQC 闭环的代码修复和目标单元回归。
