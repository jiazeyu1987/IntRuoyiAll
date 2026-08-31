# 20260831 修正 project_code 迁移依赖

## Task Goal

修正 `20260830_mes_batch_record_report_project_code` 的错误迁移依赖，使独立 schema 加列只依赖真实建表迁移，解除 code-only 测试服 target preflight 的 `BLOCKED_SCOPE_DEPENDENCY`。

## Milestones

1. 建立任务、经验门禁与 BDD/TDD 证据。
2. 写入失败回归测试并复现错误 dependsOn。
3. 最小修正迁移元数据。
4. 运行目标测试、迁移策略和 code-only target preflight 回归。
5. 提交任务变更并安全融合到 `int_main`。

## Expected Verification

- RED: 当前迁移元数据因依赖 `20260829_mes_old_form_template_binding_switch` 而失败。
- GREEN: 目标合同测试通过，dependsOn 精确为 `20260514_mes_batch_record_report`。
- 维护仓实际 migration policy gate 通过。
- 使用历史测试服 target state 生成 code-only preflight plan 时，`BLOCKED_SCOPE_DEPENDENCY=0`，目标 migration action=`APPLY`。
- Git diff 只包含目标 SQL 元数据、回归测试和任务记录。

## 经验门禁

- Trigger: code-only release、schema dependsOn data、BLOCKED_SCOPE_DEPENDENCY。
- Preflight check: 先证明目标 SQL 是否真实读取依赖数据，再确认真实表创建 migration 和测试服 APPLIED state。
- Blocker: 依赖语义不清、目标建表 migration 未应用、测试不能复现或会触发数据写入时停止。
- Verification: RED/GREEN、migration policy、target-bound preflight、diff/check、提交 allow-list。
- Forbidden action: 禁止执行无关 data migration、手工改 ledger、放宽 preflight、修改 DDL 正文或复用失败 releaseTag。
- Evidence: 上级发布任务 `IntRuoyiMaintance/doc/tasks/20260831-test-only-release-head` 与本任务日志。
- Experience index: 已命中 code-only required SQL、dependsOn、BLOCKED_SCOPE_DEPENDENCY、迁移策略门禁和 worktree 提交/融合门禁；禁止用 with-data 或手工台账绕过错误依赖。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。
- 是否存在临时补丁或绕过：否。

## Current Status

completed

## Milestone Status

1. 任务与门禁：completed。已读取根规则、backend/database/worktree/task-closeout、经验索引及命中发布经验；独立 worktree 已登记 slot 58（8313/48313），本任务不启动服务。
2. RED：completed。目标测试 1 failed/1 passed，精确命中错误 dependsOn。
3. 实现：completed。仅修改 SQL 首行 dependsOn 为真实建表 migration。
4. 回归：completed。目标测试 2/2、组合迁移测试 31/31、实际维护 gate 551 项、target-bound preflight status=passed/blocked=0/目标 action=APPLY。
5. 提交与融合：completed。实现提交 `7949cedc9`；合入并行主线提交后 branch head=`2c7bd07fa`，`int_main` 已 ff-only 到同一提交；主线回归 31/31、实际 gate 551 项和端口守卫通过。

## Closeout

- 修复 worktree 已从 Git 注册和物理路径清理。
- Slot 58 已标记 inactive，端口 8313/48313 无监听。
- 最终发布 `release-20260831-cell-link-idempotency-test-r260831c-r1` 已在测试服成功，原 scope dependency blocker 不再出现。
