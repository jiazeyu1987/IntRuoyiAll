# 执行日志：排产工单可选列导出

- BDD: 默认可见列导出 -> Given 请求未传 `exportColumns` / When 调用导出接口 / Then 后端使用默认可见业务列。
- BDD: 自定义列导出 -> Given 请求传入合法列集合 / When 导出 / Then Excel 只包含这些列。
- BDD: 非法列拒绝 -> Given 请求包含非法列 / When 导出 / Then 返回明确参数错误且不生成文件。
- GREEN: experience-preflight -> PASS，已完成经验、后端契约和数据库权限 SQL 门禁读取。
- RED: MesProScheduleOrderControllerTest export scenarios -> EXPECTED FAIL, 原控制器缺少 `/export-excel`、列白名单、非法列拒绝和全量分页导出逻辑。
- GREEN: mvn -pl yudao-framework/yudao-spring-boot-starter-excel -DskipTests install -> PASS, 新增 ExcelUtils includeColumnIndexes 重载可被下游模块解析。
- GREEN: python -m pytest script/tests/test_mes_schedule_order_export_permission_sql.py -> PASS, 3 passed。
- GREEN: apply local export permission migration -> PASS, 本机测试库新增 `system_menu.id=5589`，并授权测试租户 `super_admin` 与 `mes_scheduler`，`aoteman` 可查到 `mes:pro-schedule-order:export`。
- GREEN: mvn -pl yudao-module-mes -DskipTests compile -> PASS。
- BLOCKER: mvn -pl yudao-module-mes "-Dtest=MesProScheduleOrderControllerTest" test -> FAIL, MES 模块 testCompile 阶段存在既有缺类测试，目标测试未进入执行。
