# BDD Scenarios

## Purpose and Scope

本文件定义“批次执行打开填写误报未确认填写规则”的可观察行为。当前任务范围覆盖批记录模板填写规则的保存语义和执行打开前校验；历史异常 JSON 只保留后续受控修复场景，不纳入当前最小修复完成验收。设计要求复用现有 `MesProBatchRecordCellRuleSupport`、`MesProBatchRecordReportServiceImpl.saveCellRules`、Jimu 报表 JSON 网关和执行快照校验，不新增并行规则系统。

## Evidence Reviewed

- `MesProBatchRecordCellRuleSupport.isReviewedRule`：执行层认可的规则条件为 `reviewed=true`、`source` 非 `AUTO`、`valueType` 受支持。
- `MesProBatchRecordReportServiceImpl.saveCellRules`：规则配置页保存入口，负责写回 Jimu JSON。
- `MesProBatchRecordExecutionServiceImpl.validateConfirmedCellRules`：打开填写前的 fail fast 校验入口。
- `MesProBatchRecordCellRuleSupport.applyAutomaticSuggestions`：自动建议生成入口，当前应保持 `reviewed=false`。
- 当前用户报错坐标清单：证明模板内有大量可填单元格被执行层识别为未确认。
- `MesProBatchRecordReportServiceImpl.getCellRules`：当前存在读取时把 `source=AUTO && reviewed=true` 写回未确认的保护行为，历史修复前必须避免把它误当成 dry run。

## Feature Scenarios

Scenario: 保存已确认的自动建议时归一化为人工确认

Given 规则配置页提交一个可填单元格规则，规则包含 `source=AUTO` 且 `reviewed=true`

When 后端执行 `saveCellRules` 并写回 Jimu 报表 JSON

Then 该单元格持久化后的 `edhrCellRule.source` 必须为 `MANUAL`

And `edhrCellRule.reviewed` 必须保持为 `true`

And 返回结果中的未确认可填单元格数量不应包含该单元格

Scenario: 打开填写时接受已归一化的确认规则

Given 批记录模板 JSON 中可填单元格包含 `source=MANUAL`、`reviewed=true` 且 `valueType` 受支持的规则

When 用户在批次执行中点击打开填写

Then 系统应构建执行快照

And 快照字段应包含对应单元格规则

And 系统不应提示该单元格为未确认填写规则

## Failure Scenarios

Scenario: 真正未确认的可填单元格仍必须阻断打开填写

Given 模板 JSON 中存在可填单元格但没有规则，或规则为 `source=AUTO` 且 `reviewed=false`

When 用户点击打开填写

Then 系统必须抛出未确认填写规则错误

And 错误信息必须包含具体行列坐标

And 不得创建执行记录或执行快照

Scenario: 自动识别但未确认的规则不得直接打开填写

Given 模板生成或 Word 导入自动识别出可填单元格，并生成 `source=AUTO` 且 `reviewed=false` 的建议规则

When 用户未在规则配置页确认这些建议就点击打开填写

Then 系统必须继续提示未确认填写规则坐标

And 不得把自动建议当作已确认规则写入执行快照

Scenario: 不支持的规则类型不能通过归一化绕过

Given 规则包含 `reviewed=true`，但 `valueType` 不在现有支持范围内

When 后端保存规则或构建执行快照

Then 系统必须按现有规则校验失败

And 不得把该规则当作已确认规则写入可执行快照

## Boundary Scenarios

Scenario: 空来源的已确认规则默认按人工确认持久化

Given 保存入参中规则 `reviewed=true` 且 `source` 为空

When 后端序列化规则 JSON

Then `source` 应保存为 `MANUAL`

Scenario: 小写 auto 的已确认规则同样归一化

Given 保存入参中规则 `reviewed=true` 且 `source=auto`

When 后端序列化规则 JSON

Then `source` 应保存为 `MANUAL`

Scenario: 自动建议不因生成而自动确认

Given Word 导入或模板生成时自动识别出可填单元格

When 系统调用自动建议生成逻辑

Then 自动建议应保持 `source=AUTO` 且 `reviewed=false`

And 执行打开前仍要求用户确认或显式修复

## Open Questions

- 当前设计阶段不执行历史数据写入；历史修复 apply 前需要用户明确确认目标报表 ID、环境和备份状态。
- 若后续启动历史修复任务，必须先决定如何处理 `getCellRules` 读取时写回行为，避免 dry run 候选状态被提前改写。
- 如果需要把历史修复暴露为管理端入口，应优先挂接现有批记录模板管理能力，不新建独立规则管理模块。

## Follow-up Scenarios

Scenario: 历史异常规则通过显式修复后可打开填写

Given 某个历史模板存在 `source=AUTO` 且 `reviewed=true` 的异常规则

When 运维或管理员在不触发规则页读时写回的前提下执行 dry run，并对指定报表明确执行修复

Then 修复逻辑应把经校验的候选规则写回为 `source=MANUAL` 且 `reviewed=true`

And 再次打开填写时不应因这些已修复单元格报未确认

## Test Blockers

- 关键 Maven GREEN 已通过：保存规则归一化、保存输出被执行规则识别、已确认规则快照冻结、未确认规则 fail fast 与自动建议保护均有测试覆盖。
- 相关回归集仍有一项损耗报告 Word 解析断言失败，期望 `□报废`、实际 `报废`；该失败与本次规则来源归一化无直接关系，需其所属任务确认。
- 后续单独复现时，范围外 `MesProRouteFlowConfigServiceImpl.resolveRecordbookEnabled` 调用缺少 helper，当前构建状态不稳定。
- 真实 E2E 缺少有效登录会话和任务专用账号，当前不能执行写型验证。
