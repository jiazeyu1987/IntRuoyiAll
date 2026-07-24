# Execution Log: 展厅被指派产品可见范围与审批页签收口（前端）

BDD: 被指派编辑人只能在产品管理看到自己被分配的产品 -> Given 当前登录用户仅通过真实菜单进入 `展厅/产品管理` 且后端已按指派关系返回受限产品列表, When 前端渲染产品管理页, Then 页面只展示该用户被指派的产品且不暴露越权操作入口。

BDD: 审批中心菜单继续由真实菜单授权控制 -> Given 当前登录用户拥有或未拥有 `ShowroomAdminApproval` 对应菜单, When 前端合并静态展厅路由和动态菜单, Then 只有拥有 `审批中心` 菜单授权的用户才看到该页签。

GREEN: `node --test scripts/showroom-admin-product-assignee-scope.test.mjs` -> PASS

GREEN: `pnpm exec eslint src/views/showroom-admin/index.vue src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-assignee-scope.test.mjs` -> PASS

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260520-showroom-assignee-product-scope --mode preview` -> PASS

NOTE: `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-product-assignee-scope.test.mjs` -> FAIL-BLOCKED, 旧脚本 `showroom-admin-frontend.test.mjs` 仍断言 `productRowsPage` 变量名，属于当前工作区既有断言漂移，与本次受指派编辑人按钮收口改动无关。
