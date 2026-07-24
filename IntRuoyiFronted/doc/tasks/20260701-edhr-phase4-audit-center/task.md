# 任务：eDHR Phase 4 审计中心收口（前端）

- Task ID: `20260701-edhr-phase4-audit-center`
- Workspace: `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\yudao-ui-admin-vue3`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

把操作审计、字段审计、域追溯统一收口为批次详情页里的审计中心视图，减少用户在多个审计专业页之间自行判断。

## Previous Task Check

- 上一个前端任务：`20260701-edhr-phase3-release-integration`
- 状态：`completed`

## 经验门禁

- 命中 `docs/powershell-memory.md`
- 命中 `docs/worktree-memory.md`
- 命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过批次详情页审计中心统一入口，而不是继续分散入口。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 审计中心统一呈现 -> Given 用户打开批次详情页 / When 查看审计摘要 / Then 能看到操作审计、字段审计、域追溯三个入口和摘要。`

## Milestones

1. M1：建立 Phase 4 前端任务台账。`completed`
2. M2：在批次详情页审计摘要补齐域追溯入口与摘要。`completed`
3. M3：补 frontend evidence。`completed`

## Expected Verification

- `pnpm --dir D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\yudao-ui-admin-vue3 ts:check`

## Current Blockers

- 暂无。

## Final Verification Result

- 批次详情页审计摘要已统一展示操作审计、字段审计与域追溯入口。
- `BatchExecutionTemplateSimulatePage.vue` 的 TEMPLATE 类型转换问题已修复。
- `pnpm ts:check` 已通过。