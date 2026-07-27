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
- `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS.
- `git diff --check -- <task-owned files>` -> PASS, only Git line-ending conversion warnings.
- `mvn -pl yudao-module-mes -am test` -> FAIL in upstream `yudao-module-infra`; MES skipped.
- `mvn -pl yudao-module-mes test` -> TIMEOUT after 15 minutes without a fresh complete report.

## Blockers and Follow-up

完整模块回归未通过放行门禁：`-am test` 在上游 infra 失败，单独 MES 全量测试超时。目标与同类服务测试均已通过，但在完整模块回归通过前不得提交、推送或标记任务完成。
