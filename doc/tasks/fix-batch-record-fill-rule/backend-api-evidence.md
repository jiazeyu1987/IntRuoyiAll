# Backend API Evidence

## Scope

本次后端行为范围为批记录模板单元格规则保存与执行前规则识别，不新增接口、不修改权限、不修改数据库结构。修复入口是 `MesProBatchRecordCellRuleSupport.toRuleJson`，保存服务 `MesProBatchRecordReportServiceImpl.saveCellRules` 继续通过现有 Jimu JSON 网关持久化，执行服务继续使用既有 `isReviewedRule` 和 fail fast 校验。

## Contract

数据契约为：自动建议必须持久化为 `source=AUTO && reviewed=false`；用户确认后的规则必须持久化为 `source=MANUAL && reviewed=true`。当保存请求错误携带 `source=AUTO && reviewed=true` 时，后端归一化为人工确认态。未确认、来源为 `AUTO` 或值类型不支持的规则仍不得通过执行校验。权限、认证、路由和外部服务契约保持不变。

## Validation

保存规则时继续复用 `ensureManualFillForm`、`validateRule` 和 `toRuleJson`。打开填写时继续由 `validateConfirmedCellRules` 收集未确认坐标并抛出 `PRO_BATCH_RECORD_EXECUTION_CELL_RULE_UNREVIEWED`，不增加 fallback、不吞异常、不在执行阶段修复模板 JSON。

## BDD Scenarios

- BDD: 保存已确认自动建议 -> Given 规则保存请求包含 `source=AUTO && reviewed=true` / When 保存规则 / Then JSON 必须为 `source=MANUAL && reviewed=true` 且被执行规则识别为已确认。
- BDD: 未确认自动建议仍阻断 -> Given 模板规则为 `source=AUTO && reviewed=false` / When 打开填写 / Then 返回未确认坐标且不创建执行快照。

## TDD Evidence

- RED: `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordCellRuleSupportTest#toRuleJson_normalizesReviewedAutoSuggestionToExecutableManualRule test` -> 修复前预期 `source` 断言失败且 `isReviewedRule` 为 false；代码已存在时才恢复测试环境，因此未取得修复前运行态 RED，不能补记为已完成。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordCellRuleSupportTest#toRuleJson_normalizesReviewedAutoSuggestionToExecutableManualRule test` -> PASS，1 个测试通过。

## Verification

- `MesProBatchRecordReportServiceImplDbTest.saveCellRules_normalizesReviewedAutoSuggestionToManualConfirmation` 覆盖保存服务写回 JSON。
- `MesProBatchRecordCellRuleSupportTest.toRuleJson_normalizesReviewedAutoSuggestionToExecutableManualRule` 覆盖保存输出被现有执行识别规则认可。
- `MesProBatchRecordExecutionServiceImplTest.openOrCreateByContext_freezesReviewedNumberAndDateCellRulesIntoExecutionSnapshot` 覆盖已确认规则进入执行快照。
- `MesProBatchRecordExecutionServiceImplTest.openOrCreateByContext_unreviewedFillableCellRule_mustFailFastWithoutCreatingExecution` 覆盖未确认规则继续 fail fast。
- 组合命令运行上述关键服务路径与自动建议保护，共 5 个测试通过。
- `MesProBatchRecordCellRuleSupportTest,MesProBatchRecordReportServiceImplDbTest` 回归集运行 129 个测试，发现 1 个范围外损耗报告 Word 解析断言失败。

## Observability

本次不新增日志、指标或追踪字段。运行失败保持由现有错误码和 Maven 编译输出可见，不返回默认成功结果。

## Blockers

严格 RED 缺少修复前运行证据。相关回归集存在 1 个范围外失败：损耗报告 Word 解析期望 `□报废`，实际 `报废`。后续单独复现时，范围外的 `MesProRouteFlowConfigServiceImpl.resolveRecordbookEnabled` 调用又缺少 helper 而导致主代码编译失败。本地前端和后端均在运行，但浏览器登录会话超时且未提供任务专用测试账号，真实 E2E 未执行。
