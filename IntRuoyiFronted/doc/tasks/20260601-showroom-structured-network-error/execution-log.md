# 执行日志：展厅结构化网络错误提示

BDD: 网络无响应错误提示 -> Given 浏览器请求展厅接口且没有收到后端响应 / When 前端格式化错误 / Then 提示包含 `NETWORK_RESPONSE_UNAVAILABLE`、请求方法与 URL、后端服务/网络/CORS/反向代理排查方向和原始错误。

BDD: 后端结构化错误保持原格式 -> Given 后端返回已有结构化错误信息 / When 前端格式化错误 / Then 仍显示后端错误码、目标、资源和接口信息。

RED: `node <HEAD structuredError regression probe>` -> FAIL, `HEAD` 版本只返回 `展厅发布失败：Network Error`，缺少 `NETWORK_RESPONSE_UNAVAILABLE` 和请求目标。

GREEN: `node --check scripts\showroom-structured-network-error.test.mjs` -> PASS。

GREEN: `node scripts\showroom-structured-network-error.test.mjs` -> PASS，2 passed。

CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260601-showroom-structured-network-error --mode preview` -> PASS，delete `<none>`，blocked `<none>`。

CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260601-showroom-structured-network-error --mode apply` -> PASS，deleted_paths `<none>`。
