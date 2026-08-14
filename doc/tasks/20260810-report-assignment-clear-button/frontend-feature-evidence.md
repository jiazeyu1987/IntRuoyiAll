# Feature

Goal: 在“分配报工 / 活跃订单分配”表格每一行增加“清除”按钮，点击后只将当前行分配数量设为 0。

Non-goals:

- 不改变活跃订单候选来源。
- 不改变 FIFO 自动分配接口、确认分配接口或 leaderType 上下文来源。
- 不新增后端接口或降级逻辑。

Owned files:

- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue
- IntRuoyiFronted/tests/e2e/team-leader-report-allocation-clear-static.spec.cjs

# Acceptance

- 每个可编辑分配行展示“清除”按钮。
- 点击“清除”后，当前行 allocatedQuantity 变为 0，其它行不变。
- 清零后汇总使用现有 allocationRows 计算链路自动更新。
- 正式提交构造逻辑把 0 数量行从 allocations payload 中移除，避免向后端提交无效 0 分配明细。
- 确认分配 leaderType 继续通过 resolveCurrentLeaderType() 从 activeLeaderTab 获取。

# BDD

- BDD: 行级清除分配数量 -> Given 分配报工弹窗存在多行活跃订单且某行分配数量为非零 When 用户点击该行清除按钮 Then 仅该行分配数量变为 0 且其它行输入不被改动
- BDD: 清除动作参与现有汇总 -> Given 某行分配数量被清除为 0 When 弹窗重新计算已分配和未分配 Then 汇总使用清零后的正式输入值

# RED

- RED: workdir=IntRuoyiFronted; node tests/e2e/team-leader-report-allocation-clear-static.spec.cjs -> FAIL，AssertionError: allocation rows must expose a row-level 清除 button wired to the clear handler.

# GREEN

- GREEN: workdir=IntRuoyiFronted; node tests/e2e/team-leader-report-allocation-clear-static.spec.cjs -> PASS
- GREEN: workdir=IntRuoyiFronted; node tests/e2e/team-leader-report-allocation-static.spec.cjs -> PASS
- GREEN: workdir=IntRuoyiFronted; node tests/e2e/team-leader-report-allocation-dialog-hide-static.spec.cjs -> PASS
- GREEN: workdir=IntRuoyiFronted; pnpm ts:check -> PASS

# Verification

- 新增静态合同锁定 data-team-leader-allocation-clear、clearAllocationQuantity(row) 和 0 数量行提交过滤。
- 既有分配静态合同通过，证明 FIFO 预览、最大/一半快捷分配、0/空数量提交处理和 leaderType 上下文来源仍满足合同。
- 既有弹窗隐藏静态合同通过，证明分配报工模式没有引入复核签名等内部字段。

# Blockers

- 无。
