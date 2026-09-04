# DCC 第一阶段 Windchill 版本数据模型设计

## Purpose and Scope

本文定义 DCC 第一阶段的逻辑文件、大版本、小版本、检出、发布快照和审计数据模型。设计目标是把当前混合在 `dcc_controlled_file_master` 与 `dcc_controlled_file` 中的身份、工作版本、审批版本和正式版本职责拆清，同时复用现有审批签名、源文件独占、发布文件和受控内容生命周期能力。

第一阶段采用三层版本结构：

```text
逻辑文件 Master
  -> 大版本 Revision：A、B、C
      -> 小版本 Iteration：A/1、A/2、A/3
```

检出是独立的编辑锁和工作副本会话，不是小版本生命周期状态。只有检入成功才创建新的 Iteration。

## Evidence Reviewed

- `docs/product/dcc-windchill-version-phase1-prd.md` 及配套用户流程、验收标准。
- 当前 `DccControlledFileMasterDO`、`DccControlledFileDO`、Mapper、状态枚举和数字段版本解析器。
- `20260513_dcc_base_schema.sql` 中主档、受控文件、路线快照、签名、发布与审计表。
- `20260718_controlled_content_lifecycle.sql` 中单 ACTIVE、单开放候选和转换审计约束。
- `20260903_dcc_controlled_file_checkout.sql`、`20260811_dcc_upload_slot_idempotency.sql`、`20260811_dcc_source_ownership.sql`。
- 当前上传凭证、审批路线快照、签名证据、发布原子切换和版本历史查询实现。

## Entities

### 1. `dcc_controlled_file_master`：逻辑文件

保留现有表并将其明确为长期稳定的逻辑文件身份。目标字段如下：

| 字段 | 含义 |
|---|---|
| `id` | 逻辑文件 ID |
| `tenant_id` | 租户 ID |
| `dcc_project_code_id` | DCC 项目 ID，身份组成部分 |
| `file_type_taxonomy_id` | 分类叶子 ID，身份组成部分 |
| `file_number` | 文件编码展示值 |
| `normalized_file_number` | 文件编码规范化值，身份组成部分 |
| `file_name` | 当前展示名称，不参与身份唯一性 |
| `category_id` | 当前治理类别，不参与身份唯一性 |
| `directory_id` | 当前归档目录，不参与身份唯一性 |
| `current_released_iteration_id` | 当前正式小版本 ID |
| `open_revision_id` | 当前唯一开放大版本 ID |
| `status` | `ACTIVE_CHAIN` 或 `OBSOLETE_CHAIN` |
| `identity_unique_flag` | 未删除记录为 1，删除记录为 NULL 的生成列 |

现有 `current_active_controlled_file_id` 在切换迁移中改名为 `current_released_iteration_id`。不得长期保留两个意义相同的指针，也不得双写。

唯一索引：

```text
(tenant_id, dcc_project_code_id, file_type_taxonomy_id,
 normalized_file_number, identity_unique_flag)
```

旧的“类别 + 目录 + 文件名”唯一索引必须在新身份预检通过后移除，不能同时作为第二套身份规则继续生效。

### 1A. `dcc_project_access_rule`：项目访问权威规则

新增项目访问规则表，不使用项目负责人文本、项目修正任务或菜单权限推断项目访问。

| 字段 | 含义 |
|---|---|
| `id` | 规则 ID |
| `tenant_id` | 租户 ID |
| `dcc_project_code_id` | DCC 项目 ID |
| `subject_type` | `USER`、`DEPT`、`ROLE` 或 `POSITION` |
| `subject_id` | 对应主体 ID |
| `access_level` | `OWNER`、`EDIT` 或 `VIEW` |
| `active` | 是否生效 |
| `valid_from` | 可选生效时间 |
| `expire_time` | 可选到期时间 |
| `change_reason` | 授权或调整原因 |
| `active_rule_unique_flag` | 当前生效规则为 1，否则为 NULL |

