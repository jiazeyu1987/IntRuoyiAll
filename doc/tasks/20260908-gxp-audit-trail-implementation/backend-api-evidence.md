# Backend API Evidence

## Endpoint, Service, Job, Or Handler Scope

第一阶段实现内部服务契约，不新增远程业务 API：`GxpAuditTrailService.append`、事件命令对象、状态信封、规范化 hash 和失败关闭异常。

## API Contract And Data Contract

- 所有 GxP 业务写入后续通过内部 append 服务登记审计事实。
- 远程 Controller 不得伪造正式操作人、正式时间或 before/after。
- append 命令必须包含 operationId、subject、action、reason、actor、before/after 状态信封、策略版本和业务引用。

## Auth, Permissions, Validation, And Error Behavior

- append 服务从服务端上下文接收稳定 actor，不接受客户端自报正式时间。
- 缺少必填字段、策略或状态信封时 fail fast。
- 审计写入失败必须抛出异常，交由同一事务回滚业务写入。

## Required Config, Services, Fixtures, And Migrations

- 依赖 `config/gxp-audit-policy.yaml` 和 `IntRuoyiBackend/sql/mysql/20260908_gxp_audit_trail_core.sql`。

## BDD Scenarios

- BDD: 完整审计事件 -> Given 授权用户提交已登记的 GxP 业务写入；When 领域服务调用统一审计 append；Then 同一事务保存操作人、服务器时间、动作、原因、前后状态信封、对象版本、策略版本和 hash。
- BDD: 审计失败关闭 -> Given 业务数据本可保存但审计 append 失败；When 调用方提交受控写入；Then 事务失败且业务数据不得单独成功。
- BDD: 策略登记强制 -> Given 新增 GxP operationId；When 策略登记缺少 owner、reasonPolicy、retentionClass 或 testIds；Then 策略合同测试失败并指出缺失字段。

## RED Command And Expected Failure

- RED: `mvn -pl yudao-module-signature '-Dtest=GxpAuditTrailSchemaContractTest,GxpAuditTrailServiceContractTest' test` -> FAIL，GxP 审计 API、DO、Mapper、Service 和 migration 尚不存在。

## GREEN Command And Passing Result

- GREEN: `mvn -pl yudao-module-signature -am '-Dtest=GxpAuditTrailSchemaContractTest,GxpAuditTrailServiceContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，5 tests passed。

## Contract Or Integration Verification

- `GxpAuditTrailServiceContractTest` 覆盖完整 append、缺少原因 fail fast、Mapper 插入失败原样抛出以支持事务回滚、禁止 `UNAVAILABLE/null` 状态信封。

## Observability Touchpoints

- 审计 append 失败使用 `GXP_AUDIT_COMMAND_INVALID` 或底层持久化异常暴露，不返回默认成功，不记录敏感明文。

## Blockers And Downstream Skill Needs

- 首批业务写入口接入、查询中心、周期审查、WORM/NTP 和运行态证据仍在后续阶段。
