# eDHR 现有合约盘点

任务：`CR-P0-01 existing-edhr-contract`

基线：`codex/20260618-edhr-cr-p0-existing-contract`，从后端 `int_main@04769b26fb` 创建。

范围：本文件只锁定现有后端表、接口、权限、菜单 SQL 和测试契约。未访问真实数据库，后续任何 schema、菜单、权限或租户绑定变更必须先以当前真实库 `SHOW TABLES` / `DESCRIBE` / 菜单权限查询复核。

## 可复用对象

### 1. 批次级 eDHR 执行对象

- 控制器：`MesProEdhrBatchExecutionController`
- 基础路径：`/mes/pro/edhr-batch-execution`
- 已有能力：
  - `GET /page`、`GET /get`
  - `POST /open-or-create`
  - `POST /task/open`
  - `POST /task/special-node/skip`
  - `POST /task/special-node/complete`
  - `POST /task/special-node/attachment/prepare-upload`
  - `POST /sync-status`
  - `POST /close`
  - `POST /quality-reject`
- 权限码：
  - `mes:pro-edhr-batch-execution:query`
  - `mes:pro-edhr-batch-execution:create`
  - `mes:pro-edhr-batch-execution:update`
  - `mes:pro-edhr-batch-execution:close`
  - `mes:pro-edhr-batch-execution:quality-reject`
- 数据对象：
  - `mes_pro_edhr_batch_execution`
  - `mes_pro_edhr_batch_execution_task`
  - `mes_pro_edhr_batch_execution_signature`
  - `mes_pro_edhr_batch_execution_archive`
- 已有多批记录能力：`mes_pro_edhr_batch_execution_task.batch_record_sort` 与唯一键 `uk_mes_pro_edhr_batch_task_process_report` 支持同一工序下多张批记录按顺序执行。
- 迁移来源：`sql/mysql/20260608_edhr_batch_execution_schema.sql`
- 后续复用方式：T1/T2/T3/T4 需要关联批次级执行对象时，应通过该对象扩展元数据或建立外键/业务关联，不另造平行批次执行主表。

### 2. 旧版电子批记录执行对象

- 控制器：
  - `MesProBatchRecordExecutionController`
  - `MesProBatchRecordExecutionArchiveController`
  - `MesProBatchRecordExecutionFieldAuditController`
  - `MesProBatchRecordDomainTraceController`
  - `MesProBatchRecordExecutionAttachmentController`
- 基础路径：
  - `/mes/pro/batch-record-execution`
  - `/mes/pro/batch-record-execution-archive`
  - `/mes/pro/batch-record-execution/field-audit`
  - `/mes/pro/batch-record-execution/domain-trace`
  - `/mes/pro/batch-record-execution/attachment`
- 权限码：
  - `mes:pro-batch-record-execution:create`
  - `mes:pro-batch-record-execution:query`
  - `mes:pro-batch-record-execution:update`
  - `mes:pro-batch-record-execution:approve`
  - `mes:pro-batch-record-execution:track`
  - `mes:pro-batch-record-execution:signature-query`
  - `mes:pro-batch-record-execution-archive:create`
  - `mes:pro-batch-record-execution-archive:query`
  - `mes:pro-batch-record-execution-archive:download`
  - `mes:pro-batch-record-execution:field-audit-update`
  - `mes:pro-batch-record-execution:field-audit-query`
  - `mes:pro-batch-record-execution:field-audit-verify`
  - `mes:pro-batch-record-execution:field-audit-export`
  - `mes:pro-batch-record-execution:domain-trace-query`
  - `mes:pro-batch-record-execution:domain-trace-verify`
- 数据对象：
  - `mes_pro_batch_record_execution`
  - `mes_pro_batch_record_execution_signature`
  - `mes_pro_batch_record_execution_archive`
  - `mes_pro_batch_record_execution_archive_event`
  - `mes_pro_batch_record_execution_field_audit_batch`
  - `mes_pro_batch_record_execution_field_audit_item`
  - `mes_pro_batch_record_domain_trace_snapshot`
  - `mes_pro_batch_record_domain_trace_item`
  - `mes_pro_batch_record_execution_attachment`
