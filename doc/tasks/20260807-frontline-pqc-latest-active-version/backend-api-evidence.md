# Backend API Evidence

## Scope

- 一线 PQC 活跃订单工序读取与待执行任务版本刷新服务。

## Contract

- 待执行 PQC 使用当前唯一 ACTIVE 路线版本、当前 routeProcess 和匹配 PUBLISHED QA 规程。
- 旧 PENDING 任务不得跨版本返回；已 SUBMITTED 任务保持冻结。

## Auth And Validation

- 沿用现有一线设备账号与 PQC 人员权限；本任务不放宽权限。
- 当前 ACTIVE 路线、发布规程、正式检验项目或任务刷新前置缺失时 fail fast。

## Data And Migrations

- 当前预计不修改 schema；如实现核对发现 schema 前置不足，立即阻塞并转入数据库变更流程。

## BDD

- 详见 `execution-log.md`。

## RED And GREEN

- RED：pending。
- GREEN：pending。

## Contract Verification

- pending。

## Observability

- 保留现有结构化业务错误；不吞异常、不返回默认成功。

## Blockers

- 当前无 blocker。

