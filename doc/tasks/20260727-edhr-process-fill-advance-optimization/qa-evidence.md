# QA Evidence

## Scope

Scope: eDHR 工序多填写人个人工作台、FormCenter 动态表单入口、填写提交、过程检验优先推进、无过程检验推进、真实数据清理。

## Requirement To Test Matrix

Matrix: candidate 非 assignee 可见 -> 后端 `getMyPage_includesCandidateFillTaskForNonAssignee` + 真实 E2E `noInspection`。
Matrix: 过程检验存在时主表填写人不推进 -> 后端 `completeFillAndCreateNextFill_doesNotAdvanceWhenInspectionFillerExistsAndActorIsOnlyMainFiller` + 真实 E2E `mainBlockedByInspection`。
Matrix: 过程检验填写人可推进 -> 后端 `completeRouteFormFillAndCreateNextFill_advancesWhenActorIsInspectionFiller` + 真实 E2E `inspectionAdvances`。
Matrix: 无过程检验时全部解析填写人可推进 -> 后端 `completeRouteFormFillAndCreateNextFill_allowsAnyProcessFillerWhenNoInspectionFiller` + 真实 E2E `noInspection`。
Matrix: 非填写人 fail-fast -> 后端 `completeRouteFormFillAndCreateNextFill_rejectsActorOutsideProcessFillerSnapshot`。
Matrix: 前端统一入口 -> 三个静态合同 + 真实工作台行级“处理”点击。

## Test Data And Fixtures

Test data: 本机 Docker MySQL `ruoyi-vue-pro`、租户 `测试租户`、用户 `aoteman/admin`；E2E 动态创建 task-owned `mes_pro_work_order`、`mes_pro_edhr_batch_execution`、`mes_pro_edhr_batch_execution_task`、`mes_pro_edhr_work_task`、FormCenter 实例和审批策略，批次前缀 `EDHR-ADV-<runId>`。

## RED

RED: 新增/更新目标测试初始覆盖旧行为失败；真实 E2E 夹具在缺少正式工单、字段归属或 root/predecessor 拓扑时被后端 fail-fast 拦截，作为测试夹具修复证据。

## GREEN

GREEN: 后端 Maven 目标测试 PASS，5 tests, 0 failures。
GREEN: 前端静态合同三项 PASS。
GREEN: 完整真实 E2E PASS，runKey `EDHR-ADV-6T182008199Z`，三场景 DB evidence 均满足预期。
GREEN: 清理 SQL 后 `batch_execution/work_task/work_order/form_instance` 活跃残留 `0/0/0/0`。

## Failed Or Blocked Tests

Verification: 无剩余失败或跳过；`git diff --check` PASS。

## Release Recommendation

Blockers: none。建议放行该行为切片进入后续集成；仍需按项目 closeout 规则完成经验沉淀和临时产物清理。