同一项目、同一主体同时只能有一条生效规则。权限包含关系为 OWNER > EDIT > VIEW；OWNER 可以创建 Revision 和提交审批，EDIT 可以检出检入，VIEW 只能查看正式版本。规则只决定项目边界，仍需叠加全局接口权限、类别动作权限和目录/正文权限。

### 2. `dcc_controlled_file_revision`：大版本

新增大版本聚合表，一行代表 A、B、C 中的一个大版本。

| 字段 | 含义 |
|---|---|
| `id` | 大版本 ID |
| `tenant_id` | 租户 ID |
| `master_id` | 逻辑文件 ID |
| `revision_sequence` | 大版本内部顺序，从 1 开始 |
| `revision_code` | 大版本展示值，例如 A、B |
| `source_iteration_id` | 用户明确选定的修订来源小版本 |
| `released_baseline_iteration_id` | 创建本大版本时的当前正式小版本快照 |
| `revision_reason` | 创建大版本原因 |
| `source_selection_reason` | 选用非当前正式小版本时的理由 |
| `latest_iteration_id` | 本大版本最新工作小版本 |
| `submitted_iteration_id` | 当前正在审批或待发布的小版本；无时为 NULL |
| `released_iteration_id` | 本大版本最终正式发布的小版本；未发布时为 NULL |
| `status` | 大版本生命周期状态 |
| `open_revision_unique_flag` | 开放状态为 1，终态为 NULL 的生成列 |

唯一索引：

```text
(tenant_id, master_id, revision_sequence)
(tenant_id, master_id, revision_code)
(tenant_id, master_id, open_revision_unique_flag)
```

第三个索引确保一个逻辑文件最多只有一个开放大版本。

### 3. `dcc_controlled_file`：不可变小版本

保留现有表和主键，让现有审批、签名、分发、培训、关联文件及发布文件仍能通过 `controlled_file_id` 绑定同一个具体对象。该表从“模糊版本记录”收敛为不可变小版本记录。

新增或重新定义字段：

| 字段 | 含义 |
|---|---|
| `revision_id` | 所属大版本 ID |
| `iteration_no` | 小版本数字，从 1 开始 |
| `version_no` | 服务端生成的展示快照，例如 A/2；不接受客户端写入 |
| `legacy_version_no` | 迁移前版本标签，例如 V1.0；仅用于历史说明，不参与新版本逻辑 |
| `predecessor_iteration_id` | 同一大版本内直接前一小版本 |
| `source_file_sha256` | 本小版本源文件哈希快照 |
| `change_scope` | `CONTENT`、`METADATA` 或 `BOTH` |
| `metadata_snapshot_json` | 本小版本允许业务元数据的结构化快照及 schema 版本 |
| `metadata_diff_json` | 相对直接前驱的结构化字段差异 |
| `status` | 小版本生命周期状态 |
| `process_instance_id` | 本小版本送审的 BPM 流程 ID |
| 现有文件与业务字段 | 源文件、发布文件、盖章文件、标题、类别、目录、项目和分类快照等 |

唯一索引：

```text
(tenant_id, revision_id, iteration_no)
```

`revision_code + iteration_no` 是展示版本；排序和并发分配使用 `revision_sequence + iteration_no`，不得按字符串排序。

现有 `checked_out_by`、`checked_out_time` 在切换完成后从小版本表移除。历史小版本不能承担逻辑文件当前锁的职责。

### 4. `dcc_controlled_file_checkout`：检出和工作副本会话

新增独立检出记录，所有记录永久保留，只有一条可以处于 ACTIVE。

