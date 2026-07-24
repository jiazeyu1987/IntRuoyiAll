# 执行日志：修复运行控制台运维卡片不显示

- BDD: 运维辅助卡片必须显示 -> Given 操作员进入运行控制台 / When 状态矩阵加载后查看运维区域 / Then 页面必须显示 `站内信告警`、`责任人矩阵`、`备份演练` 三个卡片。
- BDD: 运维辅助卡片不依赖空数据隐藏 -> Given 告警、责任人或备份点接口返回空列表 / When 页面渲染运维区域 / Then 三个卡片仍显示空状态和刷新入口，不得整卡消失。
- VERIFY: 上一前端任务 `doc/tasks/20260604-runtime-control-restore-target-ui/task.md` 状态为 `completed`。
- RED: `node tests/e2e/runtime-control-ops-cards-visible.e2e.js` -> FAIL，原因：真实页面登录后不滚动查看 1366x900 初始视口，`站内信告警` 标题位于 `y=906.5`，已经超出视口底部。
- FIX: 将 `站内信告警`、`责任人矩阵`、`备份演练` 拆为运行控制台优先运维区，放在状态矩阵前；保留决策向导、巡检报告、业务健康、探针状态、日志与磁盘风险在状态矩阵后的诊断区。
- GREEN: `node tests/e2e/runtime-control-ops-cards-visible.e2e.js` -> PASS。
- GREEN: `node tests/e2e/runtime-control-ops-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/runtime-control-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/runtime-control-recent-operations-visible-static.spec.js` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence yudao-ui-admin-vue3\doc\tasks\20260604-runtime-control-ops-cards-visible\bug-regression-evidence.md` -> PASS。
- CLOSEOUT PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-runtime-control-ops-cards-visible --mode preview` -> READY，delete `<none>`，blocked `<none>`，warnings `<none>`。
