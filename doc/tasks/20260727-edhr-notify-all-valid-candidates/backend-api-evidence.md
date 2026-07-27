# Backend API Evidence

## Scope

Service behavior: `MesProEdhrWorkTaskServiceImpl#createTask` -> `sendNotify` for eDHR work task station-message recipients.

## Contract

- Data contract: notification recipients come from the created task's `candidateUserSnapshot`.
- Recipient behavior: notify every parsed candidate user once per work task; repeated IDs in the same snapshot are de-duplicated by existing authorization parsing.
- Template behavior: preserve existing template code resolution, template parameters, `actionUrl`, and `workTaskId`.
- Failure behavior: missing or invalid candidate snapshot remains fail-fast through existing `parseCandidateUserIds`; no assignee fallback is introduced.

## Auth, Permission, and Validation

- Runtime entitlement sync already uses the same candidate snapshot parser and remains unchanged.
- No controller API, permission code, menu, tenant binding, or database schema is changed.
- Candidate validity remains owned by candidate snapshot creation and resolver validation.

Validation: notification delivery is verified through Mockito captures on `NotifyMessageSendApi#sendSingleMessageToAdmin` and existing entitlement sync assertions.

## BDD

BDD: 填写任务通知全部有效候选人 -> Given 一个待办填写任务的候选快照包含多个有效账号，When 创建该工作任务，Then 每个有效候选账号各收到一条填写任务站内信。

BDD: 审核任务通知全部有效候选人 -> Given 一个待办审核任务的候选快照包含多个有效账号且当前任务有一个实际 assignee，When 创建该审核任务，Then 候选快照中的每个有效候选账号各收到一条审核任务站内信。

BDD: 同一任务候选账号去重 -> Given 一个任务候选快照重复包含同一账号，When 发送任务通知，Then 该账号只收到一条站内信。

BDD: 候选来源不混淆 -> Given 填写任务和审核任务拥有不同候选快照，When 分别创建任务，Then 每个任务只按自己的候选快照通知，不把两个任务的候选人合并。

## RED

`mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot+createReviewTasks_createsOneTodoPerSignatureCellAndCompletesSubmitTask+createReviewTasks_deduplicatesRepeatedFrozenCandidateNotifyRecipients" "-Dsurefire.failIfNoSpecifiedTests=false" test`

RED: FAIL. Current `sendNotify` sent only to `assigneeUserId`, so multi-candidate and duplicate-candidate notification expectations failed.

## GREEN

`mvn -pl yudao-module-mes org.apache.maven.plugins:maven-surefire-plugin:3.5.3:test "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot+createReviewTasks_createsOneTodoPerSignatureCellAndCompletesSubmitTask+createReviewTasks_deduplicatesRepeatedFrozenCandidateNotifyRecipients" "-Dsurefire.failIfNoSpecifiedTests=true"`

GREEN: PASS. 3 tests run, 0 failures, 0 errors.

## Verification

- `mvn -pl yudao-module-mes org.apache.maven.plugins:maven-surefire-plugin:3.5.3:test "-Dtest=MesProEdhrWorkTaskServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=true"` -> PASS, 66 tests run, 0 failures, 0 errors.
- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot+createReviewTasks_createsOneTodoPerSignatureCellAndCompletesSubmitTask+createReviewTasks_deduplicatesRepeatedFrozenCandidateNotifyRecipients" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 tests run, 0 failures, 0 errors.
- The same standard targeted reactor command was rerun on the current shared branch at 2026-07-27 20:40:03 -> PASS, 3 tests run, 0 failures, 0 errors; all 24 reactor modules succeeded.
- `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS.
- `git diff --check -- <task-owned files>` -> PASS, only Git line-ending conversion warnings.
- `mvn -pl yudao-module-mes test` -> FAIL after 38:34, 2509 tests run, 58 failures, 78 errors, 31 skipped; `MesProEdhrWorkTaskServiceImplTest` passed all 66 tests in the same run.

## Blockers

`mvn -pl yudao-module-mes -am test` fails in upstream `yudao-module-infra` before MES executes. The direct MES module run completed but failed in existing scheduling contracts, missing local Word/Excel fixtures, database test contexts, and other unrelated suites. Full module regression, closeout, and the post-verification evidence commit/push remain blocked.
