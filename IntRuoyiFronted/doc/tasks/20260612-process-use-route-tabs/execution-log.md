# 20260612 工艺用途路线页签前端执行日志

BDD: 工艺排产路线前端页签 -> Given 用户拥有 `mes:pro-schedule-route:query` 权限 / When 用户打开 `/mes/pro/schedule-route` / Then 页面展示路线列表和 SCHEDULE 用途配置入口，不展示原始路线新增、编辑、删除或导入按钮。

BDD: 工艺批记录路线前端页签 -> Given 用户拥有 `mes:pro-batch-record-route:query` 权限 / When 用户打开 `/mes/pro/feedback/edhr-batch-route` / Then 页面展示路线列表和 BATCH 用途配置入口，批记录报表通过现有报表列表选择，不输入裸 ID。

RED: `node tests/e2e/mes-process-use-route-tabs-static.spec.js` -> FAIL, expected reason: `RouteUsePage.vue`、`schedule-route/index.vue`、`edhr-batch-route/index.vue` 尚不存在。

GREEN: `node tests/e2e/mes-process-use-route-tabs-static.spec.js` -> PASS。

GREEN: `node --check tests/e2e/mes-process-use-route-tabs-real-flow.e2e.js` -> PASS。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

GREEN: `node tests/e2e/mes-process-use-route-tabs-real-flow.e2e.js` -> PASS，真实登录 `测试租户/aoteman`，菜单树包含两个新菜单，打开两个新路由，选择真实路线 `ROUTE-XLSX-00002`，保存 SCHEDULE 与 BATCH 用途备注，刷新后备注仍存在；监听未发现原始路线或路线工序 CRUD 请求。

GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260612-process-use-route-tabs/frontend-feature-evidence.md` -> PASS。

GREEN: `task_closeout.py --task-id 20260612-process-use-route-tabs --mode preview` -> PASS，status ready，无 blocked、无 warnings；未执行 apply。
