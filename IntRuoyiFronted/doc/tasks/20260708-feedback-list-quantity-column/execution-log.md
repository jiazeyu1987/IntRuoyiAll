# 执行日志：生产报工正式列表补充报工个数列

BDD: 正式报工列表展示报工个数 -> Given 用户打开生产报工正式报工列表 / When 查看产品、工序、人员和日期信息 / Then 表格同时显示“报工个数”，并绑定正式报工接口的 `feedbackQuantity` 字段。

BDD: 不影响待归属列表 -> Given 用户切换到待归属页签 / When 查看导入待归属记录 / Then 原有“报工数量”列和归属编辑流程保持不变。

RED: node tests\e2e\mes-feedback-list-excel-columns-static.spec.js -> FAIL，正式报工列表缺少按截图顺序展示的“报工个数”列。

CHANGE: `src/views/mes/pro/feedback/index.vue` -> 在正式报工列表的“工段长”和“日期”之间新增 `报工个数` 列，绑定 `feedbackQuantity`，右对齐显示数量。

GREEN: node tests\e2e\mes-feedback-list-excel-columns-static.spec.js -> PASS，正式报工列表列顺序包含“报工个数”，并绑定正式报工接口字段 `feedbackQuantity`；待归属列表原“报工数量”列未改动。

GREEN: pnpm ts:check:schedule -> PASS，前端排产/MES 相关 TypeScript 检查通过。
