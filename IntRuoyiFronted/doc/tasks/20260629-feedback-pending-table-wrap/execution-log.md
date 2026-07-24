BDD: 待归属列表长工单与产品信息可多行展示 -> Given 待归属列表包含长工单号、长产品编码或长产品名称 When 页面渲染该行 Then 单元格内容按多行换行展示，不再以省略号截断

BDD: 待归属列表长工序信息可多行展示 -> Given 待归属列表的工序列由工序编码和工序名称组成且文本较长 When 页面渲染该列 Then 工序信息允许换行显示完整内容

BDD: 待归属列表结果说明可多行展示 -> Given 归属结果列包含状态、正式报工编号、归属时间和跳过说明 When 页面渲染 Then 说明信息可按多行展示，不出现统一单行省略

RED: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-pending-table-wrap-static.spec.js -> FAIL, 待归属表仍未声明局部换行类且整表继续使用统一省略策略

GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-pending-table-wrap-static.spec.js -> PASS

GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-import-diagnostics-hidden-static.spec.js -> PASS
