# 任务：展厅功能区文案统一改为展柜

## Goal

在 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` 中，将展厅功能区前端用户可见文案里带“展厅”的内容统一改为“展柜”，覆盖菜单页签、功能页签、列表列名、弹窗标题、按钮文案和页内说明文字，保持现有功能路径与接口契约不变。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\router\modules\showroom.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\**`
- 与本次文案变更直接相关的 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-copy-rename-to-display-cabinet\**`

## Non-Scope

- 不修改后端接口、数据库字段、表名或业务枚举中的 `showroom` 命名。
- 不改动 `showroom-frontstage` 前台公开浏览路径中的无关文案。
- 不顺带调整首页“数字展厅入口”卡片、测试文档历史记录或其他模块 copy。
- 不引入 fallback 文案、兼容别名或静默降级逻辑。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-frontend-gitignore-cleanup\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 无，可继续处理展厅功能区前端文案统一。

## Scan Conclusion

- `src\router\modules\showroom.ts` 中存在菜单标题与工作台标题文案：`展厅`、`展厅公司`、`展厅管理`。
- `src\views\showroom-admin\**` 中存在页签、列名、弹窗标题、按钮和提示语等大量用户可见 `展厅` 文案。
- 本次按用户要求仅收口展厅功能区可见 copy，保持 `showroom` 路由、文件路径与内部实现名不变。

## Milestones

- [x] M1: 检查上一同仓任务状态并建立当前任务记录。
- [x] M2: 先补 RED 测试，锁定展厅功能区可见文案替换范围。
- [x] M3: 实施最小前端文案变更并同步更新对应断言。
- [x] M4: 运行定向验证并确认 GREEN。
- [x] M5: 回写任务文档与执行日志，记录最终结果。

## Expected Verification

- `node --test scripts\showroom-admin-copy-rename.test.mjs scripts\showroom-admin-frontend.test.mjs scripts\showroom-admin-hall-list.test.mjs scripts\showroom-admin-product-hall-operability.test.mjs scripts\permission-hidden-shell-route-merge.test.mjs`
- `pnpm exec eslint src/router/modules/showroom.ts src/views/showroom-admin/index.vue src/views/showroom-admin/components/HallListTable.vue src/views/showroom-admin/components/HallProductMappingDialog.vue src/views/showroom-admin/hall/HallWorkbench.vue src/views/showroom-admin/hall/HallEditorDialog.vue src/views/showroom-admin/dashboard/contracts.ts src/views/showroom-admin/dashboard/ShowroomDashboardWorkbench.vue src/views/showroom-admin/narration/NarrationWorkspace.vue src/views/showroom-admin/narration/NarrationWorkbench.vue src/views/showroom-admin/history/CompanyHistoryWorkbench.vue src/views/showroom-admin/approval/ApprovalTaskPanel.vue src/views/showroom-admin/approval/contracts.ts src/views/showroom-admin/narration/contracts.ts scripts/showroom-admin-copy-rename.test.mjs --format stylish`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-copy-rename-to-display-cabinet --mode preview`

## Current Status

Completed on 2026-05-21.

已将后台展厅功能区菜单标题、页签文案、列表列名、弹窗标题、成功提示和工作台说明中的“展厅”统一改为“展柜”，并同步更新相关静态测试断言；前台公开浏览页文案保持不变。

## Blockers And Impact

- Blocker: none.
- Impact: none.

## Final Verification Result

- `node --test scripts\showroom-admin-copy-rename.test.mjs scripts\showroom-admin-frontend.test.mjs scripts\showroom-admin-hall-list.test.mjs scripts\showroom-admin-product-hall-operability.test.mjs scripts\permission-hidden-shell-route-merge.test.mjs` -> PASS，31 tests green，覆盖菜单标题、后台工作台文案和受影响静态断言。
- `pnpm exec eslint src/router/modules/showroom.ts src/views/showroom-admin/index.vue src/views/showroom-admin/components/HallListTable.vue src/views/showroom-admin/components/HallProductMappingDialog.vue src/views/showroom-admin/hall/HallWorkbench.vue src/views/showroom-admin/hall/HallEditorDialog.vue src/views/showroom-admin/dashboard/contracts.ts src/views/showroom-admin/dashboard/ShowroomDashboardWorkbench.vue src/views/showroom-admin/narration/NarrationWorkspace.vue src/views/showroom-admin/narration/NarrationWorkbench.vue src/views/showroom-admin/history/CompanyHistoryWorkbench.vue src/views/showroom-admin/approval/ApprovalTaskPanel.vue src/views/showroom-admin/approval/contracts.ts src/views/showroom-admin/narration/contracts.ts scripts/showroom-admin-copy-rename.test.mjs --format stylish` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-copy-rename-to-display-cabinet --mode preview` -> READY，仅保留 `task.md` 与 `execution-log.md`，无删除项。
