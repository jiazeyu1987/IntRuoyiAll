# 20260908 GxP Audit Trail Implementation

## Task Goal

基于现有统一 GxP 审计追踪设计文档，创建独立实施任务，不继续混入前序文档任务。目标是先落地数据模型、统一审计核心、统一内部审计写入契约、首批高风险 GxP 链路接入、自动化测试与 CI 覆盖门禁，并明确运行态证据进入 `PASS FOR OPERATIONAL COMPLIANCE` 的前置条件。

## Milestones

1. completed - M1 数据模型与统一审计核心：已建立统一审计账本表、策略登记表、覆盖登记表、hash/封存水位、只追加约束。
2. completed - M2 统一内部审计接口：已实现 `GxpAuditService.append(GxpAuditCommand)` 内部写入契约、策略校验、原因/签名/before-after 校验、幂等载荷 hash 和事件 hash。
3. completed - M3 首批 GxP 高风险链路接入：已登记并注解 eDHR、DCC、电子签名、权限/角色配置、系统配置、发布/迁移变更；电子签名 `sign`、DCC 发布审批发起 `publishControlledFile`、权限/角色配置三处 assign 入口、系统配置包导入、eDHR 字段保存已接入统一 `GxpAuditService.append` 并验证审计失败不返回成功；发布/迁移变更作为 MIGRATION source，需在 M5 运行态 release evidence/QA 签署中闭环。
4. completed - M4 测试与 CI 门禁：已补自动化测试、SQL 合同、覆盖检查脚本，并在 Maven CI 中加入“未登记写入口即失败”的覆盖检查。
5. blocked - M5 运行态证据：已新增 M5 证据矩阵、周期审查 SOP 草案、签署/培训模板，并采集本机只读时间同步与后端健康证据；正式 NTP、WORM/Object Lock、备份恢复演练、周期审查实际执行、质量负责人签署、培训记录、数据库权限分离和特权审计外送真实证据尚未提供，不能标记运行合规 PASS。

## Expected Verification

- 静态结构验证：新增任务文档、BDD/TDD 记录、数据库证据、后端证据完整。
- 数据库验证：migration/SQL 合同覆盖审计核心表、索引、唯一约束、只追加保护、hash/封存水位字段。
- 后端验证：统一审计 append 契约单元/集成测试覆盖成功 append、策略缺失失败、原因缺失失败、同事务回滚、幂等冲突、hash 链冲突。
- 覆盖门禁：策略登记与实际写入口双向一致；新增未登记 GxP 写入口测试失败。
- CI 验证：新增测试命令或脚本可在 CI 中 fail fast，不使用 mock 成功替代合规证据。

## Design Constraints Check

- 不把业务接口统一成一个接口；仅统一 GxP 审计写入契约。
- 不用访问日志、普通技术日志、异步队列、after-commit 补记或默认成功作为 GxP 审计证据。
- 审计成功事件与业务写入必须同 Spring 事务、同物理数据库事务。
- 审计缺策略、缺原因、缺 before/after、缺签名、append 失败时业务必须失败并回滚。
- 运行态 NTP、WORM、备份恢复、SOP、签署、培训缺证据时只能标记 BLOCKED，不能宣称运行合规 PASS。

## Current Status

blocked - M1/M2/M3/M4 代码侧已完成；本轮已补充 M5 运行态证据包、周期审查 SOP 草案、签署/培训模板，并记录本机只读 NTP 与后端健康证据。正式环境 NTP/WORM/备份恢复/周期审查执行/QA 签署/培训/DB 权限分离/特权审计外送证据未提供前，当前不得宣称 `PASS FOR OPERATIONAL COMPLIANCE`。

## Cleanup Keep

- doc/tasks/20260908-gxp-audit-trail-implementation/m5-operational-compliance-evidence.md
- doc/tasks/20260908-gxp-audit-trail-implementation/m5-periodic-review-sop.md
- doc/tasks/20260908-gxp-audit-trail-implementation/m5-signoff-training-record.md
