# Backend API Evidence

## Scope

加固 MES 批记录 `/cell-rules` 规则保存契约，支持下拉框 options、数字上下限和签名 marker fail-fast。

## API Contract

- Request: `BatchRecordReportCellRulesReqVO.rules[].constraints`
- Response: `BatchRecordReportCellRulesRespVO.rules/suggestions/sheetLayoutJson`

## Validation Behavior

- 下拉框必须是 `valueType=STRING` 且 `selectionMode=single`，至少两个有效 options。
- 数字 `min/max` 必须是数字，且 `min <= max`。
- 签名 `valueType=SIGNATURE` 必须对应 enabled `edhrSignature` marker。

## BDD

见 `execution-log.md`。

## RED

- 新增后端测试覆盖 `select` options 同步和 NUMBER min/max 反向约束；实现前无法满足后端持久化语义。

## GREEN

- `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellRuleSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- 结果：`Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
