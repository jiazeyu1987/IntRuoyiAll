# 20260908 GxP Audit Trail Implementation

## Task Goal

基于现有统一 GxP 审计追踪设计文档，创建独立实施任务，不继续混入前序文档任务。目标是先落地数据模型、统一审计核心、统一内部审计写入契约、首批高风险 GxP 链路接入、自动化测试与 CI 覆盖门禁，并明确运行态证据进入 `PASS FOR OPERATIONAL COMPLIANCE` 的前置条件。

## Milestones

1. completed - M1 数据模型与统一审计核心：已建立统一审计账本表、策略登记表、覆盖登记表、hash/封存水位、只追加约束。
2. completed - M2 统一内部审计接口：已实现 `GxpAuditService.append(GxpAuditCommand)` 内部写入契约、策略校验、原因/签名/before-after 校验、幂等载荷 hash 和事件 hash。
3. in_progress - M3 首批 GxP 高风险链路接入：已登记并注解 eDHR、DCC、电子签名、权限/角色配置、系统配置、发布/迁移变更；电子签名 `sign` 与 DCC 发布审批发起 `publishControlledFile` 已接入统一 `GxpAuditService.append` 并验证审计失败不返回成功；仍需继续逐业务方法接入完整 before/after。
4. completed - M4 测试与 CI 门禁：已补自动化测试、SQL 合同、覆盖检查脚本，并在 Maven CI 中加入“未登记写入口即失败”的覆盖检查。
5. blocked - M5 运行态证据：NTP、WORM/Object Lock、备份恢复演练、周期审查 SOP、质量负责人签署、培训记录等真实运行证据尚未提供，不能标记运行合规 PASS。

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

in_progress - M1/M2/M4 已完成；M3 已完成电子签名和 DCC 发布审批发起两个同事务统一审计 append 切片，剩余 eDHR、权限/角色配置、系统配置、发布/迁移变更仍需逐入口接入；M5 运行态证据仍 blocked，当前不得宣称 `PASS FOR OPERATIONAL COMPLIANCE`。
