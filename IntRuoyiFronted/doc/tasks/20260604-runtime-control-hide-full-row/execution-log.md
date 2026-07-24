# 执行日志：隐藏运行控制台 IntRuoyi 整套行

BDD: 聚合组件行不显示 -> Given 操作员进入运行控制台 / When 查看组件状态矩阵 / Then 表格只显示 `IntRuoyi 前端`、`IntRuoyi 后端`、`Website 前端`，不显示 `IntRuoyi 整套` 聚合行。

BDD: 隐藏行不丢失发布包来源 -> Given 后端仍返回 `intruoyi-full.currentReleaseTag` / When 页面计算 Test、Production、Backup 的当前发布包 / Then 页面仍优先读取该聚合状态作为发布包来源。

RED: `node tests/e2e/runtime-control-full-row-hidden.spec.js` -> FAIL, expected reason: `displayComponentRows` 仍包含 forbidden `intruoyi-full`。

GREEN: `node tests/e2e/runtime-control-full-row-hidden.spec.js` -> PASS。

GREEN: `node tests/e2e/runtime-control-static.spec.js` -> PASS。

GREEN: `node tests/e2e/runtime-control-ops-static.spec.js` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260604-runtime-control-hide-full-row/frontend-feature-evidence.md` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260604-runtime-control-hide-full-row --mode preview` -> PASS，delete `<none>`，blocked `<none>`，warnings `<none>`。
