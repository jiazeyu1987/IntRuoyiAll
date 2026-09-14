# 数据模型设计

## 复用现有模型

实现前必须核对现有 QA 表的真实字段和状态。优先复用：

- `mes_qa_inspection_regulation`
- `mes_qa_inspection_regulation_version`
- `mes_qa_inspection_regulation_item`
- 现有 QA 工序/检验项目关联表
- 正式产品主数据表

## 新增关系表

建议：`mes_qa_common_regulation_product_binding`

字段：

- `id`
- `tenant_id`
- `product_id`
- `regulation_id`
- `regulation_version_id`
- `scope_code`：固定 `COMMON_PACKAGING`
- `dcc_project_code_id`
- `binding_status`：`ENABLED` / `DISABLED`
- `active_binding_key`：启用时为 product_id 字符串，停用时为 NULL
- `version`
- `idempotency_key`
- `change_reason`
- `created_by`、`created_at`、`updated_by`、`updated_at`
- `deleted`

## 约束

- 外键语义由服务层按租户校验，不能直接复制跨租户 ID。
- `product_id`、`regulation_id`、`regulation_version_id` 必填。
- 同租户同产品只能有一条当前启用关系。
- 版本停用不删除历史绑定。
- 已有历史引用时禁止物理删除规程版本。

## 索引

- `tenant_id + product_id + binding_status`
- `tenant_id + regulation_version_id + binding_status`
- `tenant_id + product_id + binding_status`
- `tenant_id + dcc_project_code_id + regulation_id + regulation_version_id`
- `tenant_id + active_binding_key` 唯一索引

## 新增版本来源表

建议：`mes_qa_common_regulation_version_source`

字段：

- `id`
- `tenant_id`
- `regulation_id`
- `regulation_version_id`
- `source_sort`：来源文件在该版本内的顺序
- `source_file_name`
- `source_document_code`
- `source_version_no`
- `packaging_stage`：如 `INITIAL_PACKAGING`、`MIDDLE_LARGE_PACKAGING`、`MEILIAN_INITIAL_PACKAGING`
- `source_sha256`
- `parsed_process_count`
- `parsed_item_count`
- `parse_summary_json`
- `created_by`、`created_at`、`updated_by`、`updated_at`
- `deleted`

约束：

- 同一版本内 `source_sort` 唯一。
- 同一版本内 `source_sha256` 不允许重复。
- 版本发布后来源表不可修改，只能复制新版本。
- 多文件导入为一个事务：任一来源文件解析失败时，不创建或更新草稿内容。

`parse_summary_json` 最小结构：

```json
{
  "headers": ["序号", "检验项目", "接受标准", "检验方法", "检具", "检验规则"],
  "warnings": [
    {
      "code": "SOURCE_SCHEMA_STYLE_WARNING",
      "severity": "WARN",
      "blocking": false,
      "message": "styles.xml uiPriority 顺序问题"
    }
  ],
  "rows": [
    {
      "wordTableNo": 2,
      "wordRowNo": 2,
      "itemName": "外观",
      "standardExcerpt": "包装封口应平整连续...",
      "methodExcerpt": "在正常视力或矫正视力...",
      "toolText": "目测",
      "samplingPlanText": "首件：20件巡检：GB/T 2828.1..."
    }
  ]
}
```

## 新增来源项目映射表

建议：`mes_qa_common_regulation_version_item_source`

字段：

- `id`
- `tenant_id`
- `regulation_id`
- `regulation_version_id`
- `source_id`
- `qa_item_id`：正式 QA item 主键，发布或保存项目时由服务端写入
- `process_code`
- `item_code`
- `word_table_no`
- `word_row_no`
- `source_item_name`
- `source_standard_excerpt`
- `source_method_excerpt`
- `source_tool_text`
- `source_sampling_plan_text`
- `condition_type`：`NONE` / `PRODUCT_SET_REQUIRED`
- `condition_text`：例如 `百瑞吉产品要求`
- `condition_product_ids_json`：QA 管理员确认后的正式产品 ID 列表；禁止从名称自动推断
- `created_by`、`created_at`、`updated_by`、`updated_at`
- `deleted`

约束：

- 同一版本内 `source_id + word_table_no + word_row_no + item_code` 唯一。
- 来源映射优先通过 `qa_item_id` 关联正式项目；草稿导入初期允许先落 `item_code`，发布前必须解析为正式 `qa_item_id`，否则阻断发布。
- `condition_type=PRODUCT_SET_REQUIRED` 时，发布前 `condition_product_ids_json` 必须非空，否则阻断发布。
- 发布快照中的每个项目必须包含该映射表的来源证据，保证 A 双来源版本可逐项追溯。

## 新增版本适用性表

建议：`mes_qa_common_regulation_version_applicability`

字段：

- `id`
- `tenant_id`
- `regulation_id`
- `regulation_version_id`
- `applicability_type`：`ALL_PRODUCTS` / `PRODUCT_SET`
- `product_id`
- `reason`
- `created_by`、`created_at`、`updated_by`、`updated_at`
- `deleted`

约束：

- 文件名、规程名称、产品代际文本不得自动生成适用产品；仅 QA 管理员显式选择的 `productId` 生效。
- `PRODUCT_SET` 版本只能绑定表内产品；`ALL_PRODUCTS` 版本允许绑定任意通过租户/DCC 校验的正式产品。
- B 样本中的“美联”默认视为需要 QA 管理员确认的适用性线索；若选择 `PRODUCT_SET`，绑定时必须校验产品 ID 在适用表内。

## 迁移与回滚

- SQL 必须带 `release-migration` 元数据。
- 迁移前只读检查现有表、字段、唯一约束和历史重复数据。
- 迁移只新增表、索引和必要菜单，不删除或改写既有 QA 数据。
- 回滚优先停用新增菜单/绑定入口；关系表尚无业务写入时才允许评估删除，已有数据时不得自动删除。
- 退休事务锁定版本及全部启用绑定，任一步失败回滚；重复 change/retire 使用 `expectedVersion + idempotencyKey` 幂等处理。