| 字段 | 含义 |
|---|---|
| `id` | 检出记录 ID |
| `tenant_id` | 租户 ID |
| `master_id` | 逻辑文件 ID |
| `revision_id` | 当前开放大版本 ID |
| `base_iteration_id` | 被检出的小版本 ID |
| `checked_out_by` | 检出人 |
| `checkout_reason` | 修改原因 |
| `status` | `ACTIVE`、`CHECKED_IN` 或 `CANCELLED` |
| `checked_out_time` | 检出时间 |
| `finished_time` | 检入或撤销时间 |
| `result_iteration_id` | 检入成功产生的新小版本；撤销时为空 |
| `finish_reason` | 检入说明或撤销原因 |
| `active_checkout_unique_flag` | ACTIVE 为 1，终态为 NULL 的生成列 |
| `checkout_idempotency_key` | 检出幂等键 |
| `finish_idempotency_key` | 检入或撤销幂等键 |
| `finish_request_hash` | 检入或撤销请求摘要 |

唯一索引至少包括：

```text
(tenant_id, master_id, active_checkout_unique_flag)
(tenant_id, checked_out_by, checkout_idempotency_key)
(tenant_id, checked_out_by, finish_idempotency_key)
```

### 5. `dcc_controlled_file_version_event`：版本动作审计与幂等回执

新增 DCC 版本专用不可变事件表，补足平台生命周期只记录送审和发布、不记录全部工作迭代的问题。

主要字段：`master_id`、`revision_id`、`iteration_id`、`checkout_id`、`event_type`、`event_key`、`request_hash`、`actor_id`、`from_status`、`to_status`、`source_iteration_id`、`result_iteration_id`、`reason`、`result`、`failure_code`、`source_sha256`、`result_sha256`、`occurred_at`。

`event_key` 在租户和事件类型范围内唯一。重复请求的请求摘要一致时返回原结果；摘要不一致时返回幂等冲突，不能执行第二次业务动作。

### 6. 大版本发布快照

新增以下表，第一阶段只负责冻结发布时事实：

- `dcc_controlled_file_release_snapshot`：发布小版本、发布人、发布时间、查看范围哈希、关联文件范围哈希。
- `dcc_controlled_file_release_party_snapshot`：发布时有查看资格的用户、部门、角色或岗位范围及来源规则。
- `dcc_controlled_file_release_relation_snapshot`：发布时关联的逻辑文件、具体正式小版本、关系来源。

这些记录不包含“已通知”或“已完成影响评估”字段。后续阶段必须建立独立通知和影响任务，不能把快照存在解释为后续动作完成。

### 7. 继续复用的现有实体

- `dcc_controlled_file_temporary_file`：检入前的临时源文件、哈希和上传凭证。
- `dcc_controlled_file_source_ownership`：每个小版本正式源文件的独占所有权和哈希。
- `dcc_controlled_file_route_snapshot`：具体送审小版本的审批路线快照。
- `dcc_controlled_file_signature` 与签名绑定表：具体小版本的签名和文件哈希证据。
- `dcc_controlled_file_related_file`：工作小版本提交时选择的关联文件快照。
- `controlled_content_version_ref` 与 `controlled_content_transition_audit`：送审候选和当前正式版本的组织级生命周期约束。

## Relationships

```text
dcc_project_code 1 -> N dcc_controlled_file_master
dcc_project_code 1 -> N dcc_project_access_rule
dcc_file_type_taxonomy leaf 1 -> N dcc_controlled_file_master
dcc_controlled_file_master 1 -> N dcc_controlled_file_revision
dcc_controlled_file_revision 1 -> N dcc_controlled_file
dcc_controlled_file_master 1 -> N dcc_controlled_file_checkout
dcc_controlled_file 1 -> N route_snapshot / signature / related_file
dcc_controlled_file 1 -> 0..1 controlled_content_version_ref
dcc_controlled_file 1 -> 0..1 release_snapshot
release_snapshot 1 -> N party_snapshot / relation_snapshot
```

平台生命周期引用只为“已送审或已正式登记”的小版本创建：

- A/1、A/2 等纯工作迭代不创建 `controlled_content_version_ref`。
- 最新工作小版本提交审批时创建一个开放候选引用。
- 驳回后该引用进入 REJECTED 并释放开放候选唯一位。
- 新小版本重新送审时创建新的候选引用。
- 发布成功时旧 ACTIVE 与新候选在同一事务中切换为 SUPERSEDED 和 ACTIVE。

