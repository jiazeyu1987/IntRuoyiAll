# 验证报告：修复批记录模板填写规则误报

## Summary

- 已新增保存边界回归用例，覆盖保存接口收到 `source=AUTO` 且 `reviewed=true` 的规则时必须按人工确认落库。
- 已实施最小修复：已确认规则持久化时将误带的 `AUTO` 来源规范化为 `MANUAL`。
- 已优化 BDD/TDD 根治设计，范围限定为保存边界归一化和执行层现有 fail fast 校验；历史异常 JSON 的 dry run/apply 明确拆为后续受控任务。
- 文档已明确自动识别不等于可执行确认，自动建议在确认前仍应被打开填写校验拦截。
- 关键 Maven 验证已通过，覆盖保存归一化、保存输出被执行规则识别、已确认规则快照冻结和未确认规则 fail fast。
- 相关回归集有一项与本次修改无直接关系的损耗报告 Word 解析断言失败；真实前端 E2E 仍因登录前置条件缺失而未执行。

## Commands

- `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root doc\tasks\fix-batch-record-fill-rule`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\fix-batch-record-fill-rule\bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\fix-batch-record-fill-rule\backend-api-evidence.md`
- `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordReportServiceImplDbTest#saveCellRules_normalizesReviewedAutoSuggestionToManualConfirmation test`
- `mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordReportServiceImplDbTest#saveCellRules_normalizesReviewedAutoSuggestionToManualConfirmation' '-Dmaven.compiler.testIncludes=cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordReportServiceImplDbTest.java' test`
- `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordCellRuleSupportTest#toRuleJson_normalizesReviewedAutoSuggestionToExecutableManualRule test`
- `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordReportServiceImplDbTest#saveCellRules_normalizesReviewedAutoSuggestionToManualConfirmation,MesProBatchRecordExecutionServiceImplTest#openOrCreateByContext_freezesReviewedNumberAndDateCellRulesIntoExecutionSnapshot+openOrCreateByContext_unreviewedFillableCellRule_mustFailFastWithoutCreatingExecution,MesProBatchRecordCellRuleSupportTest#toRuleJson_normalizesReviewedAutoSuggestionToExecutableManualRule+applyAutomaticSuggestions_setsRulesAfterWordImportWithoutMarkingUserReviewed test`
- `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordCellRuleSupportTest,MesProBatchRecordReportServiceImplDbTest test`

## Result

- PASS：BDD/TDD acceptance 文档结构校验通过。
- PASS：bug regression evidence 文档结构校验通过。
- PASS：backend API evidence 文档结构校验通过。
- PASS：review 优化后复跑文档结构校验通过。
- PASS：保存输出到执行识别支持层回归通过，1 个测试通过。
- PASS：关键真实服务路径回归通过，5 个测试通过。
- FAIL：规则支持层与报表服务回归集共 129 个测试，其中 1 个损耗报告 Word 解析断言失败。
- BLOCKED：真实前端 E2E 未执行，浏览器会话登录超时且未提供任务专用测试账号。

## Remaining Blockers

- 严格 RED 未在修复前运行取得，不能补记为已完成。
- `MesProBatchRecordReportServiceImplDbTest.uploadExtraFormSlot_whenLossReportWordHasMergedBody_expandsAllFillableFieldsAndDoesNotReuseOldHashReport` 期望 `□报废`，实际 `报废`；该失败与本次 `source/reviewed` 归一化无直接调用关系，未修改其相关并发代码。
- 单独复现上述失败时，`MesProRouteFlowConfigServiceImpl` 新增的 `resolveRecordbookEnabled` 调用缺少对应 helper，主代码无法编译；该文件已有未提交改动，不属于本任务。
- 真实 E2E 缺少可用登录会话与任务专用测试账号；本地 `8081`、`48081` 均已监听，但不得使用共享或生产账号替代。
