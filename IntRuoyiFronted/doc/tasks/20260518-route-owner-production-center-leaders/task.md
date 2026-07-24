# Task: 工艺路线负责人候选选择

## Goal

在 MES 工艺路线编辑表单中，将“负责人”改为可手动输入并带候选下拉的输入框；候选人来自当前组织树中 `瑛泰医疗 / 生产制造中心` 下各级部门的负责人，用户可直接选择，也可继续手动输入自由文本。

## Scope

- 先显式阻塞上一条同仓库未收尾任务，再创建本任务文档。
- 严格按 BDD + TDD 先补 RED 验证，再做最小实现。
- 仅修改工艺路线编辑表单、候选构建逻辑、必要的前端验证脚本和任务证据。
- 保持现有后端工艺路线保存接口不变，不新增后端字段，不引入 fallback 数据。
- 候选范围基于真实前端 API `system/dept/list` 与 `system/user/simple-list` 组合构建。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260518-schedule-calendar-shortage-risk-daily-material-summary/task.md`
- Status before this task: blocked by user priority switch.
- Impact: previous task has been explicitly paused and does not block this route-owner autocomplete task.

## Milestones

- [x] M1: Block the previous same-repository frontend task and create this task package first.
- [x] M2: Add RED verification for production-center leader suggestions.
- [x] M3: Implement the smallest owner autocomplete flow.
- [x] M4: Run targeted verification and update evidence.
- [x] M5: Commit only task-scoped files after required verification passes.

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-route-owner-production-center-leaders\scripts\verify-route-owner-production-center-leaders.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`
- `http://localhost:8081`

## Current Status

Completed. Frontend code, static verification, real Playwright verification, and cleanup preview are complete; the route-owner candidate flow is now commit-safe.

## Final Verification Result

- PASS: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-route-owner-production-center-leaders\scripts\verify-route-owner-production-center-leaders.mjs`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session route-owner-production-center-leaders run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-route-owner-production-center-leaders\scripts\verify-route-owner-production-center-leaders-e2e.mjs`
- PASS: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260518-route-owner-production-center-leaders --mode preview`

## Blocker And Impact

- None currently.
