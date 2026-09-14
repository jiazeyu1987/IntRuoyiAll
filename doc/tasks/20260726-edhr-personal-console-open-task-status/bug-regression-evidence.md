# Bug Regression Evidence

## Bug Summary

`zhangkeying` 从个人控制台进入 eDHR 待办时看到“当前 eDHR 批次状态不允许该操作”。真实数据核对后发现目标工作任务 `EDHRT-1784803798526` 仍是 `TODO`，但所属批次 `900000000739` 已作废，`openTask` 阻断正确，错误在个人控制台仍把终态批次残留任务展示成可处理待办。

## Expected Behavior

关闭、归档、驳回、作废批次不得进入可处理填写页；同时个人控制台待办、统计、审批中心 TODO 和候选签名 TODO 不应展示或计入这些终态批次残留任务。

## Reproduction

- Browser path: `http://localhost:8081/user/profile`
- Tenant/user label: `芋道源码/zhangkeying`
- Observed before fix: target task appeared in personal console and `openTask` returned `当前 eDHR 批次状态不允许该操作`.
- DB evidence: `mes_pro_edhr_work_task.task_code='EDHRT-1784803798526'`, `status='TODO'`, joined `mes_pro_edhr_batch_execution.status=60`.

## Root Cause

`MesProEdhrWorkTaskMapper.selectMyPage(...)` and `countMy(...)` filtered only work-task assignee/status/type. They did not exclude work tasks whose `batchExecutionId` referenced terminal eDHR batch statuses, so stale active tasks from voided batches were still surfaced as actionable personal-console tasks.

## Regression Test

Added `MesProEdhrWorkTaskServiceImplTest#getMyPage_excludesTodoTasksFromTerminalBatches`, covering one normal in-progress batch task plus one voided-batch `TODO` task for the same assignee. The test asserts personal page and stats return only the normal batch task.

## RED: Failing Regression

`mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#getMyPage_excludesTodoTasksFromTerminalBatches" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected page total 1 but was 2`.

## GREEN: Passing Regression

- `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#getMyPage_excludesTodoTasksFromTerminalBatches" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 1, Failures: 0, Errors: 0`.
- `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#getMyPage_excludesTodoTasksFromTerminalBatches+getApprovalCenterTodoPage_excludesPersonalFillTasksAndKeepsApprovalTasks,MesProEdhrBatchExecutionServiceTest#openTask_allowsApprovedDynamicRouteFormBeforeCloseForCurrentFiller+openTask_allowsApprovedOrdinaryFillCompletedBeforeReleaseForHistoricalFiller+openTask_rejectsClosedBatch" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 5, Failures: 0, Errors: 0`.

## Verification

- Build/runtime: worktree `yudao-server-exec.jar` was built and copied to the local runtime; source and deployed jar SHA256 matched `1F251FC510467CA86C620E6F81FE55CE6F2D1522219700CFB0E5307C2C85D21A`; backend health returned `UP`.
- Real E2E: `zhangkeying` from `http://localhost:8081/user/profile` returned personal-console eDHR API totals `[0,0]`, did not show task `EDHRT-1784803798526`, and did not trigger the terminal-status toast.

## Risk And Regression Scope

The fix excludes terminal batch statuses `30/40/50/60` only from actionable TODO/OVERDUE surfaces. Completed-history pages remain unchanged, and `openTask` continues to fail fast for terminal batches.

## Blockers And Follow-Up

No product fallback was added. Closeout and merge are blocked until unrelated main-worktree dirty changes can be safely isolated.