- 迁移来源：
  - `sql/mysql/20260525_edhr_archive_schema.sql`
  - `sql/mysql/20260526_edhr_field_audit_schema.sql`
  - `sql/mysql/20260528_edhr_domain_trace_schema.sql`
  - `sql/mysql/20260612_mes_edhr_attachment_ledger.sql`
- 后续复用方式：T2 独立表单、T4 放行前检查、T5 报表应复用字段审计、归档、追溯和签名链路，避免重复实现审计链。

### 3. 工作任务与审批签字格

- 控制器：`MesProEdhrWorkTaskController`
- 基础路径：`/mes/pro/edhr-work-task`
- 已有能力：
  - `GET /my-page`
  - `GET /done-page`
  - `GET /candidate-todo-page`
  - `GET /stats`
  - `GET /route-archive-rule`
  - `POST /route-archive-rule`
  - `POST /candidate-signature/complete`
- 权限码：
  - `mes:pro-edhr-work-task:query`
  - `mes:pro-edhr-work-task:update`
  - `mes:pro-edhr-work-task-rule:query`
  - `mes:pro-edhr-work-task-rule:update`
- 数据对象：
  - `mes_pro_edhr_work_task`
  - `mes_pro_edhr_work_task_assignment_rule`
- 迁移来源：
  - `sql/mysql/20260611_mes_edhr_work_task_flow.sql`
  - `sql/mysql/20260611_mes_edhr_multi_signature_approval.sql`
- 后续复用方式：T3 流转单审批、T4 放行干预和 T6 交付证据应复用工作任务对象承载待办、候选签核和签字格证据。

### 4. 变更记录、作废、重开、补录

- 控制器：`MesProEdhrRecordChangeController`
- 基础路径：`/mes/pro/edhr-change`
- 已有能力：
  - `POST /void-execution/request`
  - `POST /void-execution/approve`
  - `POST /reopen-batch/request`
  - `POST /reopen-batch/approve`
  - `POST /reopen-execution/request`
  - `POST /reopen-execution/approve`
  - `POST /supplement/request`
  - `PUT /supplement/save-draft`
  - `POST /supplement/submit`
  - `POST /supplement/approve`
  - `GET /page`
  - `GET /get`
- 权限码：
  - `mes:pro-edhr-change:void`
  - `mes:pro-edhr-change:approve`
  - `mes:pro-edhr-change:reopen`
  - `mes:pro-edhr-change:supplement`
  - `mes:pro-edhr-change:query`
- 数据对象：`mes_pro_edhr_record_change_event`
- 菜单迁移：`sql/mysql/20260612_mes_edhr_record_change_menu.sql`
- 后续复用方式：T4 统一变更、受控干预和放行异常处理必须复用该变更记录入口，不允许后台直接改终态。

### 5. 对象权限与操作审计

- 控制器：
  - `MesProEdhrPermissionScopeController`
  - `MesProEdhrOperationAuditController`
- 基础路径：
  - `/mes/pro/edhr-permission-scopes`
  - `/mes/pro/edhr-operation-audit`
- 已有能力：
  - `POST /mes/pro/edhr-permission-scopes/save`
  - `GET /mes/pro/edhr-permission-scopes/get`
  - `POST /mes/pro/edhr-permission-scopes/evaluate`
  - `GET /mes/pro/edhr-operation-audit/page`
  - `GET /mes/pro/edhr-operation-audit/{id}`
- 权限码：
  - `mes:pro-edhr-permission-scope:save`
  - `mes:pro-edhr-permission-scope:query`
  - `mes:pro-edhr-permission-scope:evaluate`
  - `mes:pro-edhr-operation-audit:query`
- 数据对象：
  - `mes_pro_edhr_permission_scope`
  - `mes_pro_edhr_permission_rule`
  - `mes_pro_edhr_operation_audit_event`
- 迁移来源：`sql/mysql/20260615_mes_edhr_tail_four_goals.sql`
- 后续复用方式：T1-T6 新对象都要登记对象权限范围、能力矩阵和操作审计，不得绕开该能力另建不可审计权限。

