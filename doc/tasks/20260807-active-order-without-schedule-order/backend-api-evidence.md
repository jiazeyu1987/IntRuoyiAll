# Backend API Evidence

## Scope

- Service: `MesTeamLeaderActiveOrderServiceImpl`
- API behavior: 生产组长活跃订单候选搜索与加入。

## Contract And Data

- 有一条有效排产工单：保持既有排产路线、版本、工序、计划数量和计划日期来源。
- 无有效排产工单：使用生产工单产品的唯一正式路线绑定、路线唯一 ACTIVE 发布版本、发布快照工序与 SCHEDULE 数量系数；使用 ERP 工单数量和 ERP 计划开工日期。
- 多条有效排产工单：继续阻塞。
- 不修改数据库 schema、权限或接口字段。

## Validation And Failure

- 正数 ERP 数量、唯一产品路线绑定、唯一 ACTIVE 版本、完整发布快照、至少一个启用工序、正数数量系数、ERP 计划开工时间和正式 PQC 规程均为必需前置条件。
- 任一正式来源缺失时候选不可加入，新增操作抛出明确业务错误且事务不写入。

## Required Services And Migrations

- 依赖现有 MES mapper 和当前测试夹具。
- 无配置新增，无 schema 迁移。

## BDD Scenarios

- 无排产正式来源完整时可加入并生成快照/PQC 任务。
- 无排产正式来源缺失时明确阻塞且零写入。
- 单排产行为兼容，多排产继续阻塞。

## RED / GREEN / Verification

- RED: pending
- GREEN: pending
- Contract/regression: pending

## Observability

- 复用现有候选不可加入原因、服务业务异常和维护审计记录。

## Blockers

- 当前无阻塞。

