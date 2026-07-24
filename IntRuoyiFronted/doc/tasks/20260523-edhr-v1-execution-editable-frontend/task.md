# 任务：eDHR V1 执行页最小可编辑闭环前端

## Goal

在现有只读 eDHR 前端基础上，补齐最小可编辑闭环：执行详情页基于 `executionSnapshotJson.fields` 渲染最小表单，接入 `save-draft` 与 `submit(password, comment)`，保存后可回显 `cellValues`，提交成功后切换只读，并为页面/提交流程补最小 RED->GREEN 静态测试。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\yudao-ui-admin-vue3\src\api\mes\pro\feedback\index.ts`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\yudao-ui-admin-vue3\src\views\mes\pro\edhr\**`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\yudao-ui-admin-vue3\src\router\modules\remaining.ts`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\yudao-ui-admin-vue3\scripts\edhr-execution-page.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\yudao-ui-admin-vue3\scripts\edhr-execution-submit.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\yudao-ui-admin-vue3\doc\tasks\20260523-edhr-v1-execution-editable-frontend\**`

## Non-Scope

- 不修改后端仓
- 不改变 `FeedbackForm` 为首入口的约束
- 不增加 fallback、mock 成功或隐藏错误
- 不补与当前最小执行闭环无关的复杂布局编辑器

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\yudao-ui-admin-vue3\doc\tasks\20260523-edhr-v1-feedback-entry-green\task.md`
- Status before this task: `Completed on 2026-05-23 for owned frontend scope`
- Impact: 上一任务已完成入口、列表/详情路由和只读基础，本任务在其上继续补编辑闭环

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\yudao-ui-admin-vue3`
- Current state at start: 存在本轮前端范围内未提交改动与任务文档
- Impact: 仅在 eDHR 前端相关文件内增量修改，不回滚他人改动

## Milestones

- [x] M1: 创建新任务文档并复核现有前端/后端契约
- [x] M2: 新增 `edhr-execution-page` / `edhr-execution-submit` RED 测试并记录失败证据
- [x] M3: 实现最小字段表单渲染、草稿保存与回显
- [x] M4: 实现提交密码弹窗、提交后只读与页面回刷
- [x] M5: 运行最小前端测试与 eslint，记录 GREEN 结果与剩余风险

## Expected Verification

- `node --test scripts\\edhr-v1-feedback-entry.test.mjs`
- `node --test scripts\\edhr-execution-page.test.mjs`
- `node --test scripts\\edhr-execution-submit.test.mjs`
- `node node_modules/eslint/bin/eslint.js src/api/mes/pro/feedback/index.ts src/views/mes/pro/edhr/ExecutionListPage.vue src/views/mes/pro/edhr/ExecutionPage.vue src/views/mes/pro/edhr/ExecutionRenderer.vue src/router/modules/remaining.ts scripts/edhr-execution-page.test.mjs scripts/edhr-execution-submit.test.mjs`

## Current Status

Completed on 2026-05-23 for owned frontend scope. 已将只读 eDHR 执行详情补成最小可编辑闭环：基于 `executionSnapshotJson.fields` 渲染字段表单、接入 `save-draft` 与 `submit(password, comment)`、保存后回显 `cellValues`、提交成功后切只读，并补齐页面/提交 RED->GREEN 静态测试。

## Blockers And Impact

- Blocker: 若联调返回的 `executionSnapshotJson` 不包含 `fields` 数组，前端会按 fail-fast 明确报错，无法伪造字段表单
- Impact: 当前实现依赖运行态快照具备可编辑字段元数据；若后端/报表 JSON 只返回原始布局而没有 `fields`，页面只能暴露错误，不能进入编辑闭环
