# 任务：展厅一键封面 10 分钟后台续跑（前端）

## Goal

在 `展厅 -> 产品管理` 的 `一键封面` 入口保持现有交互不变的前提下，补齐对后端“后台续跑任务”返回值的前端承接：

- 首轮批量封面请求结束后，如果后端仍有未完成产品，前端必须明确提示“已开启后台定时检查，每 10 分钟自动续跑，全部完成后自动停止”；
- 批量结果弹窗必须展示任务编号、任务状态、剩余未完成数量和下一次检查时间；
- 如果后端检测到已有未完成后台任务并拒绝再次发起，前端必须原样暴露错误，不得吞掉或改成假成功。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\showroom-admin\index.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\index.vue`
- 与本次契约直接相关的 showroom 前端定向测试
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-batch-cover-auto-resume\**`

## Non-Scope

- 不新增任务管理页面。
- 不新增测试专用按钮、状态栏或 mock 数据。
- 不重做 `一键封面` 入口布局或模式选择交互。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-status-column\task.md`
- Status before this task: `Completed on 2026-05-22`
- Impact: 上一同仓任务已完成，不阻塞本次一键封面后台续跑交付。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: showroom 管理页相关文件存在未提交并行改动。
- Impact: 本任务只允许修改批量封面 API 类型、批量结果提示、定向测试与本任务文档，不能覆盖无关并行改动。

## Milestones

1. 创建任务文档并确认上一前端任务状态。
2. 先补 RED，锁定批量封面返回后台续跑元数据与提示文案的可观察行为。
3. 最小实现前端批量封面续跑提示与结果弹窗扩展。
4. 跑通定向测试、类型检查、证据校验与 closeout preview。
5. 按任务边界提交当前前端仓库改动。

## Expected Verification

- `node --test scripts/showroom-admin-batch-cover-auto-resume.test.mjs`
- `node --test scripts/showroom-admin-batch-cover-mode.test.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `pnpm exec eslint src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue scripts/showroom-admin-batch-cover-auto-resume.test.mjs --format stylish`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-batch-cover-auto-resume\frontend-feature-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-product-batch-cover-auto-resume --mode preview`

## Current Status

Completed and commit-boundary resolved on 2026-05-23.

## Completed Work

- 已扩展批量封面响应的前端承接，补齐 `taskId / taskStatus / remainingPendingCount / nextCheckAt`。
- 已在批量封面汇总弹窗中新增任务编号、任务状态、剩余未完成数量和下一次检查时间展示。
- 已在封面任务仍待续跑时统一提示“已开启后台定时检查，每 10 分钟自动续跑，全部完成后自动停止”。
- 已补齐前端定向回归、TS 检查、lint、evidence 校验与 closeout preview。

## Final Verification Result

- PASS: `node --test scripts/showroom-admin-batch-cover-auto-resume.test.mjs scripts/showroom-admin-batch-cover-mode.test.mjs`
- PASS: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- PASS: `node node_modules/eslint/bin/eslint.js src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue scripts/showroom-admin-batch-cover-auto-resume.test.mjs --format stylish`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-batch-cover-auto-resume\frontend-feature-evidence.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-product-batch-cover-auto-resume --mode preview`

## Commit Status

- 原始阻塞原因：`src/api/showroom-admin/index.ts`、`src/views/showroom-admin/index.vue` 与多份 showroom 相关测试文件当时混入并行任务未提交改动，无法安全切出独立 commit。
- 当前结果：2026-05-23 工作区已收敛到本任务的回归脚本与任务工件，可在不混入无关 showroom 改动的前提下补做 scoped 前端提交。
