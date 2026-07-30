# Verification Report

## Scope

- 新增“MES 工序”同级菜单页面，位于“工序设置”和“工艺流程”之间。
- 保留工艺路线编辑页内部“MES 工序”页签，位于“基础信息”和“流转关系图”之间。
- 列表展示当前路线工序、工序设置主数据、工作站派生设备和批记录报表名称。
- 不新增数据库表，不改变路线保存、流转关系图保存、报工提交或记录本写入链路。

## Commands

- `node tests/e2e/mes-route-mes-process-tab-static.spec.js` -> PASS。
- `pnpm e2e:mes:route-mes-process-tab:static` -> PASS。
- `node tests/e2e/mes-route-basic-info-tab-static.spec.js` -> PASS。
- `node tests/e2e/mes-route-edit-default-flow-tab-static.spec.js` -> PASS。
- `node tests/e2e/mes-route-flow-entry-readonly-static.spec.js` -> PASS。
- `node tests/e2e/mes-route-flow-graph-only-static.spec.js` -> PASS。
- `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file IntRuoyiBackend\sql\mysql\20260512_mes_base_schema.sql --sql-file IntRuoyiBackend\sql\mysql\20260709_mes_route_process_flow_graph.sql --sql-file IntRuoyiBackend\sql\mysql\20260709_mes_route_flow_config_unification.sql --sql-file IntRuoyiBackend\sql\mysql\20260730_mes_process_readonly_catalog_menu.sql --output doc\tasks\20260730-mes-process-mapping-tab\migration-policy-gate-after.json` -> PASS。
- `pnpm ts:check` -> PASS。
- `mvn.cmd -pl yudao-module-mes -am "-DskipTests" compile` -> PASS。
- `node tests/e2e/mes-pro-route-resource-orphan-static.spec.js` -> PASS。
- `node tests/e2e/mes-pro-mes-process-readonly-static.spec.js` -> PASS。
- `mvn.cmd -pl yudao-server -am "-Dmaven.test.skip=true" package` -> PASS，生成 `yudao-server\target\yudao-server-exec.jar`。
- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> `status=UP`。
- 登录态 `GET /admin-api/mes/pro/route-resource/page?pageNo=1&pageSize=20` -> HTTP `200`、业务码 `0`、`total=580`、首屏 `20` 行。
- Playwright 真实页面 `/mes/pro/mes-process` -> `route-resource` 业务码 `0`、可见表格 `20` 行、`系统异常` 出现次数 `0`、console error `0`。
- 本机 Docker MySQL 应用 `20260730_mes_process_readonly_catalog_menu.sql` -> PASS。
- `git diff --check` -> PASS，仅有 CRLF 提示，无空白错误。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260730-mes-process-mapping-tab\bug-regression-evidence.md` -> PASS。

## Evidence

- `RouteFormContent.vue` 懒加载 `RouteMesProcessList.vue`，并将 `name="mesProcess"` 页签插入在 `basic` 与 `flow` 之间。
- `RouteMesProcessList.vue` 复用 `ProRouteProcessApi.getRouteProcessListByRoute(routeId)`，把 `machineryList` 展平成设备行。
- `RouteEditPage.vue` 识别 `?tab=mesProcess`，且不显示页面级保存按钮。
- `process/index.ts` 类型补齐后端已经返回的批记录报表字段。
- `mes/pro/mes-process/index.vue` 作为“工序设置”和“工艺流程”同级菜单页面，复用现有资源读模型展示产品、设备、MES工序、执行工序和批记录工序名称。
- `20260730_mes_process_readonly_catalog_menu.sql` 新增同级菜单 `MES工序` 并调整菜单排序。
- `20260730_mes_process_readonly_catalog_menu.sql` 新增 `5719 / mes:pro-mes-process:query` 查询权限，并随 `5718` 一起写入角色和租户套餐菜单集合。
- `migration-policy-gate-before.json` 记录目标 SQL 单独跑门禁时的预期失败；`migration-policy-gate-after.json` 记录补齐依赖链后的 PASS。
- `MesProRouteResourceController` 将只读资源大表查询权限扩展为 `mes:pro-route:query`、`mes:pro-route:schedule-config:query` 或 `mes:pro-mes-process:query`，支撑新同级只读页复用现有资源端点。
- 本地 `int-ruoyi-mysql / ruoyi-vue-pro` 中 `system_menu.id=5718` 已落库为 `MES工序`，HEX 为 `4D4553E5B7A5E5BA8F`；`system_menu.id=5719` 已落库为 `MES工序查询`，HEX 为 `4D4553E5B7A5E5BA8FE69FA5E8AFA2`。
- 本地库活跃 `system_role_menu` 授权行数为 `10`，且有效租户套餐中已有 `5718/5719`。
- 使用 `芋道源码/admin` 登录态请求 `get-permission-info`，菜单响应包含 `MES工序`，`path=mes-process`，`component=mes/pro/mes-process/index`，权限集合包含 `mes:pro-mes-process:query`。
- 使用本机 Chrome 新上下文登录 `http://127.0.0.1:8081` 并直达 `/mes/pro/mes-process`，页面可见 `MES工序`，侧边栏位置为 `MES 系统 > 生产管理 > 工序设置 / MES工序 / 工艺流程`。
- 真实页面捕获的 HTTP 502 来自外部头像资源，不属于 `MES工序` 菜单或列表接口失败。
- `MesProRouteResourceServiceImpl#getResourcePage` 现在先过滤无法解析到正式路线或产品的 `route_product`，再组装工序、工作站、设备和批记录工序列，避免旧孤儿关联导致只读页整页 `系统异常`。
- 本机运行态已加载修复后的独立 Jar：`E:\IntRuoyi\output\runtime\int_main\backend-mes-process-route-resource-20260730-1757.jar`，PID `33108`，`48081` health 为 `UP`。
- 登录态资源池接口修复后返回业务码 `0`，页面首屏显示 `20` 行，不再出现 `系统异常` toast。

## Remaining Notes

- 当前实现是只读映射入口；若后续要在该页直接维护 MES 工序别名、员工 UI 模板或一线报工字段，需要补正式数据模型和保存接口。
- 本地库已应用 `20260730_mes_process_readonly_catalog_menu.sql`；若用户当前浏览器仍未显示，需要退出重登或硬刷新动态菜单缓存，路径为 `MES 系统 > 生产管理 > MES工序`。
- 当前 `MES工序` 页面系统异常已在本机运行态复验修复；如果用户浏览器仍看到旧提示，优先刷新页面或退出重登以清掉旧前端会话状态。
- 其它环境可见性仍依赖对应环境应用同一迁移。
- 主工作区已有本任务外未跟踪文件；本次验证只覆盖并汇报本任务涉及文件。
