# Database Schema Evidence

## Data Change Goal And Affected Entities

- Goal: 在本机业务租户创建 5 条满足正式活跃订单候选资格的数据，并通过生产组长页面加入活跃订单池。
- Entities: 生产工单、排产工单、排产工序；页面加入成功后由正式服务写入活跃订单、工序快照、PQC 任务和维护审计。

## Database Engine And Migration Tool

- Engine: MySQL 8 compatible local instance，具体版本待只读查询确认。
- Migration tool: 本任务不修改 schema；任务自有 fixture 使用受控事务和真实表结构，最终业务写入走正式页面/API 事务。

## Schema, Fixture, Seed, Index Or Constraint Changes

- Schema/index/constraint: 无变更。
- Fixture/seed: 待核对 schema 后记录 5 条任务订单、排产和排产工序的精确业务标识与来源。

## Data Safety Analysis

- 仅操作本机确认租户和任务唯一前缀。
- 不修改被复用的正式主数据、路线版本或 QA 规程。
- 不直接写活跃订单、工序快照和 PQC 任务表；由正式加入接口在同一事务生成。
- 写入前后均按租户、任务前缀和精确 ID 复核；任一数量不符立即回滚/停止。

## Rollback Or Recovery Plan

- 回滚限定为任务前缀对应的 5 条订单及其任务自有排产/排产工序，以及正式加入动作生成的活跃订单、快照、PQC 任务和审计记录。
- 执行回滚前必须重新核对精确 ID 和引用关系；默认不执行回滚，因为 5 条数据是用户要求保留的交付结果。

## BDD Scenarios

- BDD: 合格候选通过真实页面加入 -> Given 5 条任务订单满足正式候选资格 When 生产组长逐条从远程下拉选择并加入 Then 5 条活跃订单及正式子记录完整存在。
- BDD: 前置缺失阻塞 -> Given 任一正式资格前置缺失 When 执行候选预检 Then 不直接写活跃订单或相关子表。

## RED Command And Expected Failure

- Pending: 写入前只读查询应证明任务前缀对应候选/活跃订单数量为 0。

## GREEN Command And Passing Result

- Pending: 5 条候选资格、真实页面加入响应和最终 DB/UI 数量均通过。

## Migration Verification

- 无 schema migration；将记录 `DESCRIBE`、受控事务结果、候选接口只读结果及最终 DB/UI 验证。

## Blockers

- 暂无数据前置结论；正在核对真实 schema 与可复用的完整 QA 规程组合。
