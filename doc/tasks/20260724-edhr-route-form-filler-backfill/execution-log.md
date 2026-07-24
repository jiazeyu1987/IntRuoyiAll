# Execution Log

## User Intent

用户确认继续处理：工艺路线里绑定的损耗单配置了填写人，但批次详情右侧单据卡片仍显示未配置。要求继续修复。

## Evidence Before Fix

- Runtime read-only query: `EDHRB-1784855561493` 的路线版本 `routeVersionId=358` 中，粗洗/精洗/清洗工序绑定的“损耗单”均配置 `candidateSourceNames=["张可莹（zhangkeying）"]`、`candidateSourceIds=[152]`。
- Runtime read-only query: 同一批次详情接口中 3 个“损耗单”任务 `fillableUsers=[]`。

## BDD

BDD: 动态表单任务显示工艺路线绑定填写人 -> Given 工艺路线工序绑定的损耗单配置了填写人, When 用户打开批次执行详情, Then 对应损耗单任务 `fillableUsers` 必须返回该配置人员，供右侧单据卡片显示。

BDD: 主生产表任务填写人逻辑不被破坏 -> Given 主生产表或已有工作任务已有填写人来源, When 批次执行详情组装任务列表, Then 仍优先使用既有工作任务或任务分配规则解析填写人，不被动态表单绑定回填覆盖。

## Milestone Updates

- M1 completed: `toResp(...)` 原本只从有效填写工作任务、过程表单规则和工序任务规则解析 `fillableUsers`，未读取任务 `routeBindingId` 对应路线绑定的 `candidateSourceType/candidateSourceIds`。
- M2 completed: 已新增并校准 `detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated`。该测试只配置路线损耗单绑定 `USERS/152`，不创建填写工作任务、不插入过程表单规则或工序规则，断言响应返回 `张可莹（zhangkeying）`。
- RED: 修复前同类数据在运行时已复现：路线配置有 `candidateSourceIds=[152]`，批次详情对应损耗单任务 `fillableUsers=[]`。
- M3 completed: `MesProEdhrBatchExecutionServiceImpl` 新增 `routeBindingId -> MesProRouteFlowProcessBatchRecordDO -> candidateSourceType/candidateSourceIds -> fillableUsers` 回填链路。优先级保持为有效填写/返工工作任务 -> 过程表单规则 -> 工序填写分配规则 -> 路线绑定候选人，不引入前端推断或兜底。
- M3 completed: `MesProRouteFlowConfigServiceImpl` 补齐缺失的 `resolveRecordbookEnabled(Boolean, String)` 编译缺口，复用既有批记录执行服务规则：内部记录禁用记录本，批记录默认启用。
- M4 completed: `mvn -pl yudao-module-mes -DskipTests compile` -> PASS。
- M4 completed: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated test` -> PASS。
- M4 completed: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromActiveFillWorkTask+detailTask_includesFillableUsersFromAssignmentRuleWhenWorkTaskNotCreated+detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated test` -> PASS，3 tests, 0 failures, 0 errors。

## Current Blocker

- None for the task-owned verification scope.
