# eDHR 前端现有合约盘点

任务：`CR-P0-01 existing-edhr-contract`

基线：`codex/20260618-edhr-cr-p0-existing-contract`，从前端 `int_main@98d059c0e` 创建。

范围：本文件只锁定现有前端 API 适配层、页面入口、权限门禁和后续 UI coding 边界。未启动本地服务，未做真实 E2E；后续涉及真实用户路径时必须从 `http://localhost:8081` 或登记的 worktree 端口登录测试租户执行。

## 可复用对象

### 1. eDHR API 适配层

- `src/api/mes/pro/edhr/batchExecution.ts`
  - `/mes/pro/edhr-batch-execution/page`
  - `/mes/pro/edhr-batch-execution/get`
  - `/mes/pro/edhr-batch-execution/open-or-create`
  - `/mes/pro/edhr-batch-execution/task/open`
  - `/mes/pro/edhr-batch-execution/task/special-node/skip`
  - `/mes/pro/edhr-batch-execution/task/special-node/complete`
  - `/mes/pro/edhr-batch-execution/task/special-node/attachment/prepare-upload`
  - `/mes/pro/edhr-batch-execution/sync-status`
  - `/mes/pro/edhr-batch-execution/close`
  - `/mes/pro/edhr-batch-execution/quality-reject`
  - `/mes/pro/edhr-batch-execution-archive/generate`
  - `/mes/pro/edhr-batch-execution-archive/latest`
  - `/mes/pro/edhr-batch-execution-archive/download`
- `src/api/mes/pro/edhr/change.ts`
  - `/mes/pro/edhr-change/void-execution/request`
  - `/mes/pro/edhr-change/void-execution/approve`
  - `/mes/pro/edhr-change/reopen-batch/request`
  - `/mes/pro/edhr-change/reopen-batch/approve`
  - `/mes/pro/edhr-change/reopen-execution/request`
  - `/mes/pro/edhr-change/reopen-execution/approve`
  - `/mes/pro/edhr-change/supplement/request`
  - `/mes/pro/edhr-change/supplement/save-draft`
  - `/mes/pro/edhr-change/supplement/submit`
  - `/mes/pro/edhr-change/supplement/approve`
  - `/mes/pro/edhr-change/page`
  - `/mes/pro/edhr-change/get`
- `src/api/mes/pro/edhr/workTask.ts`
  - `/mes/pro/edhr-work-task/my-page`
  - `/mes/pro/edhr-work-task/done-page`
  - `/mes/pro/edhr-work-task/candidate-todo-page`
  - `/mes/pro/edhr-work-task/stats`
  - `/mes/pro/edhr-work-task/route-archive-rule`
  - `/mes/pro/edhr-work-task/candidate-signature/complete`
- `src/api/mes/pro/edhr/permission.ts`
  - `/mes/pro/edhr-permission-scopes/get`
  - `/mes/pro/edhr-permission-scopes/save`
  - `/mes/pro/edhr-permission-scopes/evaluate`
- `src/api/mes/pro/edhr/operationAudit.ts`
  - `/mes/pro/edhr-operation-audit/page`
  - `/mes/pro/edhr-operation-audit/{id}`
- `src/api/mes/pro/edhr/fieldAudit.ts`
  - `/mes/pro/batch-record-execution/field-audit/save-changes`
  - `/mes/pro/batch-record-execution/field-audit/page`
  - `/mes/pro/batch-record-execution/field-audit/detail`
  - `/mes/pro/batch-record-execution/field-audit/verify-chain`
  - `/mes/pro/batch-record-execution/field-audit/export`
- `src/api/mes/pro/edhr/domainTrace.ts`
  - `/mes/pro/batch-record-execution/domain-trace/detail`
  - `/mes/pro/batch-record-execution/domain-trace/page`
  - `/mes/pro/batch-record-execution/domain-trace/verify`
- `src/api/mes/pro/edhr/archive.ts`
  - `/mes/pro/batch-record-execution-archive/generate`
  - `/mes/pro/batch-record-execution-archive/page`
  - `/mes/pro/batch-record-execution-archive/latest`
  - `/mes/pro/batch-record-execution-archive/download`
- `src/api/mes/pro/edhr/approval.ts`
  - `/mes/pro/batch-record-execution/approval-pending-page`
  - `/mes/pro/batch-record-execution/approval-done-page`
  - `/mes/pro/batch-record-execution/approval-detail`
  - `/mes/pro/batch-record-execution/approve`
  - `/mes/pro/batch-record-execution/reject`
- `src/api/mes/pro/edhr/tracking.ts`
  - `/mes/pro/batch-record-execution/tracking-page`
  - `/mes/pro/batch-record-execution/tracking-timeline`
- `src/api/mes/pro/edhr/signatures.ts`
  - `/mes/pro/batch-record-execution/signature-page`
- `src/api/mes/pro/edhr/attachment.ts`
  - `/mes/pro/batch-record-execution/attachment/prepare-upload`

### 2. eDHR 页面入口

