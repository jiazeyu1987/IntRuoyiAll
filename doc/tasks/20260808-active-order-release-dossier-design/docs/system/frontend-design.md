# 活跃订单放行资料前端设计

## Purpose and Scope

本文定义生产组长活跃订单池新增“申请放行”入口、状态展示、确认交互、错误处理和前端 API 契约。前端只负责呈现和触发，最终资格判断、生成和幂等由后端负责。

## Evidence Reviewed

- 页面：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`。
- API：`IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts`。
- 当前活跃订单行已有 `id`、`workOrderCode`、产品、数量、异常、生产进度、检验进度和操作列。
- 当前页面已使用 Element Plus、`UnifiedListTemplate`、`ElMessage`、行内按钮、loading 和错误消息模式。

## Pages and Routes

- 入口页面保持在生产组长工作台活跃订单池，不新增顶级路由。
- 操作位置：活跃订单表格操作列，在“移除”“异常上报”附近增加“申请放行”。
- 成功后可提供两个跳转入口：
  - eDHR 批次执行详情：使用后端返回的 `batchExecutionId`。
  - 负责人待办或放行事务详情：使用后端返回的 `releaseTransactionId` / `releaseApprovalWorkTaskId`。
- 若当前前端没有稳定详情路由，第一版至少展示生成结果对话框和可复制的正式 ID，不伪造跳转。

## Components

- 活跃订单行按钮：
  - 文案：`申请放行`。
  - `data-team-leader-active-order-release-apply` 作为静态合同定位点。
  - 行 loading 独立于列表 loading，避免一个订单申请时锁死整表。
- 确认弹框：
  - 标题：`申请生成放行资料`。
  - 内容展示订单号、产品、批次/批号、生产进度、检验进度、将生成资料类型、发送对象。
  - 明确提示“该操作不会直接放行，生产负责人仍需审核签名”。
- 阻塞项抽屉或对话框：
  - 展示后端返回的 `blockers[]`，包括阻塞类型、业务对象、原因和建议处理入口。
  - 不在前端合成阻塞结论。
- 生成结果摘要：
  - 展示正式批次执行、批记录、过程检验单、损耗单、放行事务和负责人待办的 ID 或名称。

## State and Data Flow

- 复用当前 `TeamLeaderActiveOrderRespVO` 放行字段；首版以已有字段为准：
  - `releaseApplicationStatus?: string`
  - `releaseApplicationBlockerSummary?: string`
  - `releaseApprovalWorkTaskId?: number`
  - 若需要 `releaseApplicationId`、`batchExecutionId`、`releaseTransactionId` 或 `releaseAppliedAt`，必须先在 M0 契约冻结中同步后端 VO、前端 TS 和静态合同。
- 复用当前 API 文件能力：
  - `TeamLeaderActiveOrderReleaseApplyReqVO`：`activeOrderId`、必填 `idempotencyKey`、`applyRemark?`。
  - `TeamLeaderActiveOrderReleaseApplyRespVO`：申请状态、批次/放行/待办 ID、`dossierSummary.sourceSnapshotHash`、`blockers[]`。
  - `TeamLeaderActiveOrderReleaseBlockerRespVO`：首版使用 `blockerType`、`objectType`、`objectId`、`objectCode`、`reason`、`suggestion`。
  - `applyTeamLeaderActiveOrderRelease(data)`：`POST /mes/pro/process-pool/team-leader/active-order/release/apply`。
- 前端可点击条件只做提示：
  - `Number(row.productionProgressPercent) >= 100`
  - `Number(row.inspectionProgressPercent) >= 100`
  - `!row.abnormal`
  - 后端返回状态不是 `PENDING_RELEASE_APPROVAL` 或 `RELEASED`
- 点击后生成客户端幂等键：
  - 建议格式：`active-order-release:<activeOrderId>:<timestamp-or-uuid>`。
  - 若同一次确认框内重试，复用同一个幂等键。
- 成功后刷新活跃订单列表并显示结果摘要。
- 后端返回阻塞时，前端不清空列表，直接展示阻塞项并刷新行状态。

## Error States

- 进度不足：按钮禁用，tooltip 展示最新原因。
- 后端资格失败：显示后端错误消息，刷新该行数据。
- 阻塞项返回：打开阻塞项抽屉，保留每个 blocker 的对象名称、类型和处理建议。
- 网络失败或超时：保留确认框重试按钮，重试复用同一幂等键；不宣称失败写入一定未发生。
- 权限不足：显示“当前账号无权申请该活跃订单放行”，不隐藏后端错误。
- 负责人待办已存在：显示“已申请，等待负责人放行”，并展示待办入口。

## Accessibility and Responsive Behavior

- 按钮禁用原因必须通过 tooltip 或相邻说明可读取，不能只靠颜色。
- 确认框和阻塞项抽屉支持键盘关闭和确认。
- 小屏下操作按钮可折叠为下拉，但“申请放行”不能被隐藏到不可发现位置。
- 表格加载时保持当前页和筛选条件，申请后刷新不强制跳回第一页。

## Open Questions

- OQ-01 成功后是否已有稳定 eDHR 批次详情路由可直接跳转，需要在实现前核对路由表。
- OQ-02 申请备注是否第一版必填；若业务要求电子签名申请，前端需增加签名密码输入。
- OQ-03 阻塞项展示是否要按“批记录/过程检验/损耗/负责人/权限”分组。

## Design Blockers

- DB-01 后端未返回放行申请状态字段时，前端无法可靠展示行状态。
- DB-02 未确认详情路由时，前端不能制造不可用跳转。
- DB-03 未确认签名要求时，前端不能提前加入签名密码并形成错误业务含义。
