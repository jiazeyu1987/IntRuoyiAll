# 第三方报工导入列表与进度回归证据

## Bug Summary

用户在报工页签选择“第三方报工”并导入 `C:\Users\BJB110\Desktop\文档\李萍.xlsx` 后，页面显示“直接报工导入结果”弹框且有进度更新信息，但弹框确认后正式报工列表没有新增报工内容，排产工单进度也可能没有按正式链路增长。

## Expected

- 李萍直报 Excel 的可匹配行必须创建 `MesProFeedbackDO` 正式报工并提交到审批中。
- 导入审计记录必须通过 `sourceImportRecordId` / `feedbackId` 与正式报工互相关联。
- 排产进度必须从正式报工汇总重算；不能用导入记录直接进度字段伪造成功。
- 缺少报工人、审批人、任务、剩余数量等正式报工前置条件时，必须返回结构化跳过原因，不得直接写进度兜底。

## Reproduction

- 前端链路：`ThirdPartyFeedbackImportForm.vue` 导入直报成功后展示结果弹框并向父页面发送 success；父页面 `index.vue` 对 `李萍报工单` 切到正式报工列表并调用 `getList()`。
- 后端旧链路：`ThirdPartyFeedbackImportServiceImpl#importDirectWorkReportWorkbook` 仅写入 `MesProFeedbackImportRecordDO.PROGRESS_SOURCE_TYPE_DIRECT_WORK_REPORT` 和 `progressQuantity`，没有创建正式报工，因此正式报工列表查询不到新增记录。

## Root Cause

李萍直报导入把“导入审计记录直接进度”当成成功输出，绕过了正式 `MesProFeedbackDO` 创建和提交链路。前端确认后刷新的是正式报工列表，因此结果弹框看似成功但列表无新增；进度也依赖导入记录重算而非正式报工状态，违反“不 fallback / 不默认成功”的项目约束。

## Regression Test

- 新增后端回归：`ThirdPartyFeedbackImportServiceImplTest#importDirectWorkReportWorkbook_shouldCreateSubmittedFeedbackAndLinkImportRecordForMatchedRow`，断言匹配行创建正式报工、提交审批、关联导入记录，且导入记录不再写直接进度字段。
- 更新相邻直报契约：缺报工人跳过不写进度；重复导入再次创建正式报工并通过正式报工汇总进度；超剩余数量跳过不写进度。
- 更新前端刷新静态合同：排产刷新 payload 类型已抽到共享 `scheduleEvents.ts`，静态验证同步读取共享定义并继续校验页面监听和命中刷新逻辑。

## RED:

- `mvn -pl yudao-module-mes -am "-Dtest=ThirdPartyFeedbackImportServiceImplTest#importDirectWorkReportWorkbook_shouldCreateSubmittedFeedbackAndLinkImportRecordForMatchedRow" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增回归期望 `submittedCount=1`，旧实现实际为 `0`。

## GREEN:

- `mvn -pl yudao-module-mes -am "-Dtest=ThirdPartyFeedbackImportServiceImplTest#importDirectWorkReportWorkbook_shouldCreateSubmittedFeedbackAndLinkImportRecordForMatchedRow" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- `mvn -pl yudao-module-mes -am "-Dtest=ThirdPartyFeedbackImportServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，25 tests, 0 failures, 17 skipped。
- `node tests/e2e/mes-direct-work-report-import-result-static.spec.js` -> PASS。
- `node tests/e2e/mes-direct-work-report-refresh-schedule-order-static.spec.js` -> PASS。

## Verification

后端目标回归和导入服务相邻回归均已通过；前端直接报工结果展示与排产刷新静态合同均已通过。宽口径 `mes-feedback-tracking-static.spec.js` 当前因无关 token `删除报工失败，请检查后端接口。` 失败，本次未修改该追踪页面业务行为。

## Blockers And Follow-Up

- 当前工作区存在并发任务脏改动和并发基线提交 `7186c11a2`，该提交已包含本任务后端实现和初始任务文档；后续提交需只 stage 本任务剩余验证文档和直接报工静态合同同步。
- 尚未执行真实 Playwright 页面导入，因为本轮未启动本地前后端、未确认登录态和测试租户；不能用 API-only 代替真实路径通过。
