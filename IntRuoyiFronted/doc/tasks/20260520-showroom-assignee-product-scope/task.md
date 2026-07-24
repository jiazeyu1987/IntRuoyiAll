# 任务：展厅被指派产品可见范围与审批页签收口（前端）

## Goal

让被指派填写产品详情的用户能够看到 `展厅 -> 产品管理` 菜单并进入产品管理页，但产品列表只能呈现指派给当前用户的产品；让有展厅产品审核权限的用户能够看到 `展厅 -> 审批中心` 菜单。前端继续遵循真实动态菜单，不引入 mock、fallback 或静默降级。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\components\ProductListTable.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-product-assignee-scope.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-assignee-product-scope\**`

## Non-Scope

- 不改动展厅前台路由与前台展示契约
- 不重做产品详情表单字段结构
- 不新增 fallback 菜单、兼容分支或 mock 返回
- 不在前端伪造权限判断替代后端真实数据范围控制

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-narration-move-to-basic-info\task.md`
- Status before this task: `Completed.`
- Impact: 无，可继续处理新的展厅产品可见范围任务。

## Milestones

- [x] M1: 创建前端任务文档并确认前端上一任务状态。
- [x] M2: 基于后端真实返回补前端定向校验，锁定“被指派用户不再看到越权操作”的可观察行为。
- [x] M3: 完成前端最小改动，适配被指派用户视角的产品管理页。
- [x] M4: 执行前端定向校验并记录 GREEN。
- [x] M5: 更新文档与交付说明，写明菜单授权配置要求。

## Expected Verification

- `node --test scripts/showroom-admin-product-assignee-scope.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/index.vue src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-assignee-scope.test.mjs`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260520-showroom-assignee-product-scope --mode preview`

## Current Status

Completed on 2026-05-20.

## Final Verification Result

- PASS: `node --test scripts/showroom-admin-product-assignee-scope.test.mjs`
- PASS: `pnpm exec eslint src/views/showroom-admin/index.vue src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-assignee-scope.test.mjs`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260520-showroom-assignee-product-scope --mode preview`

## Notes

- 前端仍完全依赖真实动态菜单；菜单授权配置仍需把 `980102` 分配给被指派填写产品详情的角色，把 `980104` 分配给审批角色。
- 已额外运行 `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-product-assignee-scope.test.mjs`，其中旧脚本 `showroom-admin-frontend.test.mjs` 因当前工作区既有断言仍期待 `productRowsPage` 变量名而失败；该漂移与本次“指派产品收口”改动无关，因此未在本任务内顺手改动旧脚本。
- 当前工作区同仓库存在多处未提交改动，且与本次涉及的 `index.vue`、`ProductListTable.vue` 同时处于脏状态；为避免混入无关变更，本次未自动执行 Git commit。