- `src/views/mes/pro/edhr/ExecutionListPage.vue`
- `src/views/mes/pro/edhr/ExecutionPage.vue`（仅承载执行表单与追踪只读视图，不再注册执行详情路由）
- `src/views/mes/pro/edhr/ApprovalPage.vue`
- `src/views/mes/pro/edhr/ApprovalDetailPage.vue`
- `src/views/mes/pro/edhr/TrackingPage.vue`
- `src/views/mes/pro/edhr/SignaturePage.vue`
- `src/views/mes/pro/edhr/RecordChangePage.vue`
- `src/views/mes/pro/edhr/FieldAuditPage.vue`
- `src/views/mes/pro/edhr/FieldAuditDetailPage.vue`
- `src/views/mes/pro/edhr/DomainTracePage.vue`
- `src/views/mes/pro/edhr/DomainTraceDetailPage.vue`
- `src/views/mes/pro/edhr/OperationAuditPage.vue`
- `src/views/mes/pro/edhr/PermissionMatrixPage.vue`
- `src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue`
- `src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`
- `src/views/mes/pro/edhr-batch/BatchExecutionReviewPage.vue`
- `src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue`
- `src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue`

### 3. 已有前端权限门禁

- 旧版执行与归档：
  - `mes:pro-batch-record-execution:update`
  - `mes:pro-batch-record-execution:approve`
  - `mes:pro-batch-record-execution:track`
  - `mes:pro-batch-record-execution:signature-query`
  - `mes:pro-batch-record-execution-archive:create`
  - `mes:pro-batch-record-execution-archive:query`
  - `mes:pro-batch-record-execution-archive:download`
- 字段审计、域追溯：
  - `mes:pro-batch-record-execution:field-audit-update`
  - `mes:pro-batch-record-execution:field-audit-query`
  - `mes:pro-batch-record-execution:field-audit-verify`
  - `mes:pro-batch-record-execution:field-audit-export`
  - `mes:pro-batch-record-execution:domain-trace-query`
  - `mes:pro-batch-record-execution:domain-trace-verify`
- 批次级 eDHR：
  - `mes:pro-edhr-batch-execution:create`
  - `mes:pro-edhr-batch-execution:update`
  - `mes:pro-edhr-batch-execution:close`
  - `mes:pro-edhr-batch-execution:quality-reject`
  - `mes:pro-edhr-batch-execution-archive:create`
  - `mes:pro-edhr-batch-execution-archive:download`
- 变更、审计、权限：
  - `mes:pro-edhr-change:void`
  - `mes:pro-edhr-change:reopen`
  - `mes:pro-edhr-change:supplement`
  - `mes:pro-edhr-change:query`
  - `mes:pro-edhr-operation-audit:query`
  - `mes:pro-edhr-permission-scope:evaluate`
- 工作任务：
  - `mes:pro-edhr-work-task:update`
  - `mes:pro-edhr-work-task-rule:update`

## 需新增对象

### T1 初始化与 DHR 模板

- 初始化中心页面、初始化批次详情、预检问题列表、manifest 证据抽屉。
- DHR 模板列表、模板版本详情、模板字段映射、模板生效/作废/签核动作。

### T2 独立表单与记录本

- 独立表单模板、表单实例草稿/提交、记录本模板、记录本条目页面。
- 表单/记录本与工单、批次、SN、工序、eDHR 执行记录的绑定入口与筛选器。

### T3 流转单、标签、打印管理

- 流转单模板、流转单实例、流转单与工单/批次/SN/工序绑定页面。
- 标签模板、标签实例、标签作废、补打申请页面。
- 打印管理页面：打印任务、打印队列、打印机绑定、打印回执、补打与作废证据。

### T4 放行前检查、统一变更、流程干预

- 放行列表、放行前检查结果、失败项钻取、放行事务详情。
- 流程日志、受控干预申请、干预审批与干预证据页面。

### T5 报表与看板

- 报表目录、口径字典、标准报表只读查询、看板指标和异常解释入口。

### T6 CSV/OQ/PQ 与部署交付

- 交付驾驶舱、CSV/OQ/PQ 证据、客户签核、培训记录、部署清单、恢复演练证据页面。

## 禁止改写对象

- 不得删除现有 eDHR API 适配层；后续只允许在当前文件中追加类型、字段或函数，不能让既有页面调用断链。
- 不得绕过菜单权限和 v-hasPermi；新增按钮、批量操作、详情动作必须同时有后端权限码和前端可见性门禁。
- 不得把后端错误吞掉，也不得用空 `catch {}`、空 toast、空表格或默认成功遮蔽真实失败。
- 不得用空白页面、默认 0 或静默失败当成验收通过；报表、看板、放行和交付页面必须展示真实查询结果或明确失败原因。
- 不得为了 E2E 临时加测试控件或绕过真实菜单入口；无入口时先补正式入口。

## 后续 Coding 门禁

- 缺真实路由、菜单、权限或样本时必须 fail fast，并在任务日志记录影响。
- 新页面必须遵守 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md` 的紧凑操作台风格，不做营销式落地页。
- 涉及写入的前端联调必须使用测试租户真实登录路径，接口只用于最终校验。
- 新增 API 函数必须绑定后端真实路径，不允许 mock 成功、静默降级或返回默认空成功。
- P1 建议先实现 `CR-T1-01 init-batch-precheck` 或 `CR-T4-01 release-precheck-engine` 的只读/预检切片，再做 T3 打印执行类高风险动作。
