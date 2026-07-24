# 任务：展厅产品列表“生命周期”列改名为“获证状态”

## Goal

将 `展厅 -> 产品管理` 列表中的表头文案从 `生命周期` 调整为 `获证状态`，不改动字段值、筛选逻辑、数据契约或其他业务行为。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\components\ProductListTable.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-product-list.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-lifecycle-column-rename\**`

## Non-Scope

- 不改动展厅产品列表的筛选项占位文案。
- 不改动生命周期字段对应的枚举值、标签样式或后端接口字段。
- 不引入 fallback、兼容分支、mock 数据或静默降级。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-frontend-gitignore-cleanup\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 无，可继续处理本次展厅产品列表文案调整。

## Milestones

- [x] M1: 确认上一同仓任务状态并创建当前任务文档。
- [x] M2: 记录 BDD 场景并先修改测试形成 RED。
- [x] M3: 更新产品列表表头文案实现最小改动。
- [x] M4: 运行定向验证并记录 GREEN。
- [x] M5: 更新任务文档、执行 closeout preview，并按任务边界提交。

## Expected Verification

- `node --test scripts/showroom-admin-product-list.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-list.test.mjs --format stylish`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-lifecycle-column-rename --mode preview`

## Current Status

Completed on 2026-05-21.

已将 `展厅 -> 产品管理` 列表中的 `生命周期` 表头改为 `获证状态`；筛选占位文案、字段值和业务逻辑保持不变。为保证定向验证可用，同步补齐了 `showroom-admin-product-list.test.mjs` 中与当前组件契约对齐的 `editable` 测试样例字段。

## Blockers And Impact

- Blocker: none.
- Impact: none.

## Final Verification Result

- `node --test scripts/showroom-admin-product-list.test.mjs` -> PASS，10 tests green。
- `pnpm exec eslint src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-list.test.mjs --format stylish` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-lifecycle-column-rename --mode preview` -> PASS，预览结果仅保留 `task.md` 与 `execution-log.md`，无删除项。
