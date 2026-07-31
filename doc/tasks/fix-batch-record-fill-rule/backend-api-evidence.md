# Backend API Evidence

## Scope

本次后端行为范围为批记录模板单元格规则保存、执行前规则识别，以及批次执行 `/task/open` 的任务上下文校验。不新增接口、不修改权限、不修改数据库结构。规则修复入口是 `MesProBatchRecordCellRuleSupport.toRuleJson`；打开任务修复入口是 `MesProEdhrBatchExecutionServiceImpl.openTask` 的上下文校验。

## Contract

数据契约为：自动建议必须持久化为 `source=AUTO && reviewed=false`；用户确认后的规则必须持久化为 `source=MANUAL && reviewed=true`。当保存请求错误携带 `source=AUTO && reviewed=true` 时，后端归一化为人工确认态。未确认、来源为 `AUTO` 或值类型不支持的规则仍不得通过执行校验。权限、认证、路由和外部服务契约保持不变。传统批记录任务必须具备 `executionId + batchRecordReportId`；Form Center 路线表单任务必须具备完整 `formCenterInstanceId + formTemplateId + formTemplateVersionId + formBindingKey`；`BATCH_SHARED` 任务缺少冻结执行仍 fail fast。

## Validation

保存规则时继续复用 `ensureManualFillForm`、`validateRule` 和 `toRuleJson`。打开填写时继续由 `validateConfirmedCellRules` 收集未确认坐标并抛出 `PRO_BATCH_RECORD_EXECUTION_CELL_RULE_UNREVIEWED`，不增加 fallback、不吞异常、不在执行阶段修复模板 JSON。

## BDD Scenarios

- BDD: 保存已确认自动建议 -> Given 规则保存请求包含 `source=AUTO && reviewed=true` / When 保存规则 / Then JSON 必须为 `source=MANUAL && reviewed=true` 且被执行规则识别为已确认。
- BDD: 未确认自动建议仍阻断 -> Given 模板规则为 `source=AUTO && reviewed=false` / When 打开填写 / Then 返回未确认坐标且不创建执行快照。
- BDD: 传统批记录任务打开 -> Given 批次任务已有 `executionId + batchRecordReportId` 且无 Form Center 字段 / When 填写人打开任务 / Then 返回真实 executionId 并进入执行页。

## TDD Evidence

- RED: `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordCellRuleSupportTest#toRuleJson_normalizesReviewedAutoSuggestionToExecutableManualRule test` -> 修复前预期 `source` 断言失败且 `isReviewedRule` 为 false；代码已存在时才恢复测试环境，因此未取得修复前运行态 RED，不能补记为已完成。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordCellRuleSupportTest#toRuleJson_normalizesReviewedAutoSuggestionToExecutableManualRule test` -> PASS，1 个测试通过。
- RED: `mvn -pl yudao-module-mes '-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_opensLegacyBatchRecordTaskWithFrozenExecutionWithoutFormCenterContext' test` -> FAIL，复现 `1040750412`。
- GREEN: `mvn -pl yudao-module-mes '-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_opensLegacyBatchRecordTaskWithFrozenExecutionWithoutFormCenterContext+openTask_requiresFrozenExecutionForBatchSharedTask' test` -> PASS，2 个测试通过。

## Verification

- `MesProBatchRecordReportServiceImplDbTest.saveCellRules_normalizesReviewedAutoSuggestionToManualConfirmation` 覆盖保存服务写回 JSON。
- `MesProBatchRecordCellRuleSupportTest.toRuleJson_normalizesReviewedAutoSuggestionToExecutableManualRule` 覆盖保存输出被现有执行识别规则认可。
- `MesProBatchRecordExecutionServiceImplTest.openOrCreateByContext_freezesReviewedNumberAndDateCellRulesIntoExecutionSnapshot` 覆盖已确认规则进入执行快照。
- `MesProBatchRecordExecutionServiceImplTest.openOrCreateByContext_unreviewedFillableCellRule_mustFailFastWithoutCreatingExecution` 覆盖未确认规则继续 fail fast。
- 组合命令运行上述关键服务路径与自动建议保护，共 5 个测试通过。
- `MesProBatchRecordCellRuleSupportTest,MesProBatchRecordReportServiceImplDbTest` 回归集运行 129 个测试全部通过。
- 真实前端 E2E `node tests\e2e\edhr-batch-execution-real-flow.e2e.js` 通过，验证 `/task/open` 真实路径。

## Observability

本次不新增日志、指标或追踪字段。运行失败保持由现有错误码和 Maven 编译输出可见，不返回默认成功结果。

## Blockers

原始 `source/reviewed` 严格 RED 缺少修复前运行证据，作为历史证据缺口保留。当前授权范围内无剩余阻塞；历史模板 JSON dry run/apply 需另行授权。
