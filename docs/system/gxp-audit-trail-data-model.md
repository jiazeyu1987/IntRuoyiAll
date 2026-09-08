# 统一 GxP 审计追踪数据模型

## Purpose and Scope

定义统一只追加事件、专业证据引用、完整性清单、归档回执和周期审查的数据关系。该模型是审计事实源，不替代领域业务表或电子签名事实源。

## Evidence Reviewed

- `system_operate_log` 继承通用 `BaseDO`，包含可更新和逻辑删除字段，不适合作为法规只追加账本。
- `mes_pro_batch_record_execution_field_audit_batch/item` 已包含原因、前后值、签名和 hash 链。
- `dcc_controlled_file_signature`、`mes_pro_batch_record_execution_signature`、`showroom_change_request_signature`、`bpm_approval_signature_record` 是现有签名事实来源。
- `infra_release_migration` 已包含发布标签、文件 hash、环境和执行状态。

## Entities

### gxp_audit_event

- 身份：`id`、`event_uuid`、`tenant_id`、`ledger_sequence`、`operation_id`、`transaction_ref_hash`。
- 对象：`domain_code`、`subject_type`、`subject_id`、`subject_code_snapshot`、`subject_version`。
- 行为：`action_type`、`result_type`、`reason_code`、`reason_text`。
- 操作人：`actor_id`、`actor_name_snapshot`、`actor_type`、`actor_auth_session_id_hash`。
- 变化：`before_state_json`、`after_state_json`、`diff_json`、`payload_schema_version`。状态信封必须包含 `state/schemaVersion/data`，并按动作约束 ABSENT、PRESENT、VOIDED、REDACTED 的合法组合。
- 来源：`source_module`、`source_api`、`request_id`、`trace_id`、`idempotency_key`。
- 策略：`policy_version`、`retention_class`、`data_classification`。
- 时间：`occurred_at_utc`、`recorded_at_utc`、`display_timezone`、`trusted_time_evidence_id`。
- 完整性：`previous_subject_hash`、`event_hash`、`hash_algorithm`。

该表不继承 `BaseDO`，不包含 `updater`、`update_time`、`deleted`，不提供 update/delete Mapper。

### gxp_audit_evidence_link

- `audit_event_id`、`evidence_type`、`source_table_code`、`source_id`、`source_version`、`evidence_hash`、`relation_type`。
- 用于关联 FIELD_AUDIT_BATCH、SIGNATURE_RECORD、FILE_HASH、APPROVAL_RECORD、MIGRATION_RECORD、TIME_EVIDENCE 等。
- 同一必需证据类型只能有一个 ACTIVE 身份；历史证据不能重绑。

### gxp_audit_daily_manifest

- `tenant_id`、`business_date`、`first_sequence`、`last_sequence`、`event_count`、`merkle_root`、`previous_manifest_hash`、`manifest_hash`、`generated_at_utc`。
- 覆盖当日全部事件，归档后不更新；迟到事件进入明确的补充清单并关联原日期，不能覆盖原清单。

### gxp_audit_archive_receipt

- `archive_package_id`、`replica_role`、`object_key`、`version_id`、`sha256`、`retention_mode`、`retain_until`、`legal_hold_status`、`verified_at_utc`、`verification_result`。
- 回执只保存正式对象存储读取结果，不能由客户端自报。

### gxp_audit_archive_package / item

- Package：`id`、`tenant_id`、`package_schema_version`、`first_sequence`、`last_sequence`、`event_count`、`evidence_count`、`signature_count`、`manifest_id`、`package_sha256`、`status`、`sealed_at_utc`。
- Item：归档包内每个规范文件的 `relative_path`、`content_type`、`byte_size`、`sha256`、`record_count`、`required`。
- 归档对象固定包含完整 `events.ndjson`、`evidence-links.ndjson`、签名证据、策略、可信时间、必需领域证据、`manifest.json` 和 `SHA256SUMS`。所有 required item 存在且 hash/计数匹配后才能从 `BUILDING` 进入 `SEALED`。
- 数据库备份与法规归档包是两种独立对象；数据库备份不能代替可长期查阅的法规归档包，归档包也不能代替灾难恢复备份。

