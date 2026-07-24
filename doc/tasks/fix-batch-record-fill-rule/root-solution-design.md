# 批记录填写规则误报根治设计

## 背景

批次执行点击“打开填写”时，执行快照构建会读取 Jimu 报表 JSON，并调用现有 `MesProBatchRecordCellRuleSupport.unreviewedFillableCoordinates` 做可填单元格规则确认校验。该校验只接受 `reviewed=true`、`source` 非 `AUTO`、且 `valueType` 受支持的规则。用户遇到的大量坐标提示，说明模板 JSON 内存在可填单元格，但系统未能识别为已确认规则。

## 根因

根因不是“打开填写”按钮本身，也不是执行层坐标枚举错误，而是规则状态语义不一致：前端或规则保存链路可能提交 `reviewed=true` 且 `source=AUTO` 的规则。保存后 JSON 表面看起来已确认，但执行层按现有安全规则仍把 `AUTO` 来源视为未确认建议，因此打开填写被 fail fast 拦截。

## 设计目标

- 保留执行层 fail fast，不绕过真正未确认或仅自动识别出的可填单元格。
- 在保存边界统一规则状态，确保“用户确认”和“自动建议”只有一种可解释状态。
- 复用现有 Jimu 报表 JSON、规则遍历、规则校验、规则序列化和执行快照构建链路。
- 对历史异常 JSON 只提供后续受控修复设计，不把历史修复混入当前最小代码修复。

## 规则状态不变量

- 自动建议：`source=AUTO` 且 `reviewed=false`，只能作为规则配置页建议，不能直接进入执行快照。
- 用户已确认：`source=MANUAL` 且 `reviewed=true`，可被执行层视为已确认规则。
- 非法混合态：`source=AUTO` 且 `reviewed=true`，不得继续新增；历史数据必须经过显式修复或重新确认。
- 执行层只消费已确认规则，不负责推断用户意图。

## 最小实现方案

1. 保存边界归一化：在 `MesProBatchRecordCellRuleSupport.toRuleJson` 内处理持久化状态；当入参 `reviewed=true` 且 `source=AUTO` 时，保存为 `source=MANUAL`。
2. 自动建议保持未确认：`applyAutomaticSuggestions` 仍产生 `source=AUTO` 且 `reviewed=false`，不改为自动通过。
3. 执行校验保持现状：`MesProBatchRecordExecutionServiceImpl.validateConfirmedCellRules` 继续调用 `unreviewedFillableCoordinates`，不增加兜底、不吞异常。
4. 规则页读取保护不作为本次根治依赖：当前 `getCellRules` 已存在把 `source=AUTO && reviewed=true` 写回 `reviewed=false` 的保护行为；它能防止误把建议当确认，但会改变历史异常证据。因此当前最小修复只依赖保存边界归一化，历史数据修复必须作为单独受控任务处理。

## 历史数据修复设计（后续受控任务）

历史数据存在两种可能：一种是用户已经在规则页确认过，但旧保存链路错误保留 `AUTO`；另一种是早期自动建议错误带了 `reviewed=true`。两者在 JSON 上不可完全区分，因此不能无条件静默批准。

历史修复不纳入当前最小修复的完成验收。若用户后续授权处理历史模板，应采用现有系统能力做显式修复，并先处理当前 `getCellRules` 读取时写回的证据破坏风险：

- 前置约束：dry run 必须直接通过 `MesProBatchRecordJimuReportGateway.getReportJson` 读取原始 Jimu JSON；执行 dry run 前不得先通过规则配置页触发 `getCellRules`，否则候选状态可能已被打回未确认。
- Dry run：读取指定报表或指定报表编码范围，复用 `MesProBatchRecordCellRuleSupport.forEachCell` 扫描 `source=AUTO && reviewed=true` 的单元格，输出报表、坐标、标签、值类型和校验状态，不写回 JSON。
- Apply：仅对用户明确确认的报表 ID 执行；每个候选单元格先复用 `toRuleVO` 和 `validateRule` 校验，再复用 `toRuleJson` 重新序列化为 `source=MANUAL && reviewed=true`，最后通过 `jimuReportGateway.updateReportJson` 写回。
- 不修复条件：规则缺失、`valueType` 不受支持、坐标不存在、JSON 不可解析、用户未确认作用范围时 fail fast。
- 不新增内容：不新增规则表、不复制 Jimu JSON DAO、不在执行打开时临时修复、不引入兼容分支。

## 验收边界

- 新保存的已确认规则不会再产生 `source=AUTO && reviewed=true`。
- 已确认规则打开填写时能进入执行快照。
- 真正未确认的可填单元格仍提示具体坐标并阻断打开填写。
- 当前任务不声明历史模板已被修复；历史修复必须有 dry run 证据和明确 apply 范围，不能自动修改未知模板。
