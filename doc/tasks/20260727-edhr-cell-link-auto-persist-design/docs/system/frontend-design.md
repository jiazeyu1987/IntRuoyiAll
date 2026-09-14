# Frontend Design

## Purpose and Scope

前端设计目标是配合后端自动落库后的新语义：执行页、批次详情只读预览和链接配置页都以数据库已保存的 `cellValuesJson` 为准。前端不再把本地 draft hydrate 的预填值当作已保存结果，也不通过隐藏保存动作绕过字段审计。

范围包含 eDHR 执行页、批次任务打开响应、只读预览、错误展示和静态契约调整。不包含本阶段 UI 代码实现。

## Evidence Reviewed

- `ExecutionPage.vue` 当前在 DRAFT 且非只读跟踪模式时调用 `BatchRecordCellLinkApi.getPrefill(currentExecutionId, workTaskId.value)`。
- `ExecutionPage.vue#hydrateDraftState` 会在目标单元格未保存值时，把 `prefills` 写入本地 `draftFieldValues`，但不会落库。
- `EdhrExecutionReadonlyForm.vue` 解析 `props.formViewModel?.cellValuesJson` 生成只读展示值，因此只显示已保存值。
- 批次详情右侧卡片和截图中的只读预览是用户观察问题的主要入口，当前空值说明后端未保存目标格。
- 链接配置页已能显示生产工单来源字段和目标单元格关系，配置链路不是本次空值根因。

## Pages and Routes

- eDHR 批次详情页：打开工序任务时调用 `POST /mes/pro/edhr-batch-execution/task/open`，新响应可展示自动落库摘要或阻断错误。
- eDHR 执行填写页：加载执行详情后以 `detail.cellValues` 和 `cellValuesJson` 为准；后端落库成功后目标单元格应像普通已保存值一样显示。
- eDHR 批次详情只读预览：继续通过 `formViewModel.cellValuesJson` 渲染，不增加前端预填兜底。
- 批记录单元格链接配置页：保留当前建立链接流程；可在未来增加“自动落库说明”和冲突状态说明，但不是阶段一必须。

## Components

- `ExecutionPage.vue`
  - 保留已保存值优先逻辑。
  - 实现后应移除或降级当前“仅前端 hydrate prefills”的保存语义，避免用户看到未保存值误以为已落库。
  - 可保留 `/prefill` 调用用于显示“可预填但尚未落库”的诊断，但不能把它作为正式值来源。
- `EdhrExecutionReadonlyForm.vue`
  - 不做前端兜底；继续只读展示 `cellValuesJson`。
  - 若后端返回自动落库阻断，父页面应展示错误，不渲染空值为成功。
- `BatchExecutionTaskCard` 或批次详情任务区
  - 可展示 `cellLinkAutoPersist.appliedCount` 和冲突数量，帮助用户理解打开任务时已自动保存哪些链接值。
  - 对 `SOURCE_VALUE_MISSING` 展示“生产工单生产批号为空，无法自动预填目标单元格”的明确文案。
- `BatchRecordCellLinkConfig`
  - 不改变现有交互。未来可在建立链接后提示“值将在创建/打开执行记录时由后端自动落库”。

## State and Data Flow

目标数据流：

1. 用户在批次详情点击工序任务打开。
2. 前端调用 `POST /mes/pro/edhr-batch-execution/task/open`，携带 `batchExecutionId`、`taskId`、`workTaskId`。
3. 后端创建或绑定执行记录，并执行单元格链接自动落库。
4. 后端返回 `executionId`、`executionPageQuery` 和可选 `cellLinkAutoPersist` 摘要。
5. 前端进入执行页并调用执行详情接口。
6. 执行详情返回的 `cellValues` 已包含自动落库值；`hydrateDraftState` 按已保存值填充 baseline 和 draft。
7. 批次详情只读预览再次加载时，`formViewModel.cellValuesJson` 已包含同一值。

前端原则：

- 已保存值优先于任何预填提示。
- 自动落库失败不在前端补默认值。
- 目标已有人工值时显示冲突或保持原值，不提示“已覆盖”。
- 重复打开时不出现重复 toast；仅在有新 `APPLIED` 或阻断时提示。

## Error States

- `SOURCE_VALUE_MISSING`：显示阻断错误，说明来源字段、目标表单和目标单元格；不进入“保存成功”提示。
- `TARGET_ALREADY_MANUAL`：非阻断信息，说明目标已有值且按规则不覆盖。
- `SOURCE_EXECUTION_MISSING`：跨表来源尚未产生时显示显式冲突；生产工单来源不应出现该状态。
- `FIELD_AUDIT_CHAIN_CONFLICT`：显示字段审计链冲突，提示刷新后重试；不自动重试写库。
- `RULE_NOT_APPLICABLE` 或模板字段缺失：显示配置错误，引导检查单元格链接和批记录版本，不隐藏错误。
- 网络或 500 错误：沿用现有全局错误处理，但不能额外调用 `/prefill` 做前端兜底。

## Accessibility and Responsive Behavior

- 自动落库摘要或冲突提示应使用现有 Element Plus Alert/Message 体系，提供可读文本，不仅依赖颜色。
- 错误文案需包含来源字段中文名“生产批号”和目标表单“粗洗工序生产记录”，便于现场人员定位。
- 移动或窄屏下提示应出现在任务打开区或执行页顶部，不遮挡表格核心输入区。
- 只读预览不新增交互控件，因此无额外键盘操作负担。

## Open Questions

- 是否需要在执行页顶部长期展示“系统已根据链接自动预填 N 个单元格”，还是仅首次打开 toast 提示。
- 是否需要在链接配置页增加规则风险提示，例如“来源为空将阻断打开执行记录”。
- 当前 `/prefill` 诊断接口是否继续保留给配置调试，还是在实现后只用于后台校验。

## Design Blockers

- 前端实现前必须确认后端响应字段名和状态枚举，避免静态契约和真实接口不一致。
- 不能在后端未落库前，仅靠前端 `hydrateDraftState` 显示目标值来宣布问题解决。
