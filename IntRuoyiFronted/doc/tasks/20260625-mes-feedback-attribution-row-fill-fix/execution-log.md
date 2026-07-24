# 执行日志：修复归属弹窗行内全部按钮与无限大显示

BDD: 当前订单点击全部按计划上限填充 -> Given 某候选订单工序计划 99、实际 3412 / When 用户点击该行全部 / Then 分配数量显示 99。
BDD: 当前订单点击全部按剩余上限填充 -> Given 某候选订单工序计划 99、剩余 43 / When 用户点击该行全部 / Then 分配数量显示 43。
BDD: 其他订单计划哨兵显示无限大 -> Given 其他订单候选的 plannedQuantity 为 999999 / When 用户查看数量列 / Then 计划显示“无限大”而不是数字或短横线。
BDD: 行内全部保持勾选联动 -> Given 某候选行未勾选且可分配 / When 用户点击该行全部 / Then 该行自动勾选并写入目标分配数量。
RED: `node tests/e2e/mes-feedback-attribution-row-fill-static.spec.js` -> FAIL，新增合同要求 `importAttributionQuantity.ts` 不存在，说明“全部”取值规则与 999999 显示规则尚未显式落地。
GREEN: `node tests/e2e/mes-feedback-attribution-row-fill-static.spec.js` -> PASS，行内全部按钮已复用显式数量规则，999999 计划数量已受控显示为“无限大”。
GREEN: `node tests/e2e/mes-feedback-simulated-import-static.spec.js` -> PASS，模拟导入归属静态合同继续满足当前订单/其他订单分配与提交结构要求。
BLOCKER: `pnpm ts:check` -> FAIL，前端仓库存在与本任务无关的既有类型错误：`BatchRecordHistoryPage.vue` 缺少 `EdhrBatchExecutionReviewBatchEvent` 类型、`task/calendar/index.vue` 读取不存在的 `issue.id`。
