# Execution Log: 排产工单主列表隐藏工单编码列

BDD: 主列表隐藏工单编码 -> Given 排产员进入排产工单主列表 / When 查看主表列头 / Then 主表不显示“工单编码/工单编号”列，首个业务数据列为产品编号。

BDD: 查询区保留工单编码筛选 -> Given 排产员需要按工单编码筛选 / When 查看查询区 / Then 查询条件仍保留“工单编码”输入框。

BDD: 冻结醒目样式保持 -> Given 排产工单处于冻结状态 / When 排产员浏览列表 / Then 冻结醒目样式保持不变。

RED: `node tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js` -> FAIL, expected reason: 当前静态契约仍要求主列表显示 `label="工单编码"`，页面主表也仍包含该列。

GREEN: `node --check tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js` -> PASS。

GREEN: `node --check tests/e2e/mes-schedule-order-workorder-link-static.spec.js` -> PASS。

GREEN: `node tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js` -> PASS。

GREEN: `node tests/e2e/mes-schedule-order-workorder-link-static.spec.js` -> PASS。

GREEN: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。

Verification: 排产工单主列表已移除 `label="工单编码"` / `prop="erpWorkOrderCode"` 列和对应跳转链接；查询区仍保留工单编码筛选；冻结醒目展示契约保持通过。

GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260702-schedule-order-hide-workorder-code-column/frontend-feature-evidence.md` -> PASS, Frontend feature evidence is valid。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260702-schedule-order-hide-workorder-code-column --mode preview` -> PASS, status ready, delete none, blocked none。
