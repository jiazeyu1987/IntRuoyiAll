# Database Schema Evidence

## Data Change Goal And Affected Entities

建立统一 GxP 审计追踪第一阶段持久化结构，覆盖策略登记、只追加事件账本和 hash/完整性字段。

## Database Engine And Migration Tool

- Engine：MySQL，沿用 `IntRuoyiBackend/sql/mysql` 脚本管理。
- Migration tool：仓库 SQL 脚本 + 项目既有迁移验证脚本/静态合同测试。

## Schema, Migration, Fixture, Seed, Index, Or Constraint Changes

- 新增 `IntRuoyiBackend/sql/mysql/20260908_gxp_audit_trail_core.sql`。
- 新增 `gxp_audit_event`：统一审计事件只追加账本，包含 operationId、actor、serverOccurredAt、before/after 状态信封、策略版本、幂等键、签名引用、规范化事件 JSON、previousEventHash、eventHash 和 createTime。
- 新增 `gxp_audit_policy_version`：策略版本、策略 hash、批准依据、覆盖报告 hash。
- `gxp_audit_event` 不包含 `deleted`、`updater` 或 `update_time`，避免软删除和业务更新语义污染审计账本。

## Data Safety Analysis

- 本阶段只新增表/索引/约束，不修改或删除既有业务数据。
- 不执行生产数据库写入。

## Rollback Or Recovery Plan

- 新增对象必须可通过显式 drop 新表回滚；不得影响既有表。
- 若迁移验证发现既有命名冲突或引擎差异，停止并记录 blocker。

## BDD Scenarios

- BDD: 完整审计事件 -> Given 授权用户提交已登记的 GxP 业务写入；When 领域服务调用统一审计 append；Then 同一事务保存操作人、服务器时间、动作、原因、前后状态信封、对象版本、策略版本和 hash。
- BDD: 只追加审计账本 -> Given 审计事件已经写入；When 应用层代码尝试通过审计 Mapper 更新或删除事件；Then 编译期或静态合同测试失败，正式 Mapper 仅允许 insert/select。
- BDD: 策略登记强制 -> Given 新增 GxP operationId；When 策略登记缺少 owner、reasonPolicy、retentionClass 或 testIds；Then 策略合同测试失败并指出缺失字段。

## RED Command And Expected Failure

- RED: `mvn -pl yudao-module-signature '-Dtest=GxpAuditTrailSchemaContractTest,GxpAuditTrailServiceContractTest' test` -> FAIL，GxP 审计 API、DO、Mapper、Service 和 migration 尚不存在。

## GREEN Command And Passing Result

- GREEN: `mvn -pl yudao-module-signature -am '-Dtest=GxpAuditTrailSchemaContractTest,GxpAuditTrailServiceContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，5 tests passed。

## Migration Verification

- `GxpAuditTrailSchemaContractTest` 静态验证 migration 包含 `gxp_audit_event`、`gxp_audit_policy_version`、operationId、actor、serverOccurredAt、before/after 状态信封、event hash、previous hash、canonical JSON 和租户内幂等唯一键。
- 未执行真实数据库迁移；本阶段不写生产数据库。

## Blockers

- 正式数据库最小权限、WORM、NTP、归档恢复、首批业务写入口接入和生产 SOP 证据仍在后续阶段。
