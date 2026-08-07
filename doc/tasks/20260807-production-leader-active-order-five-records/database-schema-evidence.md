# Database Schema Evidence

## Data Change Goal And Affected Entities

- Goal: 在本机业务租户创建 5 条满足正式活跃订单候选资格的数据，并通过生产组长页面加入活跃订单池。
- Entities: 生产工单、排产工单、排产工序；页面加入成功后由正式服务写入活跃订单、工序快照、PQC 任务和维护审计。

## Database Engine And Migration Tool

- Engine: 本机 Docker MySQL `8.0.39`，数据库 `ruoyi-vue-pro`。
- Migration tool: 本任务不修改 schema；任务自有 fixture 使用受控事务和真实表结构，最终业务写入走正式页面/API 事务。

## Schema, Fixture, Seed, Index Or Constraint Changes

- Schema/index/constraint: 无变更。
- Fixture/seed: 新增工单 `980022..980026 / CODX-AO5-20260807-01..05`、排产 `148..152`、排产工序 `3465..3469`，以及 QA 规程 `36 / CODX-AO5-QA-20260807`、发布版本 `36/V1` 和 FIRST/PATROL/FINAL 三条项目。
- Formal source: tenant 1 产品 `924008/IDI`、路线 `980091/RT000028-IDI`、ACTIVE 版本 `622/V1`、路线工序 `980631/922985`；未修改这些共享主数据。

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

- 写入前只读查询 -> `work_orders=0 / schedules=0 / regulations=0 / active_orders=0`，符合预期 RED。
- 首轮 fixture -> FAIL，完整路线版本快照超出排产工单 `TEXT` 容量且事务整体回滚；随后使用只包含唯一启用工序的结构化排产快照。

## GREEN Command And Passing Result

- Fixture GREEN: `regulation_id=36 / regulation_version_id=36 / fixture_count=5`。
- Pre-UI verification GREEN: 5 条工单/排产/排产工序数量、状态、路线版本、数量因子、计划数量和计划日期完整；已发布规程具有 FIRST=2、PATROL=10%、FINAL=3；ACTIVE 活跃订单仍为 0。
- Pending: 真实页面加入响应和最终 DB/UI 数量。

## Migration Verification

- 无 schema migration；`SHOW COLUMNS/SHOW INDEX`、受控事务、`verify.sql` 已通过，最终候选/API/UI/DB 验证待执行。

## Blockers

- 暂无数据前置 blocker；Playwright CLI 的 Windows 会话稳定性正在验证，目标写入仍必须通过真实页面完成。
