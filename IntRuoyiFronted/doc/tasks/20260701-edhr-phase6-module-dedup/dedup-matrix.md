# eDHR Phase 6 模块去重矩阵


## 判定规则

- `保留`：仍是主流程、执行填写、列表入口或明确专业能力。
- `下沉/后台页`：不再作为主流程，但作为专业查询、规则配置或后台管理保留。
- `合并候选`：与批次详情页有明显职责重叠，需二次验证后再合并。
- `删除/隐藏高优先候选`：疑似演练/临时工具；必须证明无生产入口与无业务职责后才能删除。

## 路由矩阵

| 路由 | 标题 | 组件 | 当前角色 | 建议动作 | 证据/理由 |
|---|---|---|---|---|---|
| `pro/edhr-work-task` | 库区设置 | `@/views/mes/wm/warehouse/location/index.vue` | 工作任务 | 合并候选 | 可能与详情页任务明细重叠；需确认角色待办和执行入口后再决定。 |
| `pro/feedback/edhr-work-task` | eDHR 工作任务 | `@/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue` | 工作任务 | 合并候选 | 可能与详情页任务明细重叠；需确认角色待办和执行入口后再决定。 |
| `pro/feedback/edhr-execution/detail` | eDHR 执行详情 | `@/views/mes/pro/edhr/ExecutionPage.vue` | 执行详情/表单 | 保留 | 可能承载真实电子批记录填写，不可因详情页总控而删除。 |
| `pro/feedback/edhr-execution/form` | eDHR执行表单 | `@/views/mes/pro/edhr/ExecutionPage.vue` | 执行详情/表单 | 保留 | 可能承载真实电子批记录填写，不可因详情页总控而删除。 |
| `pro/feedback/edhr-domain-trace` | 主数据追溯 | `@/views/mes/pro/edhr/DomainTracePage.vue` | 域追溯 | 下沉 | Phase 4 已在详情页审计中心收口；专业页保留为后台查询。 |
| `pro/feedback/edhr-domain-trace/detail` | 主数据追溯详情 | `@/views/mes/pro/edhr/DomainTraceDetailPage.vue` | 域追溯 | 下沉 | Phase 4 已在详情页审计中心收口；专业页保留为后台查询。 |
| `pro/feedback/edhr-tracking` | eDHR执行追踪 | `@/views/mes/pro/edhr/TrackingPage.vue` | 执行追踪 | 下沉候选 | 详情页已有流程/历史入口，追踪页可作为专业页保留。 |
| `pro/feedback/edhr-field-audit` | 字段审计链 | `@/views/mes/pro/edhr/FieldAuditPage.vue` | 字段审计 | 下沉 | Phase 4 已在详情页审计中心收口；专业页保留为后台查询。 |
| `pro/feedback/edhr-field-audit/detail` | 字段审计详情 | `@/views/mes/pro/edhr/FieldAuditDetailPage.vue` | 字段审计 | 下沉 | Phase 4 已在详情页审计中心收口；专业页保留为后台查询。 |
| `pro/feedback/edhr-change` | eDHR变更记录 | `@/views/mes/pro/edhr/RecordChangePage.vue` | 变更记录 | 下沉 | 详情页已有变更/审计入口；专业页保留。 |
| `pro/feedback/edhr-operation-audit` | eDHR操作审计 | `@/views/mes/pro/edhr/OperationAuditPage.vue` | 操作审计 | 下沉 | Phase 4 已在详情页审计中心收口；专业页保留为后台查询。 |
| `pro/feedback/edhr-permission-matrix` | eDHR对象权限 | `@/views/mes/pro/edhr/PermissionMatrixPage.vue` | 权限矩阵 | 后台页 | Phase 5 明确属于管理后台工作区。 |
| `pro/feedback/edhr-approval/detail` | eDHR 审批详情 | `@/views/mes/pro/edhr/ApprovalDetailPage.vue` | 专业后台/周边能力 | 保留待复核 | 当前证据不足以证明可删除。 |
| `pro/feedback/edhr-batch-execution` | eDHR批次执行 | `@/views/mes/pro/edhr-batch/BatchExecutionListPage.vue` | 入口列表 | 保留 | 列表/创建/进入详情入口，不是主流程重复页。 |
| `pro/feedback/edhr-batch-execution/detail` | eDHR批次详情 | `@/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue` | 主流程页 | 保留 | Phase 1-5 指定批次详情页为唯一主流程页。 |
| `pro/feedback/edhr-batch-execution/review` | eDHR批次复盘 | `@/views/mes/pro/edhr-batch/BatchExecutionReviewPage.vue` | 复盘/时间线 | 合并候选 | 与详情页历史/时间线入口重叠；本轮先移除列表直达入口，避免成为主流程并行页，保留隐藏路由待二次合并验证。 |
| `pro/feedback/edhr-batch-execution/template` | eDHR批次模板 | `@/views/mes/pro/edhr-batch/BatchExecutionTemplatePage.vue` | 批次模板查看 | 下沉 | 本轮从列表主操作移除，改由批次详情页管理后台工作区进入。 |
| `pro/feedback/edhr-batch-execution/template-simulate` | eDHR模板模拟填写 | `@/views/mes/pro/edhr-batch/BatchExecutionTemplateSimulatePage.vue` | 模板模拟填写 | 暂不删除，后台/演练入口保留 | 既有任务记录证明该页曾作为真实 E2E 验证对象；当前不能证明无生产职责，禁止直接删除。 |
| `pro/feedback/edhr-batch-history` | 历史批记录 | `@/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue` | 专业后台/周边能力 | 保留待复核 | 当前证据不足以证明可删除。 |
| `pro/feedback/edhr-signatures` | eDHR签名记录 | `@/views/mes/pro/edhr/SignaturePage.vue` | 专业后台/周边能力 | 保留待复核 | 当前证据不足以证明可删除。 |
| `pro/feedback/edhr-signature` | eDHR签名记录 | `@/views/mes/pro/edhr/SignaturePage.vue` | 专业后台/周边能力 | 保留待复核 | 当前证据不足以证明可删除。 |
| `pro/feedback/edhr-recordbook` | eDHR记录本 | `@/views/mes/pro/edhr-recordbook/RecordbookPage.vue` | 记录本后台 | 后台页 | Phase 5 明确属于记录簿后台。 |
| `pro/edhr-recordbook` | eDHR记录本 | `@/views/mes/pro/edhr-recordbook/RecordbookPage.vue` | 记录本后台 | 后台页 | Phase 5 明确属于记录簿后台。 |
| `pro/feedback/edhr-label` | eDHR标签管理 | `@/views/mes/pro/edhr-label-print/LabelPrintQueuePage.vue` | 专业后台/周边能力 | 保留待复核 | 当前证据不足以证明可删除。 |
| `pro/feedback/edhr-print-task` | eDHR打印任务 | `@/views/mes/pro/edhr-label-print/LabelPrintQueuePage.vue` | 专业后台/周边能力 | 保留待复核 | 当前证据不足以证明可删除。 |
| `pro/feedback/edhr-unified-change` | eDHR统一变更 | `@/views/mes/pro/edhr-unified-change/UnifiedChangePage.vue` | 专业后台/周边能力 | 保留待复核 | 当前证据不足以证明可删除。 |
| `pro/feedback/edhr-flow-intervention` | eDHR流程干预 | `@/views/mes/pro/edhr-flow-intervention/FlowInterventionPage.vue` | 专业后台/周边能力 | 保留待复核 | 当前证据不足以证明可删除。 |
| `pro/feedback/edhr-dhr-template` | DHR模板目录 | `@/views/mes/pro/edhr-dhr-template/DhrTemplatePage.vue` | DHR模板目录 | 后台页 | Phase 5 明确属于模板后台。 |
| `pro/feedback/edhr-form` | eDHR独立表单 | `@/views/mes/pro/edhr-form/FormPage.vue` | 表单后台 | 后台页 | Phase 5 明确属于表单后台。 |
| `pro/feedback/edhr-form-template` | eDHR表单模板 | `@/views/mes/pro/edhr-form/FormPage.vue` | 表单后台 | 后台页 | Phase 5 明确属于表单后台。 |
| `pro/feedback/edhr-form-instance` | eDHR表单实例 | `@/views/mes/pro/edhr-form/FormPage.vue` | 表单后台 | 后台页 | Phase 5 明确属于表单后台。 |
| `pro/feedback/edhr-deployment-evidence` | eDHR部署证据 | `@/views/mes/pro/edhr-deployment/DeploymentPage.vue` | 专业后台/周边能力 | 保留待复核 | 当前证据不足以证明可删除。 |

## 优先级结论

1. `eDHR模板模拟填写` 不是本轮安全删除对象：历史任务和页面入口证明仍有演练/模板验证职责，后续只能在明确产品确认后删除或权限隐藏。
2. 本轮先处理 `eDHR批次复盘` 与 `eDHR批次模板` 的主流程重复入口：从列表主操作移除，统一经详情页进入详情、历史/时间线和管理后台工作区。
3. `eDHR放行管理`、审计三页、权限矩阵、DHR模板、表单、记录簿：不要删除，统一作为后台/专业页下沉。
4. `eDHR执行详情/表单` 不删除：它可能仍是实际填写页。
5. `eDHR工作任务`、`eDHR初始化批次` 进入下一轮二次验证，不能直接删。
