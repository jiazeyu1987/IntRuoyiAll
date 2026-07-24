BDD: 人工工序返回可编辑资源绑定 -> Given 人工工序已绑定工作站和人力资源 / When 查询工艺路线工序列表 / Then 接口返回工作站编号、人力资源绑定编号、人数、单人产能/h、班次小时和班次总产能。
BDD: 人工产能保存更新既有底层表 -> Given 人工工序的工作站已有一条人力资源绑定 / When 保存人数、单人产能/h 和班次小时 / Then 系统更新既有人力资源数量，并更新工作站单人产能/h 与班次小时，不新增重复人力绑定。
BDD: 无人力绑定时创建工作站人力资源 -> Given 人工工序工作站没有人力资源绑定 / When 保存人数、单人产能/h 和班次小时 / Then 系统为该工作站创建人力资源绑定，并更新工作站产能字段。
RED: `mvn -pl yudao-module-mes "-Dtest=MesProRouteProcessControllerWorkstationViewTest,MesProRouteResourceServiceImplTest" test` -> FAIL，测试编译失败，缺少 `shiftHours`、`workstationWorkerId` 与 `updateWorkerCapacity` 契约。
GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProRouteProcessControllerWorkstationViewTest,MesProRouteResourceServiceImplTest" test` -> PASS，11 tests。
GREEN: `python -m pytest script\tests\test_mes_workstation_shift_hours_sql.py` -> PASS，2 tests。
GREEN: `rg -n "shift_hours|shiftHours|班次小时" sql\mysql\20260609_mes_md_workstation_shift_hours.sql sql\mysql\ruoyi-vue-pro.sql yudao-module-mes\src\test\resources\sql\create_tables.sql yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes` -> PASS。
