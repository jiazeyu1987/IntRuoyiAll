# 执行日志：修复运行控制台最近操作不显示

BDD: 最近操作不被概览失败阻断 -> Given 运行控制台操作记录接口成功返回真实操作记录，但概览接口失败 / When 页面执行刷新 / Then 最近操作列表仍必须更新显示成功返回的操作记录，顶部错误仍保留运维矩阵失败提示。

BDD: 最近操作失败必须明确暴露 -> Given 最近操作接口请求失败 / When 页面执行刷新 / Then 顶部错误必须包含最近操作失败上下文，不得静默吞掉或显示默认成功状态。

CHECK: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest" test` -> PASS，35 tests，确认后端现有运行控制台服务用例通过。

RED: `node tests/e2e/runtime-control-recent-operations-visible-static.spec.js` -> FAIL，当前 `loadOverview` 仍将 `getRuntimeControlOverview()` 与 `getRuntimeControlOperations()` 绑定到同一个 `Promise.all`，概览失败会阻断最近操作列表赋值。

GREEN: `node tests/e2e/runtime-control-recent-operations-visible-static.spec.js` -> PASS，最近操作请求已从概览请求中独立出来。

GREEN: `node tests/e2e/runtime-control-static.spec.js` -> PASS。

GREEN: `node tests/e2e/runtime-control-hide-foolproof-error-static.spec.js` -> PASS。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

REPRO: Playwright 本机真实页面刷新 -> FAIL，`/admin-api/infra/runtime-control/operations` 返回 `{"code":0,"msg":"","data":[]}`；根因转入后端同名任务修复状态目录漂移。

GREEN: Playwright 本机真实页面刷新 -> PASS，后端状态目录修复后 `/operations` 返回 34 条，最近操作表格显示 34 行。

GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260603-runtime-control-recent-operations-visible/frontend-feature-evidence.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-runtime-control-recent-operations-visible --mode preview` -> PASS，delete `<none>`、blocked `<none>`、warnings `<none>`。
