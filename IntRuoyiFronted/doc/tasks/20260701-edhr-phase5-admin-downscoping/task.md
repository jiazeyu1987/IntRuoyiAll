# 任务：eDHR Phase 5 管理后台下沉（前端）

- Task ID: `20260701-edhr-phase5-admin-downscoping`
- Workspace: `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\yudao-ui-admin-vue3`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

让模板、权限、放行规则、审计查询等管理能力在批次详情页中明确成为“后台工作区入口”，不再和主流程动作混在一起。

## Previous Task Check

- 上一个前端任务：`20260701-edhr-phase4-audit-center`
- 状态：`completed`
- 处理说明：Phase 4 已把审计入口收口到批次详情，Phase 5 继续做前台主流程与后台工作区的分层。

## BDD 场景

- `BDD: 主流程与后台工作区视觉分层 -> Given 用户打开批次详情页 / When 查看页面头部和摘要区 / Then 能区分哪些是流程推进动作，哪些是后台配置/审计/模板工作区。`

## Milestones

1. M1：建立 Phase 5 前端任务台账并确认后台页范围。`completed`
2. M2：在批次详情页新增后台工作区分层与入口整理。`completed`
3. M3：补 frontend evidence。`completed`

## Expected Verification

- `pnpm --dir D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\yudao-ui-admin-vue3 ts:check`

## Current Blockers

- 暂无。

## Final Verification Result

- 批次详情页已把 `主流程动作` 与 `管理后台工作区` 明确分层。
- 模板、权限矩阵、表单工作区、记录簿等入口已被归入后台工作区，不再和主流程推进动作混在一起。
- `pnpm ts:check` 已通过。
- 真实运行态已验证页面可见 `管理后台工作区`，满足 Phase 5 管理后台下沉目标。
