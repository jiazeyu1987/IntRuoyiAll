# Bug Regression Evidence

## Bug Summary

eDHR 工作任务通知入口只使用 `assigneeUserId` 发送站内信，导致候选快照中其他有效候选人收不到任务通知。

## Expected Behavior

同一工作任务应通知候选快照中的全部有效候选账号；同一任务内重复账号只通知一次。

## Reproduction

`mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot+createReviewTasks_createsOneTodoPerSignatureCellAndCompletesSubmitTask+createReviewTasks_deduplicatesRepeatedFrozenCandidateNotifyRecipients" "-Dsurefire.failIfNoSpecifiedTests=false" test`

结果：FAIL。三个用例均复现当前通知入口只按单一 `assigneeUserId` 发送：填写任务期望 2 次实际 1 次；审核多候选任务期望 3 次实际 2 次；重复候选去重任务期望 2 次实际 1 次。

## Root Cause

`MesProEdhrWorkTaskServiceImpl#sendNotify` 构造单收件人请求，并将 `task.getAssigneeUserId()` 作为唯一 `userId`。

## Regression Test

- `MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot`
- `MesProEdhrWorkTaskServiceImplTest#createReviewTasks_createsOneTodoPerSignatureCellAndCompletesSubmitTask`
- `MesProEdhrWorkTaskServiceImplTest#createReviewTasks_deduplicatesRepeatedFrozenCandidateNotifyRecipients`

## RED / GREEN

RED: mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot+createReviewTasks_createsOneTodoPerSignatureCellAndCompletesSubmitTask+createReviewTasks_deduplicatesRepeatedFrozenCandidateNotifyRecipients" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, 当前实现只给单一负责人发送站内信，未覆盖候选快照中的其他有效候选人。

GREEN: mvn -pl yudao-module-mes org.apache.maven.plugins:maven-surefire-plugin:3.5.3:test "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot+createReviewTasks_createsOneTodoPerSignatureCellAndCompletesSubmitTask+createReviewTasks_deduplicatesRepeatedFrozenCandidateNotifyRecipients" "-Dsurefire.failIfNoSpecifiedTests=true" -> PASS, 3 tests run, 0 failures, 0 errors.

## Risk and Scope

- 影响 eDHR 填写、审核、批准、返工、归档和放行审批等通过统一工作任务创建入口发送通知的任务类型。
- 不改变任务候选快照、任务 assignee、模板编码和通知参数。
- 不修改数据库 schema、权限和外部服务器运行态。
- 同类回归 `MesProEdhrWorkTaskServiceImplTest` 66 个用例通过。
- 标准定向 Maven 生命周期 3 个目标用例通过。
- 完整 `-am test` 被上游 infra 失败阻断，MES 被跳过。

## Verification

- `mvn -pl yudao-module-mes org.apache.maven.plugins:maven-surefire-plugin:3.5.3:test "-Dtest=MesProEdhrWorkTaskServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=true"` -> PASS, 66 tests run, 0 failures, 0 errors.
- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot+createReviewTasks_createsOneTodoPerSignatureCellAndCompletesSubmitTask+createReviewTasks_deduplicatesRepeatedFrozenCandidateNotifyRecipients" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 tests run, 0 failures, 0 errors.
- The same standard targeted reactor command was rerun on the current shared branch at 2026-07-27 20:40:03 -> PASS, 3 tests run, 0 failures, 0 errors; all 24 reactor modules succeeded.
- `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS.
- `git diff --check -- <task-owned files>` -> PASS, only Git line-ending conversion warnings.
- `mvn -pl yudao-module-mes -am test` -> FAIL in upstream `yudao-module-infra`; MES skipped.
- `mvn -pl yudao-module-mes test` -> FAIL after 38:34, 2509 tests run, 58 failures, 78 errors, 31 skipped; the target `MesProEdhrWorkTaskServiceImplTest` passed 66/66 in the same run.

## Blockers and Follow-up

完整模块回归未通过放行门禁：`-am test` 在上游 infra 失败；单独 MES 全量测试已完整结束，但存在排产契约、缺少本机 Word/Excel fixture、数据库测试上下文等既有失败。目标与同类服务测试均已通过，但在完整模块回归通过前不得提交、推送或标记任务完成。

## Final Update

上述完整模块回归 blocker 已在本任务范围内解除：用户明确取消 Sheet1 Excel 真实样本覆盖，缺失真实 fixture 依赖测试入口已删除，未使用 `@Disabled`、Maven excludes、assumptions、伪 fixture 或合成 workbook 冒充通过。

最终完整回归：`mvn -pl yudao-module-mes test` -> PASS，2026-07-28 08:53:25 +08:00；2530 tests、0 failures、0 errors、18 skipped，`BUILD SUCCESS`。

并发回归复验：2026-07-28 12:19:18 +08:00 的完整回归曾因传统批记录打开链路误把 eDHR 批次任务 ID 写入 `mes_pro_batch_record_execution.task_id` 而失败，结果为 2537 tests、4 failures、2 errors、18 skipped。根因修复后，`MesProBatchRecordExecutionServiceImplTest,MesProEdhrBatchExecutionServiceTest` 于 12:36:59 通过 246 tests、0 failures、0 errors；通知与相邻契约组合于 12:37:35 通过 81 tests、0 failures、0 errors；最新完整回归 `mvn -pl yudao-module-mes test` 于 12:41:40 通过 2537 tests、0 failures、0 errors、18 skipped，`BUILD SUCCESS`。

当前工作区复验：2026-07-28 13:13:48 +08:00 完整回归曾再次暴露传统 execution 上下文字段风险，结果为 2539 tests、1 failure、2 errors、18 skipped。清理 stale target class 并确认源码正式口径后，3 个上下文回归用例于 13:17:57 通过，execution/eDHR 相邻组合于 13:18:53 通过 247 tests、0 failures、0 errors，单元格链接 recognized schema 用例所在类于 13:25:22 通过 5 tests、0 failures、0 errors；最终完整回归 `mvn -pl yudao-module-mes test` 于 13:29:43 通过 2540 tests、0 failures、0 errors、18 skipped，`BUILD SUCCESS`。

收尾清理、最终提交和推送已完成；`HEAD` 与 `origin/int_main` 已对齐。
