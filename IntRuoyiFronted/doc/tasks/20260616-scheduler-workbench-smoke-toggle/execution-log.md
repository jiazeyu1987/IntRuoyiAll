# 执行日志

- 2026-06-16：创建前端任务记录，定位 `src/views/mes/pro/scheduler-workbench/index.vue` 与 `src/api/mes/pro/schedulerWorkbench/index.ts`。
- BDD: 工作台空闲时显示开始按钮 -> Given 后端状态为 `IDLE` / When 用户打开排产员工作台 / Then 页面展示“开始冒烟测试”按钮和空闲状态。
- BDD: 工作台运行时显示结束按钮 -> Given 后端状态为 `RUNNING` / When 用户打开排产员工作台 / Then 页面展示“结束冒烟测试”按钮和运行信息。
- BDD: 启停失败显式提示 -> Given 后端启动或停止接口失败 / When 用户点击按钮 / Then 页面显示后端错误，不伪造成功状态。
- RED: `node tests/e2e/mes-scheduler-workbench-smoke-toggle-static.spec.js` -> FAIL, 当前排产员工作台缺少 `smokeTestStatus` 冒烟测试启停按钮和 API 片段。
- GREEN: `node tests/e2e/mes-scheduler-workbench-smoke-toggle-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-pro-scheduler-workbench-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/smart-scheduling-smoke-real-flow-static.spec.js` -> PASS。
- RED: `pnpm ts:check` -> FAIL，Node 默认堆内存 OOM，未产生业务类型错误。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260616-scheduler-workbench-smoke-toggle --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
