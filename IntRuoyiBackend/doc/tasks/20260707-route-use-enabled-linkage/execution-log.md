# 路线用途启停联动执行日志

BDD: 用途启用受基础路线状态约束 -> Given 基础工艺流程已禁用 When 用户尝试启用排产或批记录用途 Then 后端拒绝并返回“工艺流程已经禁用，请先启用工艺流程”。

BDD: 排产用途和批记录用途独立启停 -> Given 同一路线存在排产和批记录用途配置 When 用户只切换排产用途 Then 只更新排产用途记录，不影响批记录用途。

BDD: 用途启停字段可发布迁移 -> Given 旧库缺少用途级启用字段 When 执行发布迁移 Then `mes_pro_route_use_config.enabled` 以非空默认禁用状态创建。

RED: mvn.cmd -pl yudao-module-mes -Dtest=MesProRouteUseConfigServiceImplTest test -> FAIL, `MesProRouteUseConfigServiceImpl` 未覆盖 `updateRouteUseEnabled(Long,String,Boolean)`，用途启停服务契约缺失实现。

RED: python -m pytest script/tests/test_mes_route_use_config_enabled_sql.py -> FAIL, SQL 契约测试尚未存在，无法校验 `mes_pro_route_use_config.enabled` 迁移定义。

GREEN: mvn.cmd -pl yudao-module-mes -Dtest=MesProRouteUseConfigServiceImplTest test -> PASS, 13 个用途配置服务测试通过，覆盖禁用基础路线拒绝启用、用途类型独立更新和禁用配置创建。

GREEN: node tests/e2e/mes-route-use-enabled-linkage-static.spec.js -> PASS, 前端用途启停静态契约通过。

GREEN: node tests/e2e/mes-route-use-copy-buttons-static.spec.js -> PASS, 路线用途复制按钮回归契约通过。

GREEN: node tests/e2e/mes-route-use-source-route-detail-link-static.spec.js -> PASS, 路线编码和路线名称原入口未回归。

GREEN: pnpm.cmd exec eslint src/views/mes/pro/route-use/RouteUsePage.vue tests/e2e/mes-route-use-enabled-linkage-static.spec.js tests/e2e/mes-route-use-copy-buttons-static.spec.js -> PASS, 目标前端页面和静态契约测试无 ESLint 问题。

## 收尾

- 状态：completed
- GREEN: python -m pytest script/tests/test_mes_route_use_config_enabled_sql.py -> PASS, 2 个 SQL 契约测试通过，确认迁移脚本和测试建表字段定义一致。
