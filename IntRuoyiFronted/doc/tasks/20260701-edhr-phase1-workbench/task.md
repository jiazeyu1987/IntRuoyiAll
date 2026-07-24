# 任务：eDHR Phase 1 批次总控页收口（前端）

- Task ID: `20260701-edhr-phase1-workbench`
- Workspace: `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\yudao-ui-admin-vue3`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

把 `BatchExecutionDetailPage.vue` 升级为 Phase 1 的批次总控页，统一展示阶段、阻塞项、任务摘要、放行摘要、审计摘要和下一步动作入口。

## Previous Task Check

- 当前 worktree 最近前端任务文档与 eDHR Phase 目标无直接关联，本轮在隔离 worktree 内建立新的正式任务台账。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
  - 命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 本次在现有 eDHR 样式语言上做结构收口，不引入无关重设计。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过总控页收口而不是继续散落多个平级入口。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 批次详情成为主流程页 -> Given 用户打开批次详情 / When 页面加载成功 / Then 顶部能直接看到当前阶段、阻塞项、放行摘要、审计摘要和下一步动作。`
- `BDD: 子流程入口统一回到批次详情上下文 -> Given 用户从批次详情进入执行、放行或审计相关入口 / When 返回时 / Then 能持续围绕同一批次上下文操作，而不是丢回分散列表。`

## Milestones

1. M1：建立前端任务台账并确认总控页现有结构。`completed`
2. M2：新增 workbench API 与页面结构收口。`completed`
3. M3：补 frontend evidence。`completed`

## Expected Verification

- `pnpm --dir D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\yudao-ui-admin-vue3 ts:check`

## Current Blockers

- 暂无。`BatchExecutionTemplateSimulatePage.vue` 类型问题已在本轮一并修复，`ts:check` 已通过。

## Final Verification Result

- 已在批次详情页接入 workbench 总控摘要区。
- 已将批次级阻塞项、放行摘要、审计摘要前置到详情页头部区域。
- 详情页已成为批次主流程总控页，保留任务表格作为同一页面内的执行明细区。
- 真实运行态已验证批次详情页展示 `批次总控 / 阶段摘要 / 放行 / 审计 / 管理后台`。
