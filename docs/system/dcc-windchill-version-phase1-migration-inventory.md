# DCC 第一阶段 Windchill 版本迁移盘点方案

## 目的与边界

本文定义测试环境现有 DCC 数据进入 Revision/Iteration 模型前的只读盘点、确定性自动映射、人工决策输入、迁移阶段、验证和回滚边界。本文不是运行结果，也不宣称当前测试数据库已经满足迁移条件。

本方案禁止：默认项目、默认分类叶子、按文件名猜身份、把 `V1.0` 直接假定为 `A/1`、自动合并主档、自动删除重复版本或用最新记录覆盖历史。

## 当前静态基线

- `dcc_controlled_file_master` 当前身份字段为 category、directory、file_name、file_number，唯一索引不是目标复合身份。
- `dcc_controlled_file` 保存项目、分类、版本字符串、审批状态、文件指针和当前检出字段。
- 当前版本解析只接受数字段格式，不能表达 A/1。
- 当前 `current_active_controlled_file_id` 表示正式版本指针。
- 当前平台生命周期表可以约束一个 Master 的一个 ACTIVE 和一个开放候选。
- 当前上传 ticket、源文件独占、签名和受控发布已经保存部分文件哈希证据。

## 盘点冻结边界

每次盘点必须记录：

- 数据库环境和库名，不记录连接密码。
- 租户范围。
- `MAX(dcc_controlled_file_master.id)`。
- `MAX(dcc_controlled_file.id)`。
- 盘点开始和结束时间。
- 代码 commit、迁移 manifest 版本和运行后端版本。
- 盘点查询版本和导出文件 SHA-256。

同一份报告的所有查询必须使用相同的最大 ID 边界。数据库仍有写入时，报告只用于发现问题，不能直接作为正式迁移输入。正式迁移前需要在维护窗口重新执行并复核。

## 必查项目

### I-01 主档缺少目标身份

逐个 Master 汇总其所有 Iteration 的：

- `dcc_project_code_id`
- `file_type_taxonomy_id`
- 规范化 `file_number`

任一字段全部为空、同一 Master 出现多个不同非空值、或版本记录与 Master 文件编码冲突时均为 blocker。

### I-02 分类叶子合法性

确认每个 `file_type_taxonomy_id`：

- 节点存在且未删除；
- 节点启用；
- 没有启用子节点；
- 当前文件上的层级快照与正式分类路径不存在无法解释的冲突。

### I-03 目标身份跨 Master 重复

只对 I-01 已形成唯一候选身份的 Master 分组：

```text
tenant + dccProjectCodeId + taxonomyLeafId + normalizedFileNumber
```

同一组出现多个 Master 时必须人工决定合并、拆分或修正；系统不自动选择文件更多或更新时间更晚的 Master。

### I-04 当前正式版本完整性

检查：

- Master 正式指针是否指向本 Master 的未删除文件；
- 被指向文件状态是否为当前允许的正式状态；
- 一个 Master 是否存在多个 ACTIVE/APPROVED 候选；
- ACTIVE 文件是否缺少发布文件、发布时间或必要文件记录；
- 新旧版本 `superseded_by_file_id` 是否形成断链或跨 Master。

### I-05 版本序列可解释性

按 Master 导出所有 `version_no`、status、submitted/published/create time 和 ID。以下情况均需人工映射：

- 空版本号或非法数字段版本；
- 相同 Master 重复版本号；
- 版本号顺序与时间、发布状态明显冲突；
- 同一版本号存在不同文件哈希；
- 当前正式版本不是可解释的最高正式版本；
- 历史记录不足以判断哪些旧版本属于同一大版本的小版本。

旧系统没有 Windchill 小版本语义，因此确定性自动映射只采用“大版本 `/1`”策略，不把旧 `V1.1` 猜成 A/2。满足以下全部条件时允许自动形成候选映射：

- Master 目标身份唯一且所有版本一致；
- 每个旧 `version_no` 唯一并能被现有数字段解析器排序；
- 没有运行中、待审批、待发布、发布失败、驳回或撤回记录；
- 多版本链除最后一条外均为 SUPERSEDED，最后一条为 ACTIVE；或者单版本作废链为 OBSOLETE；
- Master 当前正式指针与最后 ACTIVE 一致；
- 文件、哈希、签名和平台生命周期检查通过。

映射规则：按旧业务版本数字顺序，依次映射为 A/1、B/1、C/1；第 27 个旧业务版本映射为 AA/1。任一条件不满足时整条 Master 阻塞，不能只迁移其中“看起来正常”的部分。

### I-06 检出状态

检查当前 `checked_out_by/time`：

- 检出人不存在或停用；
- 检出版本不是可编辑工作版本；
- 同一 Master 多个版本同时带检出人；
- 检出时间为空或异常；
- 检出版本正在审批或已经正式发布。

迁移时每个合法检出转换为一条 ACTIVE Checkout；无法解释的检出必须在维护窗口前人工处理。

### I-07 文件与哈希

