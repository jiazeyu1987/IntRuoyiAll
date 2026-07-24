# 执行日志：同步排产工作台报工偏差新口径

- BDD: 卡片显示当次排产总偏差 -> Given summary 返回新的总偏差字段 / When 工作台渲染 / Then 报工偏差卡片显示当次排产实际报工数量与排产数量的差值。
- BDD: 弹窗显示工序明细 -> Given summary 返回工序偏差明细 / When 点击报工偏差卡片 / Then 弹窗逐条展示工序 planned/reported/deviation，不再使用瓶颈数据伪装。
- GREEN: `node tests/e2e/mes-pro-scheduler-workbench-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-scheduler-workbench-interaction-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