## 需新增对象

### T1 初始化与 DHR 模板

- 初始化批次、初始化 manifest、预检问题、导入任务状态与证据文件对象。
- DHR 模板生命周期对象：模板版本、客户字段映射、签核/生效/作废状态。
- 与现有复用点：批次级 eDHR 执行对象、对象权限、操作审计、归档证据。

### T2 独立表单与记录本

- 独立表单模板、表单实例、记录本模板、记录本条目。
- 表单/记录本与工单、批次、SN、工序、eDHR 执行记录的受控绑定关系。
- 与现有复用点：`mes_pro_batch_record_execution`、字段审计、变更记录、对象权限。

### T3 流转单、标签、打印管理

- 流转单模板、流转单实例、流转单状态流、与工单/批次/SN/工序绑定的业务唯一键。
- 标签模板、标签实例、标签版本、标签作废/补打证据。
- 打印管理对象：打印任务、打印队列、打印机绑定、打印回执、补打申请、作废记录。
- 与现有复用点：工作任务、签名时间、对象权限、操作审计。

### T4 放行前检查、统一变更、流程干预

- 放行事务、放行前检查项、失败项源对象钻取、检查快照、检查结论。
- 流程日志、受控干预、人工干预审批、干预证据。
- 与现有复用点：变更记录、工作任务、字段审计、域追溯、归档证据。

### T5 报表与看板

- 报表目录、报表口径字典、报表查询配置、标准报表版本。
- 看板指标、指标口径、数据刷新记录、异常解释入口。
- 与现有复用点：批次执行、流转单、放行、打印、变更、审计表。

### T6 CSV/OQ/PQ 与部署交付

- CSV/OQ/PQ 证据对象、测试脚本结果、客户签核、培训记录、部署清单、恢复演练证据。
- 交付驾驶舱与 gate explainer。
- 与现有复用点：操作审计、归档、对象权限、初始化 manifest、放行检查结论。

## 禁止改写对象

- 不得改写 `/mes/pro/batch-record-execution`、`/mes/pro/batch-record-execution-archive`、`/mes/pro/batch-record-execution/field-audit`、`/mes/pro/batch-record-execution/domain-trace` 的已有语义；后续只能兼容性扩展，不得替换为新路径后让旧页面失效。
- 不得重命名 `mes_pro_batch_record_execution`、`mes_pro_batch_record_execution_signature`、`mes_pro_batch_record_execution_archive`、`mes_pro_edhr_batch_execution`、`mes_pro_edhr_work_task`、`mes_pro_edhr_operation_audit_event`、`mes_pro_edhr_permission_scope`、`mes_pro_edhr_permission_rule`。
- 不得绕过 eDHR 对象权限、操作审计、签名时间和归档证据；新增写操作必须明确权限码、对象范围、审计事件和失败原因。
- 不得用后台改状态、手工 SQL、默认成功、默认空数据、空白看板或静默豁免代替真实流程。
- 不得在缺少真实表、菜单、权限、租户绑定、样本数据、打印回执、客户签核或恢复演练证据时声明商业化交付完成。

## 后续 Coding 门禁

- 缺真实表、菜单、权限、租户绑定或样本时必须 fail fast，并在任务执行日志写明影响范围。
- 新增 schema 前必须用真实库 `SHOW TABLES` / `DESCRIBE` 核对表名字段，不能只按 DO 类名或历史记忆写 SQL。
- 新增接口必须遵循 `/mes/pro/edhr-*` 或现有 `/mes/pro/batch-record-execution*` 扩展边界，并配套权限码 `mes:pro-edhr-<domain>:<action>` 或已有权限码。
- 写操作必须有幂等键或业务唯一键；重复提交不得产生重复记录。
- 高风险动作，包括真实 E2E 写入、服务器操作、备份恢复、发布、数据库 schema、菜单/租户权限修改，必须先在任务日志记录经验预检通过。
- P1 建议顺序：先做 `CR-T1-01 init-batch-precheck` 或 `CR-T4-01 release-precheck-engine`，并在实现前再次复核本文件。
