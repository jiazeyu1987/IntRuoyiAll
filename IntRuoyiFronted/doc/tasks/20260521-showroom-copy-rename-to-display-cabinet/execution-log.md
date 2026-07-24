# 执行日志：展厅功能区文案统一改为展柜

BDD: showroom admin tabs and menu titles should use 展柜 -> Given 用户进入展厅功能区菜单与工作台 When 查看菜单页签和对应模块标题 Then 原本面向用户展示的“展厅”文案应统一显示为“展柜”

BDD: showroom admin content copy should use 展柜 consistently -> Given 用户查看展厅管理列表、编辑弹窗、映射弹窗、讲解工作台和版本工作台 When 页面渲染用户可见文案 Then 所有带“展厅”的可见内容应改为“展柜”，且不影响真实接口行为

BDD: internal implementation names stay unchanged -> Given 前端仍依赖现有 showroom 路由、组件路径与接口命名 When 本次只做用户可见文案替换 Then 不应改动内部 `showroom` 标识、路由路径或后端契约

RED: `node --test scripts\showroom-admin-copy-rename.test.mjs` -> FAIL, `src/router/modules/showroom.ts` 仍保留 `title: '展厅'` / `展厅公司` / `展厅管理`，且 `src/views/showroom-admin/**` 仍存在多处 `展厅` 文案

GREEN: `node --test scripts\showroom-admin-copy-rename.test.mjs scripts\showroom-admin-frontend.test.mjs scripts\showroom-admin-hall-list.test.mjs scripts\showroom-admin-product-hall-operability.test.mjs scripts\permission-hidden-shell-route-merge.test.mjs` -> PASS, 31 tests green，后台展柜文案与关联静态断言全部通过

GREEN: `pnpm exec eslint src/router/modules/showroom.ts src/views/showroom-admin/index.vue src/views/showroom-admin/components/HallListTable.vue src/views/showroom-admin/components/HallProductMappingDialog.vue src/views/showroom-admin/hall/HallWorkbench.vue src/views/showroom-admin/hall/HallEditorDialog.vue src/views/showroom-admin/dashboard/contracts.ts src/views/showroom-admin/dashboard/ShowroomDashboardWorkbench.vue src/views/showroom-admin/narration/NarrationWorkspace.vue src/views/showroom-admin/narration/NarrationWorkbench.vue src/views/showroom-admin/history/CompanyHistoryWorkbench.vue src/views/showroom-admin/approval/ApprovalTaskPanel.vue src/views/showroom-admin/approval/contracts.ts src/views/showroom-admin/narration/contracts.ts scripts/showroom-admin-copy-rename.test.mjs --format stylish` -> PASS

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-copy-rename-to-display-cabinet --mode preview` -> READY, 默认保留 `task.md` / `execution-log.md`，无附属产物需要清理
