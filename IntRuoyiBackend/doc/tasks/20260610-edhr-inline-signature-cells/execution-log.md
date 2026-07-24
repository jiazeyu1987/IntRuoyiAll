# 执行记录

BDD: 模板标记复核签名位 -> Given 模板中某单元格标记为 FORM_REVIEW, When 查看已完成历史批记录, Then 该单元格显示复核签名人和签名时间。

BDD: 模板标记批准签名位 -> Given 模板中某单元格标记为 APPROVE, When 查看已批准历史批记录, Then 该单元格显示审批签名人和签名时间。

BDD: 未签名不伪造 -> Given 模板中存在签名位但历史记录没有对应签名, When 渲染历史表单, Then 单元格只显示未签名弱提示，不生成假签名。

BDD: 旧快照只读 overlay -> Given 旧执行快照没有签名位, When 当前模板配置签名位, Then 复盘接口返回签名位 marker 供只读渲染，不回写历史快照。

RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportControllerTest,MesProEdhrBatchExecutionControllerTest" test` -> FAIL, 缺少 `BatchRecordReportSignatureCellMarkersReqVO` 与签名位接口/VO 字段。

GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportControllerTest,MesProEdhrBatchExecutionControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，8 tests，签名位配置接口与复盘 marker VO 契约通过。

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，已构建包含新接口的本机后端 jar。

REGRESSION: Playwright 真实页面验证 -> PASS，测试租户打开电子批记录模板配置签名位后，历史批记录模板单元格内显示 `芋道1\n2026-06-10 02:11`，模板外签名主表数量为 0。
