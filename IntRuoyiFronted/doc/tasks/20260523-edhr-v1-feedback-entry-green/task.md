# 任务：eDHR V1 FeedbackForm 首入口 GREEN 实现

## Goal

在已放行 RED 测试基础上，仅在前端范围内为 eDHR V1 执行节点补齐最小 GREEN 实现：从 `FeedbackForm` 提供 `打开 eDHR` 入口，接通专用入口 API、隐藏执行路由、执行页与渲染器，并保持缺失前置条件时 fail-fast。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\yudao-ui-admin-vue3\src\api\mes\pro\feedback\index.ts`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\yudao-ui-admin-vue3\src\views\mes\pro\feedback\FeedbackForm.vue`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\yudao-ui-admin-vue3\src\router\modules\remaining.ts`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\yudao-ui-admin-vue3\src\views\mes\pro\edhr\**`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\yudao-ui-admin-vue3\scripts\edhr-v1-feedback-entry.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\yudao-ui-admin-vue3\doc\tasks\20260523-edhr-v1-feedback-entry-green\**`

## Non-Scope

- 不修改后端仓或后端接口实现
- 不把入口切换到 `WorkOrderForm2`
- 不引入 fallback、mock 成功或静默降级
- 不处理与 eDHR V1 FeedbackForm 首入口无关的其它页面重构

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\yudao-ui-admin-vue3\doc\tasks\20260523-edhr-v1-feedback-entry-red\task.md`
- Status before this task: `Completed on 2026-05-23 for RED scope`
- Impact: 上一任务已完成 RED 测试与约束锁定，不阻塞本次 GREEN 实现

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\yudao-ui-admin-vue3`
- Current state at start: 存在未提交的 RED 文档与测试脚本输入；本任务仅在指定前端文件范围内增量修改，不回滚他人改动
- Impact: 需要避免覆盖并行工作，仅实现当前 RED 对应前端能力

## Milestones

- [x] M1: 核对上一任务完成状态并创建当前任务文档
- [x] M2: 复现 RED 并记录失败证据
- [x] M3: 实现 eDHR 专用入口 API、FeedbackForm 入口与上下文组织
- [x] M4: 实现隐藏路由、执行页与 `ExecutionRenderer`，只消费 `executionSnapshotJson`
- [x] M5: 运行最小 GREEN / 回归测试与 eslint，并记录结果

## Expected Verification

- `node --test scripts\\edhr-v1-feedback-entry.test.mjs`
- `pnpm install --frozen-lockfile`
- `node node_modules/eslint/bin/eslint.js src/api/mes/pro/feedback/index.ts src/views/mes/pro/feedback/FeedbackForm.vue src/router/modules/remaining.ts src/views/mes/pro/edhr/ExecutionPage.vue src/views/mes/pro/edhr/ExecutionRenderer.vue`
- `pnpm ts:check`

## Current Status

Completed on 2026-05-23 for owned frontend scope. 已补齐 `FeedbackForm` 的 `打开 eDHR` 入口、专用入口 API、隐藏列表/详情路由、执行列表页、执行详情页与 `ExecutionRenderer`；详情主摘要已优先切换到 route/process/workstation/report 语义，模板字段仅保留为兼容信息。定向脚本与本地 ESLint 已通过；全量 `pnpm ts:check` 因 Node 堆内存耗尽未能完成。

## Blockers And Impact

- Blocker: `pnpm ts:check` 在本仓当前环境下两次均因 Node 堆内存耗尽失败（默认与 `NODE_OPTIONS=--max-old-space-size=8192` 均复现）
- Impact: 当前无法给出全仓类型检查 GREEN 结论，但这不是已观察到的本次前端文件 ESLint 或定向 RED/GREEN 回归失败；若需全仓类型验证，需要进一步拆分检查范围或提高机器可用内存
