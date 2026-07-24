# Execution Log

BDD: 有设备工序返回机器班次产能 -> Given 工序工作站绑定多台设备且设备工序小时产能已配置 / When 查询工艺路线工序列表 / Then 返回 `capacitySource=MACHINE`、设备数量合计、工序小时产能合计和 10.5 小时班次产能。

BDD: 无设备工序返回人工班次产能 -> Given 工序工作站没有设备但配置了单人小时产能和人工人数 / When 查询工艺路线工序列表 / Then 返回 `capacitySource=WORKER`、`workerQuantityTotal` 和人工总班次产能。

BDD: 有设备时不使用人工兜底 -> Given 工序同时存在设备和人工配置 / When 查询工艺路线工序列表 / Then 工序产能只来自设备工序产能，不叠加或替换为人工产能。

RED: `mvn -pl yudao-module-mes -Dtest=MesProRouteProcessControllerWorkstationViewTest test` -> FAIL，`MesProRouteProcessRespVO` 尚未提供 `capacitySource`、`workerQuantityTotal`、`processHourlyCapacityTotal`、`processShiftCapacityTotal` 字段 getter。

GREEN: `mvn -pl yudao-module-mes -Dtest=MesProRouteProcessControllerWorkstationViewTest test` -> PASS，6 tests，覆盖有设备工序 `MACHINE` 班次产能、无设备工序 `WORKER` 5 人人工班次产能、无工位 `UNCONFIGURED` 和有设备不叠加人工产能。

GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File script\deploy\restart-int-ruoyi-local.ps1 -Component backend` -> PASS，本机后端已重建并重启到 `48081`。

GREEN: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> PASS，status `UP`。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260608-route-process-shift-capacity-display\backend-api-evidence.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-route-process-shift-capacity-display --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --worktree-closeout off --json` -> PASS，status `ready`，delete `<none>`，blocked `<none>`，warnings `<none>`。
