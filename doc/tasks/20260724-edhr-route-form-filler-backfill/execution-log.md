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

- M1 completed: `toResp(...)` 原本只从有效填写工作任务、过程表单规则和工序任务规则解析 `fillableUsers`，未读取任务 `routeBindingId` 对应的路线单据绑定候选人字段。
- M2 completed: 已新增 `detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated`。该测试仅在路线损耗单绑定配置 `USERS/152`，不创建填写工作任务、不插入过程表单规则或工序规则，断言响应返回 `张可莹（zhangkeying）`。
- RED: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated test` -> BLOCKED，测试上下文启动时报告 `NoClassDefFoundError: MesDvRepairPageReqVO`；该类源码和编译输出存在，属于 Maven/Surefire 类路径异常，未执行到回归断言。
- 实现尝试: 已在 `MesProEdhrBatchExecutionServiceImpl` 写入冻结路线候选人字段及基于 `routeBindingId` 的填写人回填。随后发现同一服务文件被并发修改，写入的路线绑定回填逻辑已消失，未覆盖对方变更。
- GREEN: `mvn -pl yudao-module-mes -DskipTests compile` -> PASS（写入逻辑存在时）。之后有其他未归属改动使 `MesProBatchRecordExecutionFieldAuditServiceImpl` 出现 4 个编译错误，当前无法重跑目标测试。

## Current Blocker

- 同文件并发冲突：`MesProEdhrBatchExecutionServiceImpl.java` 的路线绑定回填改动被覆盖。为遵守不覆盖其他任务改动的规则，等待用户确认后才重新合并。
- 无关编译阻塞：`MesProBatchRecordExecutionFieldAuditServiceImpl.java` 缺少 `parseOptionalJson`、`parseCanonicalValueJson`，且 `ResolvedChange` 构造器参数不匹配；该文件为其他未提交改动，未修改。
