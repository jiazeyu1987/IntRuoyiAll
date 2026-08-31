# 20260831 修正批记录单元格链接迁移幂等性

## Task Goal

将 `20260726_mes_batch_record_cell_link_work_order_source` 从裸多列 ADD 改为逐列幂等 migration，使目标 schema 已存在部分或全部列时可安全重放，并继续由后续正式 migration 负责字段宽度升级。

## Milestones

1. 记录 BDD、经验门禁、真实 schema 与失败 operation。
2. 新增失败静态合同，复现无 information_schema 守卫。
3. 最小改造为逐列幂等过程。
4. 运行目标/相邻 release 回归、实际 migration gate 和 target-bound plan。
5. 提交、融合并在主线复验。

## Expected Verification

- RED: 当前 SQL 缺逐列存在性守卫且包含单次裸三列 ADD。
- GREEN: 三列分别在缺失时添加，表缺失 fail fast，DDL 定义保持不变。
- 后续 structured source widths migration 的独立职责保持。
- 实际 migration policy gate 通过，target-bound code-only plan 无 blocker。
- Git 只包含目标 SQL、回归测试和任务记录。

## 经验门禁

- Trigger: ERROR 1060、列已存在但 migration ledger 未 APPLIED、required SQL 重放。
- Preflight check: 真实 DESCRIBE/INFORMATION_SCHEMA、migration ledger、后续 migration 职责。
- Blocker: 表/列合同不清、需要手工改 ledger、DDL 语义变化或数据回填时停止。
- Verification: RED/GREEN、相邻迁移测试、实际 gate、target plan、主线复验。
- Forbidden action: 禁止吞 ERROR 1060、使用 `ADD COLUMN IF NOT EXISTS` 假定版本兼容、手工标记 APPLIED 或跳过 migration。
- Evidence: 上级发布任务 `IntRuoyiMaintance/doc/tasks/20260831-test-only-release-head`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。
- 是否存在临时补丁或绕过：否。

## Current Status

in_progress

## Milestone Status

1. 门禁与证据：completed。
2. RED：completed。目标合同 1 failed/1 passed，精确捕获无逐列守卫。
3. 实现：completed。改为表级 fail-fast + 三列独立存在性检查/ALTER，DDL 定义不变。
4. 回归：completed。目标 2/2、release 组合 46/46、实际 migration gate 551 项、target-bound plan passed/blocked=0/目标 action=APPLY。
5. 提交融合：in_progress。
