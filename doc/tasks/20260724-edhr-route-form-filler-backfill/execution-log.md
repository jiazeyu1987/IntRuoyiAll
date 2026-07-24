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

- M1 completed: `toResp(...)` 原本只从有效填写工作任务和工序任务分配规则解析 `fillableUsers`；动态表单槽位发布后同步出来的 `MesProEdhrProcessFormPermissionRuleDO` 未参与详情任务填写人回填。
- M2 completed: 已新增并校准 `detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated`。该测试使用冻结路线快照、动态表单 `formBindingKey`、发布后同步的 `FILL/USERS/152` 表单权限规则，断言响应返回 `张可莹（zhangkeying）`。
- RED: `mvn '-Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated' surefire:test`（模块目录）-> FAIL，断言 `expected: <[152]> but was: <[]>`，证明动态表单规则未回填到 `fillableUsers`。
- M3 completed: `MesProEdhrBatchExecutionServiceImpl` 新增动态表单填表规则映射，详情组装优先级为有效填写/返工工作任务 -> 表单权限规则 -> 工序填写分配规则；表单权限规则按 `batchRecordReportId` 优先、否则 `formBindingKey` 查询，候选人解析复用 `MesProEdhrCandidateResolver`，不引入兜底。
- M4 completed: `mvn '-Dmaven.compiler.useIncrementalCompilation=false' -DskipTests compile` -> PASS；隔离编译目标测试类 `javac @target\javac-edhr.args` -> PASS；`mvn '-Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromActiveFillWorkTask+detailTask_includesFillableUsersFromAssignmentRuleWhenWorkTaskNotCreated+detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated' '-Dsurefire.useManifestOnlyJar=false' surefire:test` -> PASS，3 tests, 0 failures, 0 errors。

## Current Blocker

- Full module `mvn ... test` / `testCompile` remains blocked by unrelated legacy tests such as `MesWmProductSalesDetailMapperTest` and `MesMdAutoCodeRecordServiceImplTest` referencing missing WM/MD classes. This task did not modify those tests and used isolated compilation for the owned regression scope.
