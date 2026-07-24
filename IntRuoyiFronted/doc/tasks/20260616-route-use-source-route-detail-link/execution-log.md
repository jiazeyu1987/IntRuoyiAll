# 执行日志

- 2026-06-16：读取 `frontend-feature-delivery` 技能、前端契约、`docs/experience-index.md` 与 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- 2026-06-16：上一前端任务 `20260616-route-use-1000-time-formula` 未完成；已标记 BLOCKED，避免混做旧需求。
- BDD: 列表路线名称打开源工艺路线详情 -> Given 用户进入工艺排产路线或工艺批记录路线列表 / When 点击某条路线的路线名称 / Then 系统打开该路线的只读“工艺路线详情”弹框。
- BDD: 路线编码继续打开用途配置 -> Given 用户进入用途路线列表 / When 点击路线编码 / Then 系统仍打开当前用途配置弹框。
- BDD: 负责人来源保持工艺路线 -> Given 用途路线列表从 `route/page` 加载源路线 / When 页面渲染负责人列 / Then 负责人展示 `ownerName`，不从用途配置读取或保存负责人。
- BDD: 详情钻取不写原始路线 -> Given 用户从用途路线列表点击路线名称查看详情 / When 弹框加载源工艺路线数据 / Then 不调用原始路线或工序的新增、修改、删除接口。
- RED: `node tests/e2e/mes-route-use-source-route-detail-link-static.spec.js` -> FAIL, 当前 `RouteUsePage.vue` 未引入和挂载 `RouteForm`，路线名称仍为普通文本。
- GREEN: `node tests/e2e/mes-route-use-source-route-detail-link-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-process-use-route-tabs-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-use-config-display-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-edhr-multi-batch-route-static.spec.js` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- BLOCKER: experience-preflight -> `node scripts/preflight/login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password admin123 --target-path /mes/pro/schedule-route --target-text 工艺排产路线` 失败：HTTP 200 返回 `登录失败，账号密码不正确`；影响：不得执行测试租户真实 Playwright 点击链路，也不得静默切换租户或账号。
- GREEN: `node --check tests/e2e/mes-route-use-source-route-detail-link-real-flow.e2e.js` -> PASS。
- GREEN: `git diff --check` -> PASS，仅提示现有 LF/CRLF 换行转换 warning。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260616-route-use-source-route-detail-link/frontend-feature-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260616-route-use-source-route-detail-link --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
