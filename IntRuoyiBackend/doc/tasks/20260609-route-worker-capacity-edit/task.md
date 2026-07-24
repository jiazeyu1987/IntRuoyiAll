# 工艺路线人工产能直接编辑

## 任务目标

在 MES 工艺路线“组成工序”里让人工工序可以直接维护排产产能数据：点击 `5人` 或行内 `编辑` 时显示“人工产能”编辑区，可修改人数、单人产能/h、班次小时，并自动显示班次总产能。底层仍使用现有资源模型：人数保存到 `mes_md_workstation_worker.quantity`，单人产能/h 与班次小时保存到 `mes_md_workstation`，不新增资源大表或排产专用表。

## Previous Task Check

- 前序同仓库相关任务：`doc/tasks/20260609-route-structured-scheduling-resource-implementation/task.md`。
- 检查结果：该任务已标记 `completed`；本任务在其结构化资源摘要接口与资源保存接口基础上继续扩展。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。班次小时以正式工作站字段保存，缺少数据时按现有资源状态暴露为未配置或产能缺失，不用假保存。
- `是否从根因和长期维护角度解决`：是。排产人工产能仍归属工作站和工作站人力资源绑定，避免前端大表与底层数据分叉。
- `是否存在临时补丁或绕过`：否。新增的是工作站正式字段与资源保存契约，不新增临时状态或旁路表。

## BDD 场景

- BDD: 人工工序返回可编辑资源绑定 -> Given 人工工序已绑定工作站和人力资源 / When 查询工艺路线工序列表 / Then 接口返回工作站编号、人力资源绑定编号、人数、单人产能/h、班次小时和班次总产能。
- BDD: 人工产能保存更新既有底层表 -> Given 人工工序的工作站已有一条人力资源绑定 / When 保存人数、单人产能/h 和班次小时 / Then 系统更新既有人力资源数量，并更新工作站单人产能/h 与班次小时，不新增重复人力绑定。
- BDD: 无人力绑定时创建工作站人力资源 -> Given 人工工序工作站没有人力资源绑定 / When 保存人数、单人产能/h 和班次小时 / Then 系统为该工作站创建人力资源绑定，并更新工作站产能字段。

## 里程碑

- [x] M1：补后端 RED 测试，锁定接口字段、班次小时计算和保存不重复新增。
- [x] M2：实现工作站班次小时字段、资源保存契约和路线工序响应字段。
- [x] M3：运行后端单测与迁移/结构静态校验。
- [x] M4：与前端任务联调验证。

## 预期验证

- `mvn -pl yudao-module-mes "-Dtest=MesProRouteProcessControllerWorkstationViewTest,MesProRouteResourceServiceImplTest" test`
- `python -m pytest script\tests\test_mes_workstation_shift_hours_sql.py`
- SQL 结构静态检查：`mes_md_workstation.shift_hours` 出现在 MySQL 初始化 SQL 与 H2 测试建表 SQL。

## 当前状态

completed

## 完成记录

- `mes_md_workstation` 新增正式字段 `shift_hours`，用于保存人工/工序班次小时。
- `/mes/pro/route-process/list-by-route` 返回 `workstationWorkerId`，并按工作站 `shiftHours` 计算标准/今日班次产能。
- `/mes/pro/route-resource/save` 的人工保存支持 `workerQuantity`、`singleStandardHourlyCapacity`、`shiftHours` 一次提交。
- 当只传 `workstationId` 且已有工作站人力资源绑定时，更新既有绑定，不新增重复人力资源记录；没有绑定时才创建。

## 最终验证

- RED: `mvn -pl yudao-module-mes "-Dtest=MesProRouteProcessControllerWorkstationViewTest,MesProRouteResourceServiceImplTest" test` -> FAIL，缺少 `shiftHours`、`workstationWorkerId` 与 `updateWorkerCapacity` 契约。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProRouteProcessControllerWorkstationViewTest,MesProRouteResourceServiceImplTest" test` -> PASS，11 tests。
- GREEN: `python -m pytest script\tests\test_mes_workstation_shift_hours_sql.py` -> PASS，2 tests。
- GREEN: `rg -n "shift_hours|shiftHours|班次小时" sql\mysql\20260609_mes_md_workstation_shift_hours.sql sql\mysql\ruoyi-vue-pro.sql yudao-module-mes\src\test\resources\sql\create_tables.sql yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes` -> PASS。

## Cleanup Keep

- `doc/tasks/20260609-route-worker-capacity-edit/backend-api-evidence.md`
- `doc/tasks/20260609-route-worker-capacity-edit/database-schema-evidence.md`