Revision 的 `source_iteration_id` 与平台候选的 source ref 表达不同事实：

- `source_iteration_id` 是用户选定并实际复制内容的小版本，例如 A/2。
- `released_baseline_iteration_id` 是创建新大版本时普通用户正在使用的正式版本，例如 A/3。
- 平台候选的 source ref 继续指向提交时的当前 ACTIVE 正式版本 A/3，以满足平台正式版本链约束。

系统不得因为平台 source ref 是 A/3，就把 B/1 的实际内容来源改写为 A/3。

## State Models

### 大版本状态

| 状态 | 含义 |
|---|---|
| `IN_WORK` | 正在形成小版本，尚未送审 |
| `IN_REVIEW` | 最新小版本正在审批 |
| `READY_TO_PUBLISH` | 最新送审小版本已批准，等待文控发布 |
| `FINALIZING` | 发布事务处理中 |
| `TRAINING_IN_PROGRESS` | 已形成受控发布件，但必需培训尚未完成，旧正式版本继续有效 |
| `PENDING_MANUAL_DISTRIBUTION` | 现有规则要求文控完成手工下发，旧正式版本继续有效 |
| `FINALIZATION_FAILED` | 发布失败，旧正式版本不变 |
| `ACTIVE` | 本大版本有一个当前正式小版本 |
| `SUPERSEDED` | 本大版本已被下一正式大版本替代 |
| `CANCELLED` | 未发布大版本被受控取消 |
| `OBSOLETE` | 正式大版本被主动作废且无替代版本 |

### 小版本状态

| 状态 | 含义 |
|---|---|
| `WORKING` | 当前可编辑的最新工作小版本 |
| `ITERATED` | 已被同大版本下一小版本替代的工作历史 |
| `IN_REVIEW` | 本小版本正在审批，内容锁定 |
| `REJECTED` | 本小版本审批被驳回，历史保留 |
| `READY_TO_PUBLISH` | 本小版本已批准待发布 |
| `FINALIZING` | 本小版本发布处理中 |
| `TRAINING_IN_PROGRESS` | 必需培训未完成，尚未成为当前正式版本 |
| `PENDING_MANUAL_DISTRIBUTION` | 等待现有文控手工下发动作，尚未成为当前正式版本 |
| `FINALIZATION_FAILED` | 本小版本发布失败 |
| `ACTIVE` | 当前正式小版本 |
| `SUPERSEDED` | 曾正式发布，现已被下一大版本替代 |
| `CANCELLED` | 未发布小版本所在大版本被取消 |

### 检出状态

```text
ACTIVE -> CHECKED_IN
ACTIVE -> CANCELLED
```

检出状态不改变小版本的 `WORKING` 状态。是否“修改中”由 ACTIVE 检出记录投影得到。

### 项目访问级别

```text
OWNER -> EDIT -> VIEW
```

更高级别包含低级别能力，但不会自动包含类别动作或全局接口权限。项目规则不存在时默认无项目访问权。

### 审批与平台生命周期映射

| DCC 小版本 | 平台标准状态 |
|---|---|
| `IN_REVIEW` | `IN_REVIEW` |
| `REJECTED` | `REJECTED` |
| `READY_TO_PUBLISH` | `READY_TO_PUBLISH` |
| `FINALIZING` | `FINALIZING` |
| `FINALIZATION_FAILED` | `FINALIZATION_FAILED` |
| `ACTIVE` | `ACTIVE` |
| `SUPERSEDED` | `SUPERSEDED` |

## Migration Notes

