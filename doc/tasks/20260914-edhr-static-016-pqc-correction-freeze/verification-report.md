# EDHR-STATIC-016 Verification Report

## Bug Summary

PQC提交进入不合格评审并冻结工单后，普通“修改PQC表单”更正入口仍可继续签名、修订事件、删除重建逐件检验结果、更新PQC正式记录并触发过程检验汇集。

## Expected Behavior

待处理不合格评审或工单临时冻结期间，普通PQC更正入口必须在签名和任何写入前 fail fast；只有冻结解除且其它前置满足时，原有更正流程才可继续。

## Reproduction

RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-016-pqc-correction-freeze-static.spec.cjs` -> FAIL, expected reason: `MesProcessPoolPqcInspectionCorrectionService` did not depend on `MesProEdhrNonconformanceReviewService` and did not call the formal work-order freeze gate before correction writes.

## Root Cause

`MesProcessPoolPqcInspectionCorrectionService.correct` 只校验事件、PQC任务、组长管理范围和最终放行状态，没有复用不合格评审已经建立的 `ensureWorkOrderNotFrozen` 工单冻结门禁。冻结期间的更正签名和正式事实更新因此仍可进入事务。

## Fix Summary

- `MesProcessPoolPqcInspectionCorrectionService` 注入 `MesProEdhrNonconformanceReviewService`。
- `correct` 在锁定并校验 `MesPqcInspectionTaskDO` 后立即调用 `ensureWorkOrderNotFrozen(task.getWorkOrderId(), "PQC更正")`。
- 新增静态合同锁定冻结门禁必须早于签名、事件修订和正式PQC表写入。
- 新增单元测试锁定冻结拒绝路径不调用签名、revision、逐件结果重建、正式记录更新或汇集。
- 缺陷索引中 EDHR-STATIC-016 更新为 `FIXED_STATIC_VERIFIED`。

## Verification

GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-016-pqc-correction-freeze-static.spec.cjs` -> PASS.

GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolPqcInspectionCorrectionServiceTest,MesProcessPoolEventFreezeGateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS; Tests run: 7, Failures: 0, Errors: 0, Skipped: 0.

GREEN: `git diff --check` -> PASS.

Cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260914-edhr-static-016-pqc-correction-freeze --mode preview --worktree-closeout off --json` -> PASS; keep=`task.md/execution-log.md/verification-report.md`, delete=[], blocked=[], warnings=[].

Non-E2E scope honored: no Playwright/E2E, database writes, service start/stop/restart, remote server operations, git commit, or git push were performed.

## Blockers

Final project completion still depends on the repository closeout rule requiring commit and push before `completed`. This turn has no explicit git commit/push authorization, so the task remains `blocked` unless authorization is granted.

## Risk And Regression Scope

The targeted verification covers the frozen write-prevention path, call order before mutation, and the existing unfrozen PQC correction happy path. It does not prove the real browser page hides or disables the correction button during freeze because E2E and runtime validation were outside this turn's authorized scope.
