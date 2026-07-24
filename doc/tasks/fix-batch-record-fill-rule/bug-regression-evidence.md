# Bug Regression Evidence

## Bug

批次执行点击“打开填写”时，系统提示“批记录模板存在未确认填写规则的可填单元格”，并列出大量坐标。症状说明执行层发现可填单元格没有被认可为已确认规则。

## Expected

用户已经在规则配置页确认并保存的可填单元格，应以执行层可识别的已确认状态写入 Jimu 报表 JSON。打开填写时，这些规则应被纳入执行快照；真正未确认的可填单元格仍应 fail fast 并提示坐标。

## Reproduction

可复现路径为：构造一个可填单元格规则，保存请求携带 `source=AUTO` 且 `reviewed=true`；旧保存逻辑会把该混合态写回 JSON；执行层 `isReviewedRule` 不接受 `AUTO` 来源，因此打开填写时报未确认坐标。

## Root Cause

根因是“用户已确认”和“自动建议”的持久化状态混用。执行层的规则确认标准是正确的安全门禁，问题发生在规则保存边界：已确认规则可能仍以 `source=AUTO` 持久化，导致后续执行读取同一 JSON 时无法识别为已确认。

## RED:

`mvn -pl yudao-module-mes -Dtest=MesProBatchRecordReportServiceImplDbTest#saveCellRules_normalizesReviewedAutoSuggestionToManualConfirmation test` -> 预期在修复前失败，失败原因是保存后的 `edhrCellRule.source` 仍为 `AUTO`，或该单元格仍计入未确认数量。实际当前工作区在进入新增断言前被无关编译错误阻塞，因此严格 RED 证据尚未取得。

## GREEN:

`mvn -pl yudao-module-mes -Dtest=MesProBatchRecordReportServiceImplDbTest#saveCellRules_normalizesReviewedAutoSuggestionToManualConfirmation,MesProBatchRecordExecutionServiceImplTest#openOrCreateByContext_freezesReviewedNumberAndDateCellRulesIntoExecutionSnapshot+openOrCreateByContext_unreviewedFillableCellRule_mustFailFastWithoutCreatingExecution,MesProBatchRecordCellRuleSupportTest#toRuleJson_normalizesReviewedAutoSuggestionToExecutableManualRule+applyAutomaticSuggestions_setsRulesAfterWordImportWithoutMarkingUserReviewed test` -> PASS，5 个关键测试通过。

## Verification

当前任务验证范围包括保存边界单测、执行打开集成路径、自动建议保持未确认的回归检查。历史异常 JSON dry run/apply 属于后续用户授权的受控修复任务，不作为当前最小修复通过条件。执行层校验不放宽，保存层归一化后应自然满足现有执行校验。

## Blockers

原始 `source/reviewed` 严格 RED 缺少修复前运行证据，作为历史证据缺口保留。用户授权纳入的编译前置、损耗报告 Word 解析和真实前端 E2E 阻塞均已修复并验证通过；当前授权范围内无剩余阻塞。历史模板 JSON dry run/apply 需另行授权。

## Additional Regression: Traditional Batch Record openTask

真实 E2E 复验时新增暴露 `/task/open` 返回 `1040750412 eDHR 批次缺少唯一批记录路线`。根因是 `openTask` 把传统批记录任务和 Form Center 动态表单任务使用同一组上下文字段校验；传统任务已有 `executionId` 和 `batchRecordReportId`，但没有 `formTemplateId/formCenterInstanceId/formBindingKey`，因此被误判为上下文缺失。

- RED: `mvn -pl yudao-module-mes '-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_opensLegacyBatchRecordTaskWithFrozenExecutionWithoutFormCenterContext' test` -> FAIL，复现 `PRO_EDHR_BATCH_EXECUTION_TASK_CONTEXT_REQUIRED` / `1040750412`。
- GREEN: `mvn -pl yudao-module-mes '-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_opensLegacyBatchRecordTaskWithFrozenExecutionWithoutFormCenterContext+openTask_requiresFrozenExecutionForBatchSharedTask' test` -> PASS，2 个测试通过。
- E2E: `node tests\e2e\edhr-batch-execution-real-flow.e2e.js` -> PASS，真实前端打开既有批次任务并进入执行页。