1. 先执行只读预检，冻结 `max(master.id)`、运行时间和迁移输入摘要；不写业务表。
2. 对完全确定的旧测试链生成候选映射：身份唯一、版本号唯一可按现有数字规则排序、无未完成审批且正式指针唯一；每个旧业务版本依次映射为 A/1、B/1、C/1。
3. 其余数据形成逐主档人工映射，明确目标项目、分类叶子、规范化文件编码及每条旧版本的目标大版本、小版本和来源。
4. 新增 Project Access、Revision、Checkout、Version Event 和发布快照表，并给现有表增加新字段；此时旧代码仍运行，但不得开始回填。
5. 进入测试环境维护窗口，停止 DCC 受控文件写入口；不依赖双写或后台追赶。
6. 按已批准映射回填 master 身份、Revision 和 Iteration 字段，并迁移当前检出记录。
7. 重建并核对 `controlled_content_version_ref`：只保留送审历史和当前正式小版本，禁止将全部工作小版本登记为开放候选。
8. 所有完整性查询为零阻塞后建立新唯一索引，移除旧身份唯一索引和旧检出列。
9. 部署只读写新模型的代码并执行 postflight；没有旧模型读取 fallback。

已有签名、审批打印和审计记录中的旧 `version_no` 是当时证据快照，不得改写或重新签名。迁移后的 Iteration 通过 `legacy_version_no` 保存旧标签，页面明确展示“迁移后版本 A/1，原签名版本 V1.0”。新模型的签名只使用新版本标签。

完整盘点和迁移门禁见 `docs/system/dcc-windchill-version-phase1-migration-inventory.md`。

## Data Integrity Rules

- 同一租户的项目、分类叶子和规范化文件编码只能对应一个未删除逻辑文件。
- 分类身份必须指向有效叶子；节点后续出现子节点时不得静默改变既有身份。
- 同一逻辑文件的大版本顺序和代码分别唯一。
- `revision_sequence` 使用 Excel 式 26 进制字母算法格式化：1=A、26=Z、27=AA、28=AB、52=AZ、53=BA；解析和格式化必须互为逆运算。
- 同一逻辑文件最多一个开放大版本。
- 同一大版本的小版本数字唯一且严格递增。
- `version_no` 必须等于 Revision 代码与 Iteration 数字的服务端格式化结果。
- 同一逻辑文件最多一个 ACTIVE 检出记录。
- 检入产生的小版本必须引用本大版本的直接前一小版本。
- 检入必须有 CONTENT 或允许 METADATA 真实差异；无差异不创建 Iteration。
- metadata-only Iteration 必须创建独立受控源文件记录，内容 SHA-256 与前驱一致，禁止复用同一个可变文件记录。
- metadata_diff_json 不允许包含项目、分类叶子或文件编码变更。
- 新大版本 `/1` 必须引用用户明确选择的当前大版本历史小版本。
- 新大版本必须同时保存实际内容来源和创建时当前正式基线；两者允许不同且不得互相补齐。
- 送审小版本必须等于 Revision 的 `latest_iteration_id`。
- 送审、批准、发布期间不得产生新的小版本。
- Master 的 `current_released_iteration_id` 必须指向状态为 ACTIVE 的小版本，并与平台 ACTIVE 引用一致。
- 发布切换必须同时更新旧正式小版本、新正式小版本、两个大版本状态、Master 指针和平台生命周期引用。
- 发布失败不得清除或替换 Master 当前正式指针。
- 历史 Revision、Iteration、Checkout、Version Event、签名和发布快照不得物理删除。
- 迁移不得更新既有签名记录的版本、哈希、签署人、签署时间或证据载荷。
- 所有写事务按 `master -> revision -> checkout -> iteration` 顺序加锁，避免相反顺序产生死锁。

## Open Questions

本阶段模型决策已收口。运行库中哪些旧 Master 满足确定性自动映射条件，仍由只读盘点结果决定。

## Design Blockers

- 未获取运行库只读盘点结果，无法确认主档拆分、合并和版本映射规模。
- 现有 `controlled_content_version_ref` 与 DCC 当前正式指针是否存在漂移尚未由运行数据验证。
- 现有审批和签名链能否无损绑定迁移后的 Iteration，仍需运行数据对账证明。
