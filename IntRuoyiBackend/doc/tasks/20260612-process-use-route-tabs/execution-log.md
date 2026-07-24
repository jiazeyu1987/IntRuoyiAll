# 20260612 工艺用途路线页签后端执行日志

BDD: SQL 菜单迁移 -> Given 系统已有智能排产父菜单和 eDHR 批处理父菜单 / When 执行工艺用途路线菜单迁移 / Then 创建两个固定 ID 子页签和更新权限，并同步租户套餐与租户管理员角色。

BDD: 用途路线权限边界 -> Given 用户只有工艺排产路线或工艺批记录路线权限 / When 调用路线分页和用途配置查询保存接口 / Then 用户可以读取源路线并保存用途配置，但不能调用原始工艺路线创建、更新、删除接口。

BDD: 用途配置实时派生 -> Given 源路线工序发生变化 / When 调用用途配置列表接口 / Then 返回列表以当前路线工序为准，仍存在工序保留用途配置，新增工序使用明确的派生默认值。

RED: `python -X utf8 -m pytest script/tests/test_mes_process_use_route_tabs_sql.py -q` -> FAIL, expected reason: 菜单迁移 SQL 尚不存在。

RED: `mvn --% -pl yudao-module-mes -Dtest=MesProRouteUseConfigServiceImplTest,MesProRouteUseConfigControllerPermissionTest,MesProRouteControllerPermissionTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, expected reason: 新权限反射测试和源工序实时派生测试尚未实现；另一次早期运行被本地 ERP jar 过期阻塞，随后通过 `mvn --% -pl yudao-module-erp -Dmaven.test.skip=true install` 刷新主 jar。

RED: 本机库执行初版 SQL -> FAIL, expected reason: 父菜单 900220 的显示名在真实库为 `eDHR批记录`，初版 SQL 误以 `eDHR批处理` 作为强前置；修正为校验稳定的 `id + permission + path + deleted`。

GREEN: `python -X utf8 -m pytest script/tests/test_mes_process_use_route_tabs_sql.py -q` -> PASS, 3 tests。

GREEN: `mvn --% -pl yudao-module-mes -DskipTests compile` -> PASS。

GREEN: `mvn --% -pl yudao-module-mes -Dmaven.compiler.testExcludes=**/ExecutionArchiveRendererTest.java -Dtest=MesProRouteUseConfigServiceImplTest,MesProRouteUseConfigControllerPermissionTest,MesProRouteControllerPermissionTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 8 tests。

GREEN: 本机数据库迁移验证 -> PASS，`system_menu` 含 900121/900122/900221/900222，排产菜单排序按计划落库，租户套餐含新菜单的记录数为 2，tenant_admin 新菜单绑定记录数为 8。

GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260612-process-use-route-tabs/backend-api-evidence.md` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260612-process-use-route-tabs/database-schema-evidence.md` -> PASS。

GREEN: `task_closeout.py --task-id 20260612-process-use-route-tabs --mode preview` -> PASS，status ready，无 blocked、无 warnings；未执行 apply。

BLOCKED-NOTE: 无排除执行 Maven target test 当前会被非本任务 `ExecutionArchiveRendererTest.java` testCompile 错误阻塞；本任务没有修改该 eDHR 归档测试。
