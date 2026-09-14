# 活跃订单放行资料数据模型 V2

## Purpose and Scope

本文定义 V2 需要的实体、关系、状态和完整性规则。第一版只新增必要申请和映射追溯数据，不新增平行批次执行主流程。

## Evidence Reviewed

- PRD V2。
- 开发方案 V2。
- 当前项目关于批记录、PQC 汇集、eDHR 批次执行和放行负责人来源的约束。

## Entities

### 复用实体

- `mes_pro_process_pool_active_order`：活跃订单。
- `mes_pro_process_pool_active_order_process_snapshot`：活跃订单工序快照。
- `mes_pro_process_pool_event`：生产/PQC 事件来源。
- 生产组长报工历史相关表：生产历史数据来源。
- 生产历史表单相关表：批记录字段来源。
- PQC 检验任务和逐件明细相关表：过程检验来源。
- `mes_pqc_process_inspection_aggregate_detail`：PQC 组长复核后汇集明细。
- 批记录表单绑定和报表元数据表：正式批记录目标。
- QA 文件/检验规程版本表：PQC 约束来源。
- `mes_pro_edhr_batch_execution`：正式批次执行。
- `mes_pro_edhr_batch_execution_task`：批次执行任务。
- `mes_pro_batch_record_execution` 及字段审计表：正式批记录执行和字段追溯。
- `mes_pro_edhr_release_transaction`：放行事务。
- `mes_pro_edhr_work_task`：生产负责人放行待办。

### 新增实体：活跃订单放行申请

建议表：`mes_process_pool_active_order_release_application`

字段：

- `id`
- `tenant_id`
- `active_order_id`
- `work_order_id`
- `product_id`
- `route_id`
- `route_version_id`
- `batch_code`
- `application_status`
- `source_snapshot_hash`
- `request_idempotency_key`
- `business_idempotency_key`
- `batch_execution_id`
- `release_transaction_id`
- `release_approval_work_task_id`
- `dossier_summary_json`
- `blocker_snapshot_json`
- `applied_by`
- `applied_at`
- `created_at`
- `updated_at`
- `deleted`

唯一键：

- `tenant_id + active_order_id + request_idempotency_key + deleted`
- `tenant_id + active_order_id + business_idempotency_key + deleted`

### 可选实体：字段映射快照

第一版不新增该表。仅当批记录字段审计、PQC 汇集明细、损耗来源记录和操作审计无法证明字段来源时，后续版本才新增：

`mes_process_pool_release_dossier_field_source`

字段：

- `application_id`
- `dossier_type`
- `target_form_id`
- `target_field_code`
- `source_type`
- `source_id`
- `source_field_code`
- `source_value_hash`
- `source_user_id`
- `source_signed_at`

第一版依赖现有字段审计、汇集明细、损耗来源记录、操作审计和 `dossier_summary_json`；该表不是首版开发前置。

## Relationships

- 一个活跃订单可有多次申请历史。
- 同一活跃订单同一来源快照只允许一个有效申请。
- 一个申请关联一个正式批次执行。
- 一个申请关联一个放行事务。
- 一个放行事务关联一个生产负责人待办。
- 一个批次执行关联多张批记录、过程检验单和损耗单。
- 批记录字段来源于生产历史和生产组长确认。
- 过程检验字段来源于 PQC 历史、QA 文件和 PQC 组长复核。
- 损耗字段来源于生产损耗历史。

## State Models

### Application Status

- `NOT_READY`：来源未满足。
- `PRECHECKING`：正在预检。
- `BLOCKED`：缺来源、映射、签名或负责人。
- `GENERATING`：正在生成正式资料。
- `PENDING_RELEASE_APPROVAL`：待生产负责人放行。
- `RELEASED`：已放行。
- `REJECTED`：已驳回。

### Source Snapshot

来源快照应至少包含：

- 活跃订单和生产工单身份。
- 产品、路线、路线版本、工序。
- 生产历史记录集合 hash。
- 生产历史表单集合 hash。
- PQC 历史记录集合 hash。
- PQC 历史表单/汇集明细集合 hash。
- 损耗明细集合 hash。
- QA 文件版本和批记录表单版本。
- 签名来源集合 hash。

## Migration Notes

- 新增申请表需要租户、逻辑删除、状态索引、申请时间索引和唯一键。
- 不修改历史业务主数据来适配本功能。
- 首版不新增字段映射快照表；如后续新增，必须先确认现有字段审计、PQC 汇集明细、损耗来源记录和操作审计无法满足追溯。
- 菜单权限只增加生产组长“申请放行”权限，负责人放行优先复用现有权限。

## Data Integrity Rules

- DIR-01 活跃订单、生产工单、批次执行、放行事务必须同租户。
- DIR-02 申请人必须是该活跃订单生产组长。
- DIR-03 进度 100% 必须能追溯到正式历史记录。
- DIR-04 批记录模板和 QA 文件版本必须与产品/路线/工序匹配。
- DIR-05 字段映射必须有源对象、源字段和目标字段。
- DIR-06 人员和时间必须来自正式签名/确认记录。
- DIR-07 负责人待办必须唯一。
- DIR-08 blocker 保存可以不创建正式资料；正式资料创建失败不得留下半生成数据。
- DIR-09 不保存签名密码、token 或密钥。

## Open Questions

- OQ-01 过程检验单和损耗单是否需要独立字段映射快照表。
- OQ-02 无损耗口径由模板字段表达还是固定业务状态表达。
- OQ-03 来源快照 hash 是否纳入字段值 hash，还是只纳入来源记录版本 hash。

## Design Blockers

- DB-01 不能定位 QA 文件和批记录表单版本时，映射不可验收。
- DB-02 不能从历史数据取得签名时间时，正式表单人员时间不可验收。
- DB-03 损耗来源缺正式明细时，损耗单不可验收成功路径。
