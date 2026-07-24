# 任务：eDHR Phase 3 放行衔接（前端）

- Task ID: `20260701-edhr-phase3-release-integration`
- Workspace: `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\yudao-ui-admin-vue3`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

把放行从“独立放行后台列表页”进一步收口到批次详情页，至少让用户可以在批次详情里看到放行阶段状态、直接执行预检并进入该批次的放行事务明细。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\yudao-ui-admin-vue3\doc\tasks\20260701-edhr-phase2-stage-unification\task.md`
- 状态：`completed`
- 处理说明：Phase 2 已统一批次阶段表达，Phase 3 基于该表达进一步收口放行入口。

## BDD 场景

- `BDD: 批次详情内可完成放行预检 -> Given 用户在批次详情页看到放行摘要 / When 点击执行预检 / Then 当前页直接刷新放行状态和检查摘要。`
- `BDD: 放行事务仍以批次为上下文查看 -> Given 用户从批次详情进入放行检查项或事务事件 / When 跳转 / Then 页面直接聚焦当前批次对应的放行事务。`

## Milestones

1. M1：建立 Phase 3 前端任务台账并确认当前放行衔接缺口。`completed`
2. M2：在批次详情页收口放行预检与事务入口。`completed`
3. M3：补 frontend evidence。`completed`

## Expected Verification

- `pnpm --dir D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\yudao-ui-admin-vue3 ts:check`

## Current Blockers

- 暂无。`BatchExecutionTemplateSimulatePage.vue` 的 `recordCategory: "TEMPLATE"` 类型问题已在本轮后续阶段修复。

## Final Verification Result

- 批次详情页中的放行摘要已不再只是静态展示。
- 用户现在可在批次上下文中直接触发放行预检，并查看该批次对应的检查项与事务事件。
- 已将提交放行、批准放行、驳回放行、撤回放行的对话框与动作入口嵌入批次详情页，使放行从独立后台列表进一步转为批次流程正式阶段。
- 批次详情页新增的放行事务动作已通过 `ts:check` 验证。
