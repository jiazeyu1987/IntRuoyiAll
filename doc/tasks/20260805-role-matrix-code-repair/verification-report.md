# Verification Report

## Scope

本报告属于当前目录的 AC-M22 批记录完整性预检修复任务。调拨写入口与活跃订单边界修复证据已迁移到 `doc/tasks/20260805-transfer-active-order-repair/`，避免与 AC-M22 并行任务记录混用。

## Results

- CODE_FIXED: `MesOrderReleaseCompletenessServiceImpl` 已改为按 `workOrderId + routeId + routeVersionId` 查找 activeOrder，并对库存完整性增加必备来源 `TRANSFER/SHIPMENT/BATCH_TRACE`、trace 数量/来源对象完整性、movement 来源状态闭环校验。
- CODE_FIXED: `MesPqcProcessInspectionAggregationServiceImpl` 已在 approved PQC 聚合成功后调用 `MesPqcInspectionTaskMapper.updateConfirmedIfSubmitted(...)`，把正式 PQC task 从 `SUBMITTED` 条件更新为 `CONFIRMED`；更新失败 fail fast。
- TESTS_ADDED: `MesOrderReleaseCompletenessServiceTest` 覆盖路线版本匹配、缺必备库存来源、数量非正、sourceStatus 未闭环和完整来源 PASS；`MesPqcProcessInspectionAggregationServiceTest` 覆盖 PQC 聚合确认 task 和确认失败 fail fast。
- STATIC_CHECK_PASS: `git diff --check -- <AC-M22 paths>` 通过，仅 CRLF warning。
- JUNIT_PASS_PARTIAL: Surefire 已生成 `MesPqcProcessInspectionAggregationServiceTest` 4 tests / 0 failures / 0 errors、`MesTeamLeaderSubmissionReviewServiceTest` 6 tests / 0 failures / 0 errors、`MesFrontlinePqcContextServiceTest` 13 tests / 0 failures / 0 errors。
- JUNIT_PASS_TARGET: `mvn -pl yudao-module-mes -am "-Dtest=MesOrderReleaseCompletenessServiceTest,MesPqcProcessInspectionAggregationServiceTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过，合计 41 tests / 0 failures / 0 errors。
- TEST_FIX: `MesProEdhrReleaseServiceImplTest#approveRejectsUnverifiableSignoffEvidenceHash` 增加 `clearInvocations(operationAuditService)`，隔离 `insertPendingApprovalRelease(...)` 前置预检审计调用，避免把前置预检审计误判为审批失败路径审计；生产代码未改变。
- EVIDENCE_VALIDATOR_PASS: `validate_bug_regression.py --evidence doc\tasks\20260805-role-matrix-code-repair\bug-regression-evidence.md` 通过。
- CLEANUP_PASS: `task-closeout-cleanup` preview/apply 通过；保留 `task.md`、`execution-log.md`、`verification-report.md`、`bug-regression-evidence.md`，无删除项、阻塞项或警告。
- EXPERIENCE_UPDATED: `docs/powershell-memory.md` 已校正 AC-M22 Maven javac/Lombok 证据行，保留“阻塞时不得宣称 JUnit 通过、释放后必须复跑标准 Maven”的长期门禁。
- CLOSEOUT_BLOCKED: 当前共享工作区 `int_main...origin/int_main [ahead 13]` 且存在大量并行脏改动；为避免混入其它任务文件，尚未执行 AC-M22 实现提交、收尾提交或 push。

## Remaining Risks

- 当前分支已有大量并行脏改动和本地 ahead 13；未执行实现提交、收尾提交或 push。
- 真实页面全量 `ACCEPTED` / M6 E2E 仍不在本次已验证范围内。
