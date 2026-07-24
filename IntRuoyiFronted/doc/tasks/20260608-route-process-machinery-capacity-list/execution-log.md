# Execution Log

BDD: 设备列表显示单台和总产能 -> Given 工艺路线工序绑定了设备且设备+工序产能已配置 / When 用户打开该工序设备列表 / Then 每台设备显示单台产能和数量计算后的总产能。

BDD: 缺少设备工序产能不兜底 -> Given 工位设备缺少设备+工序产能 / When 用户查看设备列表 / Then 单台产能和总产能显示未配置，不使用设备主档产能替代。

RED: `mvn -pl yudao-module-mes -Dtest=MesProRouteProcessControllerWorkstationViewTest test` -> FAIL, 后端设备列表 VO 尚未提供产能字段，前端无可展示字段。

GREEN: `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS, 前端接口类型和 Vue 模板通过类型检查。

GREEN: Playwright admin readonly API check -> PASS, 登录本机 `芋道源码/admin` 后只读请求 `/admin-api/mes/pro/route-process/list-by-route?routeId=900026`，`B010 吹球囊成型` 设备 `A03190` 返回 `machineryStandardHourlyCapacity=9.52381` 与 `machineryHourlyCapacityTotal=9.52381`。

GREEN: Playwright admin readonly UI check -> PASS, 打开 `/mes/pro/route?openId=900026`，点击 `B010` 行设备列表，弹窗显示 `单台产能/h`、`总产能/h`、`A03190` 和 `9.52381`。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-route-process-machinery-capacity-list --mode preview` -> PASS, status `ready`, delete `<none>`, blocked `<none>`, warnings `<none>`.

INFO: `tool\verify_tdd_compliance.py` -> SKIP, 当前前端仓库不存在该仓库级 TDD 合规脚本；本次以前端类型检查和 Playwright 真实页面只读验证作为前端放行证据。
