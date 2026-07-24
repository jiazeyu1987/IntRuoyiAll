# 20260612 工艺用途路线页签后端

## 任务目标

为 `工艺排产路线` 与 `工艺批记录路线` 提供菜单、权限和用途配置后端支撑。复用现有工艺路线、路线工序和用途配置表，不复制工艺路线数据，不放开原始路线 CRUD 权限。

## 里程碑

1. M1 审计：确认现有菜单迁移、工艺路线接口、用途配置接口和测试结构。
2. M2 RED：补 SQL 静态测试、权限反射测试、用途配置派生测试。
3. M3 GREEN：新增菜单 SQL，扩展查询/保存权限，明确用途配置派生语义。
4. M4 REGRESSION：运行目标 pytest 与 Maven 测试。
5. M5 收尾：记录验证证据，运行 cleanup 预览。

## 预期验证

- `python -X utf8 -m pytest script/tests/test_mes_process_use_route_tabs_sql.py -q`
- `mvn -pl yudao-module-mes "-Dtest=MesProRouteUseConfigServiceImplTest,MesProRouteUseConfigControllerPermissionTest,MesProRouteControllerPermissionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺少父菜单、目标菜单、权限或非法 JSON 时 SQL fail fast。
- `是否从根因和长期维护角度解决`：是；新增正式菜单与权限，复用用途配置持久化。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：COMPLETED。
- 已完成：菜单 SQL、权限注解扩展、用途配置派生测试、SQL 静态测试、目标 Maven 测试和本机数据库迁移验证。
- 注意：当前工作区存在非本任务 `ExecutionArchiveRendererTest.java` 改动；无排除执行 Maven testCompile 会被该文件阻塞。本任务目标测试使用标准 `maven.compiler.testExcludes=**/ExecutionArchiveRendererTest.java` 隔离后通过 8 个测试。

## Current Status

completed