检查每个待迁移 Iteration 的 source/original/published/stamped 文件引用、源文件独占记录和 SHA-256。共享 source_file_id、文件记录缺失、文件内容不可读或哈希不一致均为 blocker。

### I-08 审批、签名和路线快照

检查每个 process_instance_id：

- 是否唯一绑定一个现有 controlled_file_id；
- 审批状态与文件状态是否一致；
- 路线快照是否存在且人员可解析；
- 签名记录的 revisionId、versionNo、sourceFileId 和哈希是否与文件一致；
- 被驳回、撤回和已批准记录是否仍可定位。

旧签名中的 versionNo、记录哈希和证据载荷必须原样保留。迁移映射只能在 Iteration 上增加新的 Revision/Iteration 身份和 `legacy_version_no`，不能回写签名快照。

### I-09 平台生命周期一致性

按 Master 对账：

- `controlled_content_version_ref.native_master_id` 是否等于 Master；
- ACTIVE ref 是否等于 Master 当前正式指针；
- 是否存在多个 ACTIVE 或多个开放候选；
- 候选的 native_version_id 是否等于当前送审或待发布版本；
- transition audit 是否覆盖关键状态变化。

### I-10 关联、分发、培训和发布证据

统计所有绑定 controlled_file_id 的关联文件、分发、培训、打印、访问、签名和发布记录。迁移必须保持原 Iteration ID，避免这些历史关联整体重写。

## 只读查询模板

以下为设计级模板，执行前必须根据真实运行库 schema 校正并记录查询版本。

### 同一 Master 身份混合

```sql
SELECT tenant_id,
       master_id,
       COUNT(DISTINCT dcc_project_code_id) AS project_count,
       COUNT(DISTINCT file_type_taxonomy_id) AS taxonomy_count,
       COUNT(DISTINCT UPPER(TRIM(file_number))) AS file_number_count,
       SUM(dcc_project_code_id IS NULL) AS missing_project_rows,
       SUM(file_type_taxonomy_id IS NULL) AS missing_taxonomy_rows,
       SUM(TRIM(COALESCE(file_number, '')) = '') AS missing_file_number_rows
FROM dcc_controlled_file
WHERE deleted = 0
  AND id <= :snapshot_max_file_id
GROUP BY tenant_id, master_id
HAVING project_count <> 1
    OR taxonomy_count <> 1
    OR file_number_count <> 1
    OR missing_project_rows > 0
    OR missing_taxonomy_rows > 0
    OR missing_file_number_rows > 0;
```

### 同一目标身份对应多个 Master

```sql
WITH stable_identity AS (
  SELECT tenant_id,
         master_id,
         MIN(dcc_project_code_id) AS project_id,
         MIN(file_type_taxonomy_id) AS taxonomy_id,
         MIN(UPPER(TRIM(file_number))) AS normalized_file_number
  FROM dcc_controlled_file
  WHERE deleted = 0
    AND id <= :snapshot_max_file_id
  GROUP BY tenant_id, master_id
  HAVING COUNT(DISTINCT dcc_project_code_id) = 1
     AND COUNT(DISTINCT file_type_taxonomy_id) = 1
     AND COUNT(DISTINCT UPPER(TRIM(file_number))) = 1
     AND SUM(dcc_project_code_id IS NULL) = 0
     AND SUM(file_type_taxonomy_id IS NULL) = 0
)
SELECT tenant_id, project_id, taxonomy_id, normalized_file_number,
       COUNT(*) AS master_count,
       GROUP_CONCAT(master_id ORDER BY master_id) AS master_ids
FROM stable_identity
GROUP BY tenant_id, project_id, taxonomy_id, normalized_file_number
HAVING COUNT(*) > 1;
```

### 多个当前正式版本或指针漂移

```sql
SELECT m.tenant_id,
       m.id AS master_id,
       m.current_active_controlled_file_id,
       SUM(f.status IN ('ACTIVE', 'APPROVED')) AS formal_candidate_count,
       SUM(f.id = m.current_active_controlled_file_id
           AND f.status IN ('ACTIVE', 'APPROVED')) AS pointer_match_count
FROM dcc_controlled_file_master m
LEFT JOIN dcc_controlled_file f
  ON f.master_id = m.id
 AND f.tenant_id = m.tenant_id
 AND f.deleted = 0
WHERE m.deleted = 0
  AND m.id <= :snapshot_max_master_id
GROUP BY m.tenant_id, m.id, m.current_active_controlled_file_id
HAVING formal_candidate_count > 1
    OR pointer_match_count <> CASE
         WHEN m.current_active_controlled_file_id IS NULL THEN 0 ELSE 1 END;
```

### 重复版本号

```sql
SELECT tenant_id, master_id, version_no, COUNT(*) AS row_count,
       GROUP_CONCAT(id ORDER BY id) AS controlled_file_ids
FROM dcc_controlled_file
WHERE deleted = 0
  AND id <= :snapshot_max_file_id
GROUP BY tenant_id, master_id, version_no
HAVING COUNT(*) > 1;
```

### 同一 Master 多个检出版本