### gxp_audit_policy_version

- `version_code`、`effective_from`、`approval_ref`、`approved_by_signature_id`、`policy_hash`、`status`。
- 策略版本一旦生效不可编辑；新规则创建新版本。

### gxp_audit_review_schedule / run / batch / item / finding / remediation

- Schedule：`tenant_id`、`rule_version`、`period_rule`、`timezone`、`effective_from`、`owner_id`、`approval_signature_id`、`status`。
- Run：`schedule_id`、`period_code`、`planned_at_utc`、`triggered_at_utc`、`completed_at_utc`、`run_type`、`covered_through_sequence`、`result`、`error_code`、`alert_event_id`；`schedule_id + period_code` 唯一。
- Batch：周期、范围、到期日、冻结水位、快照 hash、状态、质量 owner。
- Item：审查批次与事件/清单的固定关系及审查结果，并保存总体、全量/抽样方式、确定性抽样参数和排除依据。
- Finding：发现代码、严重度、来源事件和状态。
- Remediation：整改证据、负责人、期限、复核签名和关闭状态。
- 审查签名绑定批次快照 hash，不能只绑定批次编号。

### gxp_audit_coverage_report

- `policy_version`、`source_commit`、`registry_sha256`、`discovered_inventory_sha256`、`registered_count`、`not_applicable_count`、`gap_count`、`test_mapping_count`、`generated_at_utc`、`report_sha256`。
- 报告明细保存 `operation_id`、入口类型和稳定源码定位、登记结果、策略、测试 ID、owner，以及不适用批准人/依据/有效期。
- `gap_count` 非零、例外过期或双向清单不一致时不能标记 PASS。

### gxp_system_change_manifest

- `release_tag`、`from_commit`、`to_commit`、`artifact_digest_before/after`、`image_digest_before/after`、`openapi_hash_before/after`、`openapi_diff_json`、`migration_inventory_json/hash`、`config_snapshot_before/after_hash`、`config_diff_json`、`approval_ref`、`reason`、`actor_id`、`started_at_utc`、`finished_at_utc`、`result`、`rollback_of`、`manifest_hash`。
- Migration 清单必须保留确定顺序和逐文件 hash；配置快照必须脱敏但保持可重算稳定结构。

### gxp_privileged_audit_receipt

- `source_system`、`source_event_id`、`actor_ref`、`action_type`、`object_ref`、`occurred_at_utc`、`forwarded_at_utc`、`protected_object_key`、`version_id`、`event_hash`、`verification_result`。
- 承载数据库审计表 DML/DDL、授权、特权账号使用、审计配置变化和关闭审计尝试的独立外送回执。

## Relationships

- 一个业务对象有多条 `gxp_audit_event`，按对象 hash 链排序。
- 一个事件可关联多项专业证据，但每项证据必须有稳定来源身份和 hash。
- 每条事件恰好进入一个正式日清单或一个显式补充清单。
- 每条事件及其必需证据恰好进入一个 SEALED 法规归档包；一个包必须绑定正式清单并具有主存储和独立副本回执。
- 一个审查批次冻结多个事件/清单；发现项和整改只追加状态事件，不改写被审查事件。
- 每个应审周期恰好有一个运行记录和一个冻结批次；补扫复用相同 `period_code`。
- 每个发布或回滚操作恰好有一个系统变更清单，并由统一审计事件绑定其 manifest hash。

## State Models

