# Test Data

## Purpose and Scope

本文件定义 BDD/TDD/E2E 所需的最小测试数据。测试数据只服务于“已确认规则保存后误报未确认”的修复，优先复用现有单元测试 fixture、Jimu 报表 JSON 网关 mock、规则支持类和执行服务测试基座，不新增长期业务样例表。

## Evidence Reviewed

- `sampleCellRuleReportJson()` 已可构造含可填单元格的 Jimu JSON。
- `sampleEditableReportJsonWithoutRules()` 已可构造执行层未确认失败场景。
- `sampleEditableReportJsonWithReviewedNumberAndDateRules()` 已可构造执行层确认成功场景。
- `TestBatchRecordFixtures.metadataReport` 已可构造任务内报表元数据。

## Required Test Data

- 保存边界测试：报表 ID `cell-rule-report-auto-reviewed`，一个空白可填单元格，保存请求中该单元格规则为 `NUMBER`、`componentFlag=input-number`、`source=AUTO`、`reviewed=true`。
- 执行链路测试：任务专用工单、工序、报表元数据、通过保存接口生成的 Jimu JSON，以及批次号 `BATCH-RULE-FROZEN` 类型的隔离测试批次。
- 未确认失败测试：含 `fillForm` 但缺少有效 `edhrCellRule` 的 JSON，预期错误坐标包括第 1 行第 2 列和第 1 行第 4 列。
- 自动建议保护测试：含 `source=AUTO && reviewed=false` 或缺少有效 `edhrCellRule` 的 JSON，验证打开填写仍 fail fast。
- 历史修复后续测试：含 `source=AUTO && reviewed=true` 的 JSON，同时包含一个正常 `source=MANUAL && reviewed=true` 的规则；该数据只在后续用户授权的历史修复任务中使用。

## Reset Procedure

- 单元测试和服务测试使用现有 DbUnit/Mockito 生命周期，测试完成后由测试框架回滚或清理上下文。
- E2E 数据必须使用任务专用编码前缀，不复用生产模板或共享基线模板。
- 历史修复不属于当前任务写入范围；若后续 apply，必须先保存 dry run 报告；如需要回滚，只能基于执行前备份的 Jimu JSON 明确恢复指定报表。

## Data Ownership

- 单元测试数据归本任务所有，位于当前测试类 fixture 内。
- E2E 数据必须归当前任务所有，命名中包含任务 ID `fix-batch-record-fill-rule` 或等价前缀。
- 历史修复数据归用户指定环境和指定报表所有，未获得明确授权前不得写入。

## Test Blockers

- 当前 Maven 编译阻塞导致新增测试无法运行到断言阶段。
- 当前未确认真实 E2E 账号、租户、模板和端口状态，因此不能执行写型 E2E。
