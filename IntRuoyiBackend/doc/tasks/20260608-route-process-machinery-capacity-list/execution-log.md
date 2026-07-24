# Execution Log

## Pass 1 - Planning

- task id: route-process-machinery-capacity-list
- changed paths: `doc/tasks/20260608-route-process-machinery-capacity-list/*`
- implemented behavior: 建立任务记录；确认设备列表弹窗应展示单台设备产能和总产能，产能来源为设备+工序产能。
- validation commands: 文档人工复核。
- validation results: PASS。
- known risks or blockers: 当前无阻塞。

BDD: 设备列表显示单台和总产能 -> Given 工艺路线工序绑定了设备且设备+工序产能已配置 When 用户打开该工序设备列表 Then 每台设备显示单台产能和数量计算后的总产能。

BDD: 缺少设备工序产能不兜底 -> Given 工位设备缺少设备+工序产能 When 用户查看设备列表 Then 单台产能和总产能显示未配置，不使用设备主档产能替代。

## Pass 2 - RED

RED: `mvn -pl yudao-module-mes -Dtest=MesProRouteProcessControllerWorkstationViewTest test` -> FAIL, expected reason: `MesProRouteProcessMachineryRespVO` 尚未提供 `getMachineryStandardHourlyCapacity()` 与 `getMachineryHourlyCapacityTotal()`，新增断言无法编译。

## Pass 3 - Backend And Frontend Implementation

- changed paths:
  - `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/MesProRouteProcessController.java`
  - `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/vo/process/MesProRouteProcessMachineryRespVO.java`
  - `yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/MesProRouteProcessControllerWorkstationViewTest.java`
  - `../yudao-ui-admin-vue3/src/api/mes/pro/route/process/index.ts`
  - `../yudao-ui-admin-vue3/src/views/mes/pro/route/RouteProcessList.vue`
- implemented behavior: 设备列表接口按 `machineryId + processId` 返回设备工序单台标准小时产能，并按工位设备绑定数量计算总标准小时产能；前端弹窗新增单台产能和总产能列。
- no fallback: 缺少设备工序产能时返回空值，前端显示 `未配置`。

GREEN: `mvn -pl yudao-module-infra -DskipTests install` -> PASS, build prerequisite aligned local Maven artifact with existing infra source so MES could compile against the current `FileService` overload.

GREEN: `mvn -pl yudao-module-mes -Dtest=MesProRouteProcessControllerWorkstationViewTest test` -> PASS, 6 tests, 0 failures, 0 errors.

GREEN: `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS.

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS, rebuilt `yudao-server\target\yudao-server.jar` with the MES route process capacity fields.

GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File script\deploy\restart-int-ruoyi-local.ps1 -Component backend` -> PASS, local backend restarted from `backend-runtime-control-20260608-221417.jar`.

GREEN: `curl.exe -s http://127.0.0.1:48081/actuator/health` -> PASS, `{"status":"UP"}`.

GREEN: Playwright admin readonly API check -> PASS, logged in to local `芋道源码/admin`, opened `/mes/pro/route`, called `/admin-api/mes/pro/route-process/list-by-route?routeId=900026`, observed `B010 吹球囊成型` equipment `A03190` with `machineryStandardHourlyCapacity=9.52381` and `machineryHourlyCapacityTotal=9.52381`.

GREEN: Playwright admin readonly UI check -> PASS, opened `/mes/pro/route?openId=900026`, clicked the `B010` equipment link, and verified the equipment dialog shows `单台产能/h`, `总产能/h`, `A03190`, and `9.52381`.

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-route-process-machinery-capacity-list --mode preview` -> PASS, status `ready`, delete `<none>`, blocked `<none>`, warnings `<none>`.

GREEN: `python -X utf8 tool\verify_tdd_compliance.py --task-dir doc\tasks\20260608-route-process-machinery-capacity-list --all-changed` -> PASS, `TDD compliance passed.`