- 审计事件：只有 `APPENDED`；完整性结果是派生状态，不回写事件。
- 策略版本：`DRAFT -> APPROVED -> EFFECTIVE -> SUPERSEDED`，不得回到旧状态。
- 清单：`BUILDING -> SEALED -> ARCHIVED -> VERIFIED`；任一步失败进入 `FAILED` 且需新尝试，不能覆盖旧证据。
- 归档包：`BUILDING -> SEALED -> REPLICATED -> VERIFIED`；缺任一 required item、主副本回执或恢复证据时不得进入 VERIFIED。
- 审查运行：`PLANNED -> RUNNING -> COMPLETED|FAILED|OVERDUE`；FAILED/OVERDUE 保留原记录并新增重试/补扫尝试。
- 审查批次：`DRAFT -> FROZEN -> IN_REVIEW -> SIGNED -> CLOSED`；存在开放整改时不能进入 `SIGNED/CLOSED`。
- 整改：`OPEN -> SUBMITTED -> VERIFIED -> CLOSED`；拒绝后回到新的 `OPEN` 版本而非覆盖旧提交。

## Migration Notes

- 新表通过 release migration 创建，先部署 schema 和只读检查，再启用任何写路径。
- 对现有专业表不增加逻辑删除、不修改历史 hash、不重算既有签名时间。
- 历史注册按来源表和主键生成一次性事件；无法确定租户、对象、签名或 hash 的记录进入阻塞清单。
- 模块切换后禁止继续只写旧自由文本日志作为 GxP 成功证据。
- 回退仅允许在首条新事件产生前回退应用；产生正式事件后只能向前修复或从切换前完整备份恢复。

## Data Integrity Rules

- `event_uuid` 全局唯一，`tenant_id + ledger_sequence` 唯一，幂等业务键租户内唯一。
- `event_hash` 覆盖全部规范字段、冻结证据引用集合、`recorded_at_utc` 和 `previous_subject_hash`。
- 规范 JSON 固定 UTF-8、字段排序、空值表达、数值表达和 UTC 时间格式；算法由 `payload_schema_version` 锁定。
- `actor_id`、`occurred_at_utc`、hash 和序号由服务端产生，客户端值不参与正式证据。
- CREATE 必须是 ABSENT -> PRESENT，UPDATE/CORRECT 是 PRESENT -> PRESENT，DELETE 是 PRESENT -> ABSENT，VOID/OBSOLETE 是 PRESENT -> VOIDED；REDACTED 保存策略标识和原值 hash。必需快照不接受 null、UNAVAILABLE 或空对象占位。
- 需要原因的策略禁止空字符串、纯空白和通用占位原因。
- 需要签名的事件必须存在签名证据、签名人等于动作人或符合批准的代理规则，并验证内容 hash。
- 审计表数据库角色禁止 UPDATE/DELETE/TRUNCATE；schema 变更只能走受控 Migration 并生成系统审计。
- 特权审计记录必须在批准的最大外送时长内写入独立保护存储；未封存事件水位超过批准阈值、外送断链或特权审计源被关闭时合规状态必须失败。
- 法规归档恢复验收必须在隔离环境清空目标审计数据后，仅使用归档包恢复完整事件、证据、签名、时间和查询/导出能力。
- 密码、令牌、私钥和连接密钥不得进入 before/after；敏感业务字段按批准策略脱敏或保存不可逆 hash。

## Open Questions

- 日清单按自然日还是受控业务日封存。
- 超大 JSON 的数据库上限和外部证据对象阈值。
- 失败尝试是否进入同一法规账本或独立安全账本。
- 数据库特权审计使用何种正式产品或平台，由安全评审在实施前确认。

## Design Blockers

- 未批准记录保存类别、期限和起算事件，无法确定 `retention_class` 语义。
- 未确认正式数据库账号拆分和权限授予方式。
- 未确认正式 WORM 存储是否支持 versioning、Object Lock、retainUntil 和 legal hold 的真实读取验证。
- 未批准最大未封存时长、特权审计外送时限和独立保护存储目标。