```sql
SELECT tenant_id, master_id, COUNT(*) AS checkout_count,
       GROUP_CONCAT(id ORDER BY id) AS controlled_file_ids
FROM dcc_controlled_file
WHERE deleted = 0
  AND checked_out_by IS NOT NULL
  AND id <= :snapshot_max_file_id
GROUP BY tenant_id, master_id
HAVING COUNT(*) > 1;
```

### 平台 ACTIVE 与 DCC 指针漂移

```sql
SELECT m.tenant_id,
       m.id AS master_id,
       m.current_active_controlled_file_id AS dcc_active_id,
       ref.native_version_id AS platform_active_id
FROM dcc_controlled_file_master m
LEFT JOIN controlled_content_version_ref ref
  ON ref.tenant_id = m.tenant_id
 AND ref.content_type = 'DCC_CONTROLLED_FILE'
 AND ref.native_master_id = m.id
 AND ref.canonical_status = 'ACTIVE'
 AND ref.deleted = b'0'
WHERE m.deleted = 0
  AND m.id <= :snapshot_max_master_id
  AND NOT (m.current_active_controlled_file_id <=> ref.native_version_id);
```

## 人工映射包

每个现有 Master 必须有一行主档决策，并为每个 controlled_file_id 提供一行版本决策。

主档决策字段：

- tenantId
- oldMasterId
- targetDccProjectCodeId
- targetFileTypeTaxonomyId
- targetFileNumber
- normalizedFileNumber
- targetMasterGroupKey
- decisionAction：AUTO_MAP、KEEP、MERGE_INTO、SPLIT_TO、BLOCK
- decisionOwner
- decisionReason
- approvalReference

版本决策字段：

- oldControlledFileId
- targetMasterGroupKey
- revisionSequence
- revisionCode
- iterationNo
- legacyVersionNo
- predecessorControlledFileId
- revisionSourceControlledFileId
- currentReleased
- targetIterationStatus
- decisionOwner
- decisionReason

映射包必须有 schema 版本、生成时间、确认人、确认时间和 SHA-256。AUTO_MAP 行由确定性规则产生并携带规则版本；人工行必须明确确认。回填只接受状态 CONFIRMED 且哈希一致的映射包。

## 迁移阶段

### Phase M0：只读盘点

- 在当前运行库执行全部盘点查询。
- 输出总量、阻塞类型、样本、逐主档明细和文件哈希可用性。
- 不修改任何业务记录。

### Phase M1：人工决策

- 系统生成 AUTO_MAP 候选和 blocker 列表。
- 测试环境负责人确认 AUTO_MAP 总数、样本和规则版本。
- 文控和业务负责人只处理 blocker。
- 形成并确认映射包。
- 未解决项必须保持 BLOCK，不得跳过。

### Phase M2：Additive Schema

- 新增 Revision、Checkout、Version Event、发布快照表和新列。
- 运行 schema postflight，确认旧代码尚未写入新字段。

### Phase M3：维护窗口回填

- 冻结 DCC 写入口。
- 校验映射包 hash、规则版本和冻结边界。
- 按 Master ID 有序分批回填，记录每批计数和摘要。
- 任一批失败立即停止，不继续后续批次。

### Phase M4：约束切换

- 验证新身份唯一、Revision/Iteration 唯一、单开放 Revision 和单 ACTIVE Checkout。
- 建立新唯一索引。
- 移除旧身份索引和旧检出字段。
- 重新对账平台生命周期。

### Phase M5：应用切换

- 部署新后端和前端。
- 执行 API、服务和页面定向回归。
- 经授权后执行两条真实前端流程验收。

## Postflight Gates

以下条件必须全部为零或完全相等：

- 未映射 Master 数。
- 未映射 controlled_file 数。
- 目标身份重复数。
- Revision 代码/顺序重复数。
- Iteration 数字重复或断裂数。
- Revision latest/submitted/released 指针漂移数。
- Master current released 指针漂移数。
- 多开放 Revision 数。
- 多 ACTIVE Checkout 数。
- DCC 与平台 ACTIVE/候选漂移数。
- 丢失审批、签名、关联、分发、培训、打印或访问历史的记录数。
- 文件记录缺失或已记录 SHA-256 不一致数。
- 既有签名版本标签、签名哈希或证据载荷变化数。

任何非零 blocker 都阻止恢复 DCC 写入口。

## 回滚和恢复

- M2 完成但 M3 未开始：可以回退旧应用，新表保持未使用状态。
- M3 开始后：必须能够从维护窗口前数据库和文件存储恢复点整体恢复。
- M4/M5 后产生新业务写入：禁止旧应用直接接管，只能向前修复或整体恢复切换前快照。
- 回滚演练必须验证当前正式文件预览、下载、审批历史、签名证据和平台 ACTIVE 指针。

## 当前阻塞与所需输入

- 尚未连接运行库执行只读盘点，因此所有数据问题数量未知。
- 尚未形成旧 `version_no` 到 Revision/Iteration 的逐主档人工映射。
- 尚未配置用于第一阶段验收的项目 OWNER、EDIT、VIEW 测试规则。
- 尚未确定测试环境迁移维护时间和测试快照保存位置。
