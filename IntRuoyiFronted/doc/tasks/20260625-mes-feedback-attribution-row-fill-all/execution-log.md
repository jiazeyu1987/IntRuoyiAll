# 执行日志：确认归属弹窗行内全部按钮迁移

BDD: 行内全部按钮迁移 -> Given 用户打开确认归属弹窗 When 查看订单工序列表 Then 顶部不再显示全局全部按钮，且每行右侧操作位显示对应全部按钮。
BDD: 行内全部按钮复用原分配逻辑 -> Given 某行存在可分配数量 When 用户点击该行全部 Then 该行分配数量按现有规则自动填满，并保持勾选联动和提交校验不变。
RED: `node tests/e2e/mes-feedback-simulated-import-static.spec.js` -> FAIL，归属弹窗尚未提供 `handleFillRowQuantity`，顶部仍是全局 `全部` 按钮。
GREEN: `node tests/e2e/mes-feedback-simulated-import-static.spec.js` -> PASS，顶部全局 `全部` 已移除，行内 `全部` 按钮与提交合同断言通过。
GREEN: `node tests/e2e/mes-feedback-attribution-process-picker-static.spec.js` -> PASS，当前多订单分配弹窗的工序选择与 `allocations` 提交合同通过。
GREEN: `node tests/e2e/mes-feedback-attribution-row-fill-static.spec.js` -> PASS，行内 `全部` 按钮位置、禁用态与勾选联动断言通过。
