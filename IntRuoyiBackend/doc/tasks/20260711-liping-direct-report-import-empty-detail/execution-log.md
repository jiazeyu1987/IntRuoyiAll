# Execution Log

## User Request

- 用户反馈：报工导入 `C:\Users\BJB110\Desktop\文档\李萍.xlsx` 时，结果弹窗明细为空，但应该有导入数据。
- 截图现象：工作表数 1、创建报工数 50、提交审批数 0、跳过杂务行 20，明细区显示“本次导入未创建报工明细”。

## Preflight

- 已读取 `docs/powershell-memory.md` 与 `docs/experience-index.md`。
- 命中经验：PowerShell 中文编码门禁、MES 报工导入旧工序/路线工序身份门禁、前端样式门禁。
- GREEN: experience-preflight -> PASS，当前阶段仅本机代码与测试，不操作服务器、不写真实租户数据。

## BDD Scenarios

- BDD: 直接报工导入存在待归属行时结果可追踪 -> Given 李萍报工单解析出生产行且导入服务创建待归属导入记录 / When 前端打开直接报工导入结果 / Then 结果不能以“创建报工数”误指待归属记录，且用户能看到本次导入行的状态或待归属原因。
- BDD: 直接报工成功创建报工时展示明细 -> Given 李萍报工单行能唯一匹配工单、任务、路线工序和审批人 / When 导入成功创建并提交报工 / Then 结果弹窗按工单展示工序、产品、本次完成、进度变化和报工单号。

## TDD Evidence

- RED: `mvn -pl yudao-module-mes "-Dtest=ThirdPartyFeedbackImportServiceImplTest#importDirectWorkReportWorkbook_shouldKeepPendingRowsWhenScheduleRouteFlowDisabled+importDirectWorkReportWorkbook_shouldMatchTaskCodeWhenScheduleProcessLinksMultipleTasks" test` -> FAIL，`DirectWorkReportDetail` 缺少 `attributionStatus/sheetName/rowNo/feedbackUserCode/approverName/remark` 等字段，证明待归属行无法作为结果明细返回。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=ThirdPartyFeedbackImportServiceImplTest#importDirectWorkReportWorkbook_shouldKeepPendingRowsWhenScheduleRouteFlowDisabled+importDirectWorkReportWorkbook_shouldMatchTaskCodeWhenScheduleProcessLinksMultipleTasks" test` -> PASS，待归属行和已创建报工行均返回结构化结果明细。
- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=ThirdPartyFeedbackExcelParserTest,ThirdPartyFeedbackImportServiceImplTest,MesProFeedbackControllerImportDirectWorkReportXlsxTest" test` -> PASS，28 tests, 0 failures。
- GREEN: `node tests/e2e/mes-direct-work-report-import-result-static.spec.js` -> PASS，前端导入结果统计和状态展示契约通过。
- REGRESSION: `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS。

## Root Cause

- 后端 `importedCount` 实际统计的是导入记录数，包含待归属记录；但前端把它标成“创建报工数”。
- 后端只在成功创建并提交正式报工后才向 `directWorkReportDetails` 追加明细；当真实文件导入后 50 行都进入待归属或未提交状态时，响应会出现 `importedCount=50`、`submittedCount=0`、`directWorkReportDetails=[]`，导致结果弹窗空白。
- 根因不是 Excel 解析为空；真实文件已解析出 50 条生产行和 20 条杂务行。

## Completed Work

- 为直接报工结果明细增加 `sheetName/rowNo/attributionStatus/feedbackUserCode/feedbackUserName/approverName/remark` 字段。
- 后端待归属行也返回 `DirectWorkReportDetail`，保留来源行、工单、工序、人员、数量和失败归属说明。
- 前端结果统计改为“导入记录数 / 创建报工数 / 待归属数 / 跳过杂务行”，避免把待归属记录误称为已创建报工。
- 前端明细表增加“已创建 / 待归属”状态和待归属原因；已创建行继续展示进度变化和报工单号。
- 更新真实 E2E 断言口径：`submittedCount + pendingCount = importedCount`，明细数量与导入记录数一致。

## Notes

- 真实 Excel 读取结果：1 个工作表，71 行，14 列；表头后 70 行中包含 50 条生产/导入行与 20 条杂务计时行。
- 现有 `mes-feedback-tracking-static.spec.js` 仍被非本任务缺失文案 `删除报工失败，请检查后端接口。` 阻塞；本任务未修改该契约对应页面，改用新增的直接报工结果静态契约覆盖本次修复范围。

## Closeout

- 后端实现提交：`267c2ea076`。
- 前端实现提交：`a52915be8`。
- `task-closeout-cleanup --mode preview` -> PASS，keep `task.md/execution-log.md/verification-report.md`，delete `<none>`，blocked `<none>`。
- `task-closeout-cleanup --mode apply` -> PASS，deleted_paths `<none>`。
