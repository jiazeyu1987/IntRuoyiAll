# Verification Report

## Result

PASS for the requested 一线 PQC 工序卡片 source change.

## Independent Verification (2026-08-09)

- PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldDisplayOnlyQaInspectionItemProcessesWhenRouteHasExtraProcesses+shouldAttachPqcTaskFromQaRegulationProcessWhenCurrentRouteProcessIdsDrift" "-Dsurefire.failIfNoSpecifiedTests=false" test`，Surefire 进入目标类并执行 2 个用例，Failures/Errors/Skipped 均为 0。
- PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldLoadProcessesFromSelectedActiveOrderProductRoute+shouldExposeFirstAndPatrolTaskOptionsForSameProcess+shouldHideRouteProcessesWithoutQaInspectionItemsAndAttachPqcTaskToPendingProcess+shouldPreparePqcPieceDetailContextWithBulkQueriesOnly+shouldDisplaySubmittedPqcTaskProcessWithoutTaskContextAndKeepPendingProcess+shouldFailFastWhenPendingPqcTaskMissingFormalProcessIdentity" "-Dsurefire.failIfNoSpecifiedTests=false" test`，Surefire 执行 6 个相邻用例，Failures/Errors/Skipped 均为 0。
- PASS: `git diff --check -- <task-owned backend/docs files>`，退出码 0；仅有既有 LF/CRLF warning，无 whitespace error。
- PASS: 源码核对确认 `listProcessesByActiveOrder` 从 `selectPublishedListByProductRouteVersion` 读取发布态 QA 规程工序集合，路线工序表仅用于补充排序/工位信息，未作为卡片候选来源。

## Verified Behavior

- PQC 工序卡片由所选生产工单产品的发布态 QA 检验规程工序集合生成。
- 工艺路线中存在但 QA 检验项目列表未配置的工序不再显示。
- QA 工序候选仍能聚合首检/巡检任务选项、检验项目和生产提交候选。
- 待检任务不属于 QA 工序集合时 fail fast，不走路线 fallback。

## Commands

- PASS: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest#shouldDisplayOnlyQaInspectionItemProcessesWhenRouteHasExtraProcesses" test`
- PASS: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest#shouldDisplayOnlyQaInspectionItemProcessesWhenRouteHasExtraProcesses+shouldAttachPqcTaskFromQaRegulationProcessWhenCurrentRouteProcessIdsDrift" test`
- PASS: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest#shouldLoadProcessesFromSelectedActiveOrderProductRoute+shouldExposeFirstAndPatrolTaskOptionsForSameProcess+shouldHideRouteProcessesWithoutQaInspectionItemsAndAttachPqcTaskToPendingProcess+shouldPreparePqcPieceDetailContextWithBulkQueriesOnly+shouldDisplaySubmittedPqcTaskProcessWithoutTaskContextAndKeepPendingProcess+shouldFailFastWhenPendingPqcTaskMissingFormalProcessIdentity" test`
- PASS: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260808-frontline-pqc-process-cards-qa-items/backend-api-evidence.md`
- PASS: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260808-frontline-pqc-process-cards-qa-items/bug-regression-evidence.md`
- PASS: `git diff --check -- <task-owned files>`

## Known Non-Task Failure

- `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest" test` still fails because of existing PQC submit-flow dirty baseline issues outside this task, including missing submit command fields, production submit event device context expectations, and Mockito unnecessary stubbing.

## Final Status

completed
