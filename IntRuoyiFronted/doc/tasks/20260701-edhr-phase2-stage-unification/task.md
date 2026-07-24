# 任务：eDHR Phase 2 批次统一状态机（前端）

- Task ID: `20260701-edhr-phase2-stage-unification`
- Workspace: `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\yudao-ui-admin-vue3`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

让列表页和详情页统一消费后端批次阶段摘要，逐步淘汰前端本地对批次主阶段的独立解释。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\yudao-ui-admin-vue3\doc\tasks\20260701-edhr-phase1-workbench\task.md`
- 状态：`completed`
- 处理说明：Phase 1 已在详情页接入 workbench 摘要，Phase 2 继续推动列表页和详情页共享同一阶段表达。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
  - 命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。让页面共享阶段摘要，而不是列表页和详情页各自解释状态码。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 列表页显示统一阶段标签 -> Given 用户浏览批次执行列表 / When 列表加载 / Then 每条批次除原始状态外，还能看到统一主阶段标签或摘要。`
- `BDD: 详情页与列表页阶段表达一致 -> Given 用户从列表进入详情 / When 查看同一批次 / Then 阶段名称与责任角色不产生冲突。`

## Milestones

1. M1：建立 Phase 2 前端任务台账并确认列表页现有状态展示。`completed`
2. M2：列表页接入统一阶段摘要。`completed`
3. M3：补 frontend evidence。`completed`

## Expected Verification

- `pnpm --dir D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\yudao-ui-admin-vue3 ts:check`

## Current Blockers

- 暂无。`BatchExecutionTemplateSimulatePage.vue` 类型问题已在本轮后续阶段修复，最终 `ts:check` 已通过。

## Final Verification Result

- 列表页与详情页已共享同一批次阶段表达。
- 前端本地 `resolveBatchMainStageLabel(...)` 已退居兜底，优先显示后端统一阶段标签。
- 最终 `pnpm ts:check` 已通过。
