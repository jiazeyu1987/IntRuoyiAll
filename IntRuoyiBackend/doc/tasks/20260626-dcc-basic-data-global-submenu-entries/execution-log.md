# Execution Log：DCC 基础数据迁入全局基础数据子入口（后端）

BDD: DCC 菜单挂到全局基础数据下 -> Given DCC 菜单 SQL 种子执行完成 / When 动态菜单返回基础数据树 / Then DCC项目代码 与 DCC产品目录 作为全局基础数据子菜单返回。
BDD: 项目代码和产品目录为独立页面菜单 -> Given 菜单树加载到前端 / When 用户点击两个子菜单 / Then 各自指向独立 component/path，不再共享同一个页面内 tab 壳。

INFO: task-created -> 后端任务文档已创建，准备补菜单迁移与 schema RED 断言。
RED: mvn --% -f pom.xml -pl yudao-module-dcc -Dtest=DccBaseSchemaTest -Dsurefire.failIfNoSpecifiedTests=false test -> FAIL, DCC schema 尚未把基础数据子入口迁到全局 `/mdm`，也没有 `DCC产品目录` 子菜单。
GREEN: mvn --% -f pom.xml -pl yudao-module-dcc -Dtest=DccBaseSchemaTest -Dsurefire.failIfNoSpecifiedTests=false test -> PASS
INFO: implementation-complete -> 已更新 `20260513_dcc_base_schema.sql` 并新增 `20260626_dcc_basic_data_global_submenu.sql`，将 DCC 项目代码 / 产品目录正式挂到全局基础数据下。
GREEN: experience-preflight -> PASS, 已按正式 SQL 门禁复核目标本机 MySQL、菜单迁移脚本与后续动态菜单验收路径，可执行本地数据库写入验证。
GREEN: local runtime menu migration -> PASS, 已在本机 `ruoyi-vue-pro` 数据库运行修正后的菜单迁移，并验证 `system_menu` 中 `990200/990210/990216` 分别对应 `基础数据`、`DCC项目代码`、`DCC产品目录`，`system_role_menu` 已同步覆盖测试租户角色。
