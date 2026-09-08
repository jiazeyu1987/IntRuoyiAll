# GxP 审计追踪统一实施任务

## Task Goal

把 `20260907-gxp-audit-trail-unification` 的设计结论推进为可验证的软件实现第一阶段：建立统一 GxP 审计策略登记、只追加审计事件 schema、后端 append 服务契约、规范化 hash 和事务失败关闭测试基础。

本任务当前阶段只在本地代码仓库内实现和验证，不写入生产数据库、不操作远程环境、不执行 E2E。

## Milestones

- [x] M0：建立任务记录、BDD/TDD 证据文件和实施边界。
- [x] M1：完成现有模块、迁移和测试结构侦察，确认最小落点。
- [x] M2：先写 RED 测试，覆盖只追加 schema、策略登记和 append 服务失败关闭。
- [x] M3：实现最小统一审计内核、迁移脚本和策略登记样例。
- [x] M4：运行 GREEN 与回归验证，补充数据库和后端证据。
- [x] M5：按收尾规则清理、提交、推送，并记录最终结论。

## Expected Verification

- BDD 场景记录在 `execution-log.md`，且每个生产代码改动有 RED/GREEN 证据。
- 数据库 evidence 覆盖数据模型、迁移工具、schema 变更、安全分析、回滚计划和迁移验证。
- 后端 evidence 覆盖服务范围、数据契约、权限/校验/错误行为、迁移依赖、RED/GREEN 和可观测性。
- 新增审计事件表不得包含 `deleted`、业务更新 Mapper 或业务删除 Mapper。
- 审计 append 必须要求 operationId、actor、服务器时间、action、reason、before/after 状态信封、策略版本和 hash。
- 审计失败不得被吞掉；受控业务调用方必须能通过事务测试证明失败关闭。
- 不执行 E2E，除非用户后续当轮明确要求。

## Design Constraints Check

- 继承 `implementation-compliance-gate.md`：统一内部审计写入契约，不统一或代理全部业务 API。
- 不加入 fallback、异步补记、默认成功、兼容 shim 或 API 访问日志替代审计。
- 所有 GxP 成功写入最终必须在同一事务内调用统一 append；本阶段先交付内核和合同测试。
- 不写生产数据库；迁移脚本只进入仓库，验证通过脚本和测试完成。
- 不改动无关业务链路；首批领域接入若超出第一阶段则转后续里程碑。

## Current Status

completed：统一 GxP 审计内核第一阶段代码、SQL、策略登记、目标测试、evidence validator、经验沉淀、cleanup、实现提交和 closeout 推送均已完成。

## Cleanup Keep

- doc/tasks/20260908-gxp-audit-trail-implementation/task.md
- doc/tasks/20260908-gxp-audit-trail-implementation/execution-log.md
- doc/tasks/20260908-gxp-audit-trail-implementation/database-schema-evidence.md
- doc/tasks/20260908-gxp-audit-trail-implementation/backend-api-evidence.md
- doc/tasks/20260908-gxp-audit-trail-implementation/verification-report.md
