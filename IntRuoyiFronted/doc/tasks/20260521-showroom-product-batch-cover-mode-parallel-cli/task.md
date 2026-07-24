# 任务：展厅产品批量封面增加生成模式选择

## Goal

在 `展厅 -> 产品管理` 的 `一键生成所有封面` 入口中，点击后不再直接执行单一路径，而是先让用户明确选择：

- `重新生成所有`
- `只生成未上传的`

前端需要把所选模式传给后端，并保持当前真实列表筛选条件、生效权限、结果汇总弹窗和失败暴露行为不变。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\showroom-admin\index.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\components\ProductListTable.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-product-list.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-frontend.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-batch-cover-mode-parallel-cli\**`

## Non-Scope

- 不改动产品单条 `AI生成` 按钮行为。
- 不新增测试专用前端控件、mock 数据或 fallback 文案。
- 不重做产品管理页整体布局。
- 不修改与公司双语翻译任务直接相关的现有在途需求范围。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product002-cover-feedback\task.md`
- Status before this task: `Completed`
- Impact: 单产品封面生成点击反馈已收口；本次继续扩展批量封面入口的交互选择，不回退既有提示行为。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在与公司双语编辑相关的在途 API 类型和测试改动。
- Impact: 本任务只允许在目标文件上叠加批量封面模式选择相关改动，不能覆盖无关并行变更。

## Milestones

- [x] M1: 创建任务文档并确认上一同仓任务状态。
- [x] M2: 先补 RED，锁定“批量封面先选模式再发请求”的可观察前端契约。
- [x] M3: 完成前端模式选择、请求字段传递和汇总展示最小实现。
- [x] M4: 跑通定向源码验证并补齐执行日志与证据。
- [x] M5: 执行 closeout preview，并评估本仓提交边界。

## Expected Verification

- `node --test scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs`
- `node --test scripts/showroom-admin-batch-cover-mode.test.mjs`
- `pnpm exec eslint src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue scripts/showroom-admin-batch-cover-mode.test.mjs --format stylish`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-batch-cover-mode-parallel-cli\scripts\verify-batch-cover-mode-live-node.cjs`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-batch-cover-mode-parallel-cli\frontend-feature-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-batch-cover-mode-parallel-cli --mode preview`

## Current Status

- Status: Completed with commit-boundary blocker on 2026-05-21
- Completed work:
  - 已新增批量封面模式选择弹框，提供 `重新生成所有 / 只生成未上传的` 两个明确选项。
  - 已把 `coverGenerationMode` 连同现有筛选条件一起提交给后端。
  - 已在封面汇总弹窗中增加 `跳过已有封面` 展示。
  - 已用真实页面回放验证弹框出现，且关闭时不会误发真实批量封面请求。
- Remaining blockers:
  - `src/api/showroom-admin/index.ts` 当前同时承载公司字段翻译任务的在途改动，无法在不混入并行任务内容的前提下形成纯前端任务提交。

## Final Verification Result

- PASS: `node --test scripts/showroom-admin-batch-cover-mode.test.mjs`
- PASS: `pnpm exec eslint src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue scripts/showroom-admin-batch-cover-mode.test.mjs --format stylish`
- PASS: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-batch-cover-mode-parallel-cli\scripts\verify-batch-cover-mode-live-node.cjs`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-batch-cover-mode-parallel-cli\frontend-feature-evidence.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-batch-cover-mode-parallel-cli --mode preview`
