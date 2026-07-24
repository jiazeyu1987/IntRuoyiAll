# Execution Log

BDD: 行内显示单台班次产能 -> Given 设备列表中某设备有单台小时产能 / When 用户打开设备列表弹窗 / Then 行内显示 `单台产能/h` 和 `单台产能/班次`。

BDD: 底部显示工序总产能 -> Given 工序设备列表包含多台设备 / When 用户打开设备列表弹窗 / Then 底部显示所有设备按数量汇总后的 `总产能/h` 和 `总产能/班次`。

BDD: 班次小时说明可见 -> Given 班次产能按固定时长计算 / When 用户查看设备列表弹窗 / Then 底部显示 `1班次=10.5小时`。

RED: `node tests\e2e\mes-pro-route-process-machinery-capacity-summary.spec.js` -> FAIL, 设备列表尚未定义 `MACHINERY_CAPACITY_SHIFT_HOURS = 10.5`，仍将 `总产能/h` 放在行级表格列，缺少 `单台产能/班次` 和底部汇总。

GREEN: `node tests\e2e\mes-pro-route-process-machinery-capacity-summary.spec.js` -> PASS, 设备列表静态契约已包含单台小时产能、单台班次产能、底部总产能和 `1班次=10.5小时`。

GREEN: `node --check tests\e2e\mes-pro-route-process-machinery-capacity-summary-real-flow.e2e.js` -> PASS, 真实页面 E2E 脚本语法正确。

GREEN: `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。

GREEN: `node tests\e2e\mes-pro-route-process-machinery-capacity-summary-real-flow.e2e.js` -> PASS, 登录本机 `芋道源码/admin`，打开 `/mes/pro/route?openId=900026`，`B010 吹球囊成型` 设备列表弹窗表头为 `工作站 设备编码 设备名称 数量 单台产能/h 单台产能/班次`，底部显示 `1班次=10.5小时 总产能/h：47.61905 总产能/班次：500`。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260608-route-process-machinery-capacity-summary\frontend-feature-evidence.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-route-process-machinery-capacity-summary --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --worktree-closeout off --json` -> PASS, status `ready`, delete `<none>`, blocked `<none>`, warnings `<none>`。
