# Execution Log: 排产工单冻结展示优化

BDD: 主列表隐藏排产编码 -> Given 排产员进入排产工单主列表 / When 查看列表列头 / Then 主列表不显示排产编码列，首个业务识别列为工单编码。

BDD: 冻结状态醒目展示 -> Given 排产工单处于冻结状态 / When 排产员浏览列表 / Then 冻结状态以醒目的冻结徽标、锁图标和冻结行样式展示，并通过 tooltip 暴露冻结原因。

BDD: 未冻结状态低权重 -> Given 排产工单未冻结 / When 排产员浏览列表 / Then 未冻结状态保持低视觉权重，不抢占冻结异常状态。

RED: `node tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js` -> FAIL, expected reason: 排产工单主列表仍显示 `label="排产编码"`，且缺少冻结行 class 与醒目冻结徽标。

GREEN: `node --check tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js` -> PASS

GREEN: `node tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js` -> PASS

GREEN: `node tests/e2e/mes-schedule-order-main-table-wrap-static.spec.js` -> PASS

GREEN: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS

GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260702-schedule-order-freeze-visibility/bug-regression-evidence.md` -> PASS

GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260702-schedule-order-freeze-visibility/frontend-feature-evidence.md` -> PASS

Verification: 排产工单主列表已移除排产编码列，工单编码成为固定首列；已冻结行增加整行浅橙背景、高权重文本、橙色锁图标徽标和冻结原因 tooltip；未冻结状态保持低权重灰色徽标。
