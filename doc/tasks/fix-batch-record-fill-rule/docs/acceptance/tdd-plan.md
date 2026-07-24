# TDD Plan

## Purpose and Scope

本计划将根治设计拆成严格 TDD 顺序：先用失败用例证明 `source=AUTO && reviewed=true` 的错误持久化会导致后续执行层不认可，再实施最小保存边界修复。历史修复只作为后续受控任务计划，不计入当前最小修复的完成证据。范围不包含绕过执行校验，也不包含新增冗余规则表或重复 JSON 解析框架。

## Evidence Reviewed

- 现有 `MesProBatchRecordReportServiceImplDbTest.getAndSaveCellRules_suggestsAndPersistsReviewedTypedMetadata` 已覆盖规则保存主路径。
- 现有 `MesProBatchRecordExecutionServiceImplTest.openOrCreateByContext_unreviewedFillableCellRule_mustFailFastWithoutCreatingExecution` 已覆盖未确认规则的阻断。
- 现有 `MesProBatchRecordExecutionServiceImplTest.openOrCreateByContext_freezesReviewedNumberAndDateCellRulesIntoExecutionSnapshot` 已覆盖已确认规则进入执行快照。
- 现有 `MesProBatchRecordCellRuleSupportTest.applyAutomaticSuggestions_setsRulesAfterWordImportWithoutMarkingUserReviewed` 已覆盖自动建议不能自动确认。

## TDD Sequence

1. 保存边界 RED：新增 `saveCellRules_normalizesReviewedAutoSuggestionToManualConfirmation`，构造 `source=AUTO && reviewed=true` 的保存请求，断言写回 JSON 中 `source=MANUAL`。
2. 保存边界 GREEN：在 `MesProBatchRecordCellRuleSupport.toRuleJson` 内增加持久化来源归一化 helper，只改变已确认规则的持久化来源。
3. 保存到执行识别 RED：在 `MesProBatchRecordCellRuleSupportTest` 中将保存后的 JSON 交给 `isReviewedRule`，断言 `source=AUTO && reviewed=true` 输入必须被归一化并被执行层识别为确认规则。
4. 保存到执行识别 GREEN：如第 2 步已覆盖根因，该支持层组合测试应通过；执行服务的既有快照测试继续验证 `MANUAL && reviewed=true` 规则可进入快照。若失败，只允许修正保存输出或规则状态不变量，不允许放松 `isReviewedRule`。
5. 自动建议保护 RED：覆盖 `applyAutomaticSuggestions` 生成的 `source=AUTO && reviewed=false` 仍会被执行层视为未确认。
6. 自动建议保护 GREEN：不改执行校验和建议生成语义，确保自动识别不等于自动确认。

## RED Commands

- `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordReportServiceImplDbTest#saveCellRules_normalizesReviewedAutoSuggestionToManualConfirmation test`
- `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordCellRuleSupportTest#toRuleJson_normalizesReviewedAutoSuggestionToExecutableManualRule test`
- `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordExecutionServiceImplTest#openOrCreateByContext_unreviewedFillableCellRule_mustFailFastWithoutCreatingExecution test`

## Expected Failures

- 保存边界修复前，第一条 RED 应失败在 JSON 断言：实际 `source` 仍为 `AUTO`，或返回未确认数量仍包含该单元格。
- 保存到执行识别修复前，第二条 RED 应失败在 `source` 仍为 `AUTO` 且 `isReviewedRule` 返回 false。
- 自动建议保护若被破坏，第三条 RED 应失败为执行层未继续返回未确认坐标，说明系统错误地把建议当成确认。

## GREEN Commands

- `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordReportServiceImplDbTest#saveCellRules_normalizesReviewedAutoSuggestionToManualConfirmation test`
- `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordCellRuleSupportTest#toRuleJson_normalizesReviewedAutoSuggestionToExecutableManualRule test`
- `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordCellRuleSupportTest,MesProBatchRecordReportServiceImplDbTest test`

## Refactor Checks

- `isReviewedRule` 不得放宽为接受 `AUTO` 来源。
- `applyAutomaticSuggestions` 不得生成 `reviewed=true`。
- `validateConfirmedCellRules` 不得增加临时修复、fallback 或异常吞掉逻辑。
- 历史修复不得直接拼接 JSON 字符串，必须复用现有 JSON 对象、规则校验和网关写回。

## Evidence Log Template

- `BDD: <scenario name> -> Given ... / When ... / Then ...`
- `RED: <command> -> FAIL, <expected reason>`
- `GREEN: <command> -> PASS`
- `BLOCKED: <command> -> FAIL, <unrelated compile or environment blocker>`

## Test Blockers

- 关键 Maven GREEN 已通过，当前新增测试已获得运行态 PASS。
- 严格 RED 未在修复前运行取得；代码已存在时才恢复测试环境，不能补记为已完成。
- 回归集有一项范围外损耗报告 Word 解析断言失败，需其所属任务确认或修复。
- 真实 E2E 缺少有效登录会话和任务专用账号，当前不能执行写型验证。

## Follow-up Historical Repair Sequence

1. 历史修复 RED：为计划中的修复服务增加 dry run 测试，输入含 `source=AUTO && reviewed=true` 的 JSON，断言输出候选坐标且不写回。
2. 历史修复 GREEN：实现显式 apply 路径，复用 `toRuleVO`、`validateRule`、`toRuleJson` 和 `jimuReportGateway.updateReportJson`，断言只修改用户确认范围内的候选单元格。
3. 历史修复前置检查：验证 dry run 不会先触发 `getCellRules` 读时写回，或在该后续任务中正式调整该读路径行为。
