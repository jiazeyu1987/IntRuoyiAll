# Execution Log

BDD: 直接报工导入展示未创建行明细 -> Given Excel 已解析出直接报工行但未创建报工 When 导入结果弹框打开 Then 弹框仍展示导入行来源、任务单/生产订单、产品、工序、报工数量和未创建原因，而不是只显示“本次导入未创建报工明细”。

GREEN: experience-preflight -> PASS, 已读取 PowerShell、登录/E2E、报工导入旧工序与前端样式门禁；本任务只修导入结果可视化和行级明细，不改变创建报工成功/跳过统计口径。

RED: mvn -pl yudao-module-mes -Dmaven.compiler.testIncludes=**/cn/iocoder/yudao/module/mes/service/pro/feedback/importer/ThirdPartyFeedbackImportServiceImplTest.java -Dtest=ThirdPartyFeedbackImportServiceImplTest#importDirectWorkReportWorkbook_shouldSkipOverRemainingRowWhenReportedQuantityExceedsPlan+importDirectWorkReportWorkbook_shouldSkipAlreadyFullyReportedRow+importDirectWorkReportWorkbook_shouldSkipRowsWhenScheduleRouteFlowDisabled+importDirectWorkReportWorkbook_shouldRejectTextOnlyAttributionWhenScheduleProcessLinksMultipleTasks test -> FAIL, 4 个场景均只返回 0 条 directWorkReportDetails，证明跳过行没有行级明细。

RED: node tests/e2e/mes-direct-work-report-import-result-static.spec.js -> FAIL, 当前前端弹框缺少“状态 / 原因”和 formatDirectImportDetailStatus。

GREEN: mvn -pl yudao-module-mes -Dmaven.compiler.testIncludes=**/cn/iocoder/yudao/module/mes/service/pro/feedback/importer/ThirdPartyFeedbackImportServiceImplTest.java -Dtest=ThirdPartyFeedbackImportServiceImplTest#importDirectWorkReportWorkbook_shouldSkipOverRemainingRowWhenReportedQuantityExceedsPlan+importDirectWorkReportWorkbook_shouldSkipAlreadyFullyReportedRow+importDirectWorkReportWorkbook_shouldSkipRowsWhenScheduleRouteFlowDisabled+importDirectWorkReportWorkbook_shouldRejectTextOnlyAttributionWhenScheduleProcessLinksMultipleTasks test -> PASS, 跳过行返回 SKIPPED 明细且不创建报工。

GREEN: node tests/e2e/mes-direct-work-report-import-result-static.spec.js -> PASS, 前端静态合同确认大弹框展示行级状态和原因。

GREEN: pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json -> PASS, 前端类型检查通过。

GREEN: login-preflight -> PASS, 真实登录已进入目标页：baseUrl=http://localhost:8081, tenant=测试租户, username=aoteman, targetPath=/mes/pro/feedback。

GREEN: node tests/e2e/mes-direct-work-report-import-real-flow.e2e.js -> PASS, 真实导入 `C:\Users\BJB110\Desktop\文档\李萍.xlsx`，结果 importedCount=0、submittedCount=0、skippedRows=70，弹框展示行级未创建原因而不是空明细提示。
