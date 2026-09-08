# 20260908 GxP Audit Trail Implementation

## Task Goal

基于现有统一 GxP 审计追踪设计文档，创建独立实施任务，不继续混入前序文档任务。目标是先落地数据模型、统一审计核心、统一内部审计写入契约、首批高风险 GxP 链路接入、自动化测试与 CI 覆盖门禁，并明确运行态证据进入 `PASS FOR OPERATIONAL COMPLIANCE` 的前置条件。

## Milestones

1. in_progress - M1 数据模型与统一审计核心：建立统一审计账本表、策略登记表、覆盖登记表、hash/封存水位、只追加约束。
2. pending - M2 统一内部审计接口：所有 GxP 写操作调用统一审计写入契约；业务写入与审计写入同事务，审计失败则业务回滚。
3. pending - M3 首批 GxP 高风险链路接入：eDHR、DCC、电子签名、权限/角色配置、系统配置、发布/迁移变更。
4. pending - M4 测试与 CI 门禁：按设计 `test-plan.md`/BDD 场景补自动化测试，并加入“未登记写入口即失败”的覆盖检查。
5. pending - M5 运行态证据：NTP、WORM/Object Lock、备份恢复演练、周期审查 SOP、质量负责人签署、培训记录等只作为运行合规放行证据。

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

in_progress - 已创建独立实施任务，开始按 BDD/TDD 执行 M1/M2。
