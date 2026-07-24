# 执行日志：调整排产工作台报工偏差卡片

- BDD: 删除今日可用产能卡片 -> Given 排产员打开工作台 / When 顶部指标加载 / Then 不再显示“今日可用产能”卡片。
- BDD: 报工偏差整数展示 -> Given summary 返回带小数的 reportedDeviationQuantity / When 指标卡渲染 / Then “报工偏差”主值按整数展示，避免 1810.8078 这类小数。
- BDD: 点击报工偏差查看明细 -> Given 工作台 summary 已加载总偏差和工序相关信息 / When 排产员点击“报工偏差”卡片 / Then 页面打开弹窗展示总偏差、报工数、已排任务数和当前可用工序偏差信息。
- BLOCKER: backend-process-deviation-detail -> ruoyi-vue-pro 最近任务 `20260703-showroom-product-import-target-market-overflow` 仍为 in_progress，不能直接新增后端 summary 工序级偏差契约；本轮仅实现前端已有数据范围内的偏差弹窗。
- RED: `node tests/e2e/mes-pro-scheduler-workbench-static.spec.js; node tests/e2e/mes-scheduler-workbench-interaction-static.spec.js` -> FAIL，当前仍存在“今日可用产能”卡片，且缺少报工偏差明细弹窗。
- GREEN: `node tests/e2e/mes-pro-scheduler-workbench-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-scheduler-workbench-interaction-static.spec.js` -> PASS。
- GREEN: `node --check tests/e2e/mes-pro-scheduler-workbench-static.spec.js; node --check tests/e2e/mes-scheduler-workbench-interaction-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260703-scheduler-workbench-feedback-deviation-dialog --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
