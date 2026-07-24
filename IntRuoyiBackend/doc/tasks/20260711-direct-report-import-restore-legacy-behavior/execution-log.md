# Execution Log

BDD: 直接报工导入旧口径恢复 -> Given Excel 行无法归属到唯一报工任务 When 执行直接报工导入 Then 该行按跳过处理，不创建待归属导入记录，结果统计的创建报工数只等于成功创建并提交的报工单数。
BDD: 直接报工导入弹框旧口径恢复 -> Given 直接报工导入接口返回结果 When 前端展示导入完成提示 Then 展示工作表数、创建报工数、提交审批数、跳过杂务行、报工单号、记录编号，不展示待归属明细面板。

RED: `mvn -pl yudao-module-mes "-Dtest=ThirdPartyFeedbackImportServiceImplTest#importDirectWorkReportWorkbook_shouldSkipOverRemainingRowWhenReportedQuantityExceedsPlan+importDirectWorkReportWorkbook_shouldSkipAlreadyFullyReportedRow+importDirectWorkReportWorkbook_shouldSkipRowsWhenFeedbackUserMissing+importDirectWorkReportWorkbook_shouldSkipRowsWhenEffectiveScheduleOrderMissing+importDirectWorkReportWorkbook_shouldSkipRowsWhenScheduleRouteFlowDisabled+importDirectWorkReportWorkbook_shouldRejectTextOnlyAttributionWhenScheduleProcessLinksMultipleTasks" test` -> FAIL，当前实现仍把不可归属行写入 pending 导入记录。
RED: `node tests/e2e/mes-direct-work-report-import-result-static.spec.js` -> FAIL，当前前端仍打开“直接报工导入结果”明细面板并展示待归属口径。
GREEN: `mvn -pl yudao-module-mes "-DskipTests" compile` -> PASS，后端生产代码编译通过。
GREEN: `mvn -pl yudao-module-mes "-Dmaven.compiler.testIncludes=**/cn/iocoder/yudao/module/mes/service/pro/feedback/importer/ThirdPartyFeedbackImportServiceImplTest.java" "-Dtest=ThirdPartyFeedbackImportServiceImplTest#importDirectWorkReportWorkbook_shouldSkipOverRemainingRowWhenReportedQuantityExceedsPlan+importDirectWorkReportWorkbook_shouldSkipAlreadyFullyReportedRow+importDirectWorkReportWorkbook_shouldSkipRowsWhenFeedbackUserMissing+importDirectWorkReportWorkbook_shouldSkipRowsWhenEffectiveScheduleOrderMissing+importDirectWorkReportWorkbook_shouldSkipRowsWhenScheduleRouteFlowDisabled+importDirectWorkReportWorkbook_shouldRejectTextOnlyAttributionWhenScheduleProcessLinksMultipleTasks" test` -> PASS，6 个直接报工旧口径回归用例通过。
GREEN: `node tests/e2e/mes-direct-work-report-import-result-static.spec.js` -> PASS，前端静态合同确认直接报工导入恢复旧汇总 alert。
GREEN: `NODE_OPTIONS=--max-old-space-size=8192 pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS，前端类型检查通过。

BLOCKER: `mvn -pl yudao-module-mes ... test` 常规测试编译会被非本任务的批记录、路线相关测试源码引用缺失类阻断；本任务使用 `maven.compiler.testIncludes` 限定当前回归测试文件后目标用例通过。
