# Backend API Evidence

## Scope

eDHR 工作任务个人工作台、填写提交、FormCenter 路线表单提交后下一工序创建，以及工序推进资格判定。

## API Contract And Data Contract

Contract: `edhr-work-task/my-page` 必须包含当前用户作为 assignee 或 `candidateUserSnapshot` 成员的待办；`edhr-batch-execution/task/open` 和 FormCenter submit 后端 effect 必须使用真实 `workTaskId/batchTaskId/batchExecutionId` 上下文。
Validation: 提交人必须属于当前工作任务 assignee/candidate；推进人必须属于当前工序推进集合。若当前工序存在 `PROCESS_INSPECTION` 填写任务，推进集合为过程检验填写人并集；否则推进集合为当前工序所有填写任务 assignee/candidate 并集。非填写人或非推进人 fail-fast。

## BDD

BDD: candidate 待办可见 -> Given 当前用户在 `candidateUserSnapshot` 中但不是 assignee, When 查询个人工作台, Then 返回该填写任务。
BDD: 过程检验优先推进 -> Given 当前工序存在过程检验填写任务, When 主表填写人提交, Then 当前任务完成但不创建下一工序任务。
BDD: 过程检验填写人推进 -> Given 当前工序存在过程检验填写任务, When 过程检验填写人提交, Then 创建下一工序填写任务。
BDD: 无过程检验时全部填写人可推进 -> Given 当前工序没有过程检验填写任务, When 任一解析填写人提交, Then 创建下一工序填写任务。
BDD: 非填写人 fail-fast -> Given 当前用户不在工作任务或工序推进集合, When 提交或推进, Then 后端返回权限错误。

## RED

RED: 目标后端测试初始失败，旧逻辑仅按 `assigneeUserId` 过滤个人工作台，且下一工序创建未按“过程检验填写人优先，否则当前工序全部填写人”计算推进资格。

## GREEN

GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrWorkTaskServiceImplTest#getMyPage_includesCandidateFillTaskForNonAssignee+completeFillAndCreateNextFill_doesNotAdvanceWhenInspectionFillerExistsAndActorIsOnlyMainFiller+completeRouteFormFillAndCreateNextFill_advancesWhenActorIsInspectionFiller+completeRouteFormFillAndCreateNextFill_allowsAnyProcessFillerWhenNoInspectionFiller+completeRouteFormFillAndCreateNextFill_rejectsActorOutsideProcessFillerSnapshot" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS，5 tests, 0 failures。

## Contract Verification

Verification: 完整真实 E2E `node tests\e2e\edhr-work-task-process-advance-real.e2e.js` -> PASS，三种 DB 结果分别为 nextFillCount `1/0/1`，workTaskStatus `DONE`，batchTaskStatus `40`，FormCenter effectStatus `APPLIED`。

## Observability

真实 E2E 记录 runKey `EDHR-ADV-6T182008199Z`、本机 frontend/backend URL、测试租户与用户标签；清理后活跃任务自有数据残留为 0。

## Blockers

Blockers: none。
