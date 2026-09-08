# 后端/API 设计

## 模块边界

- 所属模块：`yudao-module-mes` QA/生产过程域，复用现有 QA 规程服务、产品物料服务、租户和权限框架。
- 新增能力：通用规程筛选、产品绑定 CRUD、绑定变更事务、版本状态门禁。
- 不新增活跃订单或 PQC 执行入口。

## 建议接口

### 通用规程

- `GET /admin-api/mes/qa/common-regulation/page`
- `GET /admin-api/mes/qa/common-regulation/get?id={id}`
- `POST /admin-api/mes/qa/common-regulation/create`
- `PUT /admin-api/mes/qa/common-regulation/update`
- `POST /admin-api/mes/qa/common-regulation/version/copy`
- `POST /admin-api/mes/qa/common-regulation/version/import-word-draft`
- `POST /admin-api/mes/qa/common-regulation/version/publish`
- `POST /admin-api/mes/qa/common-regulation/version/retire`
- `GET /admin-api/mes/qa/common-regulation/dcc-options`
- `GET /admin-api/mes/qa/common-regulation/source-stage-options`

### 产品绑定

- `GET /admin-api/mes/qa/common-regulation-binding/page`
- `GET /admin-api/mes/qa/common-regulation-binding/get?id={id}`
- `POST /admin-api/mes/qa/common-regulation-binding/create`
- `POST /admin-api/mes/qa/common-regulation-binding/change-version`
- `POST /admin-api/mes/qa/common-regulation-binding/disable`
- `GET /admin-api/mes/qa/common-regulation-binding/product-options`
- `GET /admin-api/mes/qa/common-regulation-binding/version-options?regulationId={id}&dccProjectCodeId={id}`

## 请求/响应约束

通用规程主档创建请求：

```json
{
  "dccProjectCodeId": 501,
  "scopeCode": "COMMON_PACKAGING",
  "regulationCode": "COMMON-PACK-A",
  "regulationName": "包装检验 A",
  "remark": "适用于 QA 管理员确认的产品集合",
  "expectedVersion": 1,
  "idempotencyKey": "create-common-pack-a"
}
```

主档响应至少包含：`regulationId`、`dccProjectCodeId`、DCC 项目代码/名称、`scopeCode`、`regulationCode`、`regulationName`、当前版本、来源文件数量、生命周期状态、`version`。`scopeCode` 只能由后端持久化为 `COMMON_PACKAGING`，请求中其它值直接拒绝。

复制版本请求：

```json
{
  "sourceVersionId": 3001,
  "targetVersionNo": "A-2026-02",
  "expectedVersion": 3,
  "idempotencyKey": "copy-a-2026-02"
}
```

复制响应至少包含：`regulationId`、`draftVersionId`、`targetVersionNo`、`sourceDocuments[]`、`processCount`、`itemCount`、`version`。复制必须完整复制来源文件记录、项目来源映射和条件适用性，但新版本状态为 `DRAFT`。

绑定创建请求：

```json
{
  "productId": 1001,
  "regulationId": 2001,
  "regulationVersionId": 3001,
  "expectedVersion": 1,
  "idempotencyKey": "bind-pump-1001-001"
}
```

`expectedVersion` 对应被修改资源的乐观锁版本：创建主档时固定传 `1` 表示客户端基于“资源不存在”的创建语义；复制/导入/发布/退休版本时对应 `regulation_version.version` 或主档 `version`；绑定 create/change/disable 时对应绑定资源或产品当前启用绑定检查版本。

绑定响应至少包含：`id`、`productId`、产品编码/名称/规格、`dccProjectCodeId`、`regulationId`、规程名称、`regulationVersionId`、版本号、版本状态、`scopeCode`、来源文件数量、包装阶段、工序数量、项目数量、绑定状态、`version`、创建/变更时间。

多文件导入请求使用 `multipart/form-data`：

- `regulationId`
- `dccProjectCodeId`
- `versionNo`
- `expectedVersion`
- `idempotencyKey`
- `files[]`：按前端展示顺序上传一份或多份 Word 文件。
- `sourceStages[]`：与 `files[]` 一一对应，取值来自后端字典，不允许前端自由文本。

导入响应至少包含：`regulationId`、`draftVersionId`、`versionNo`、`sourceDocuments[]`、`processCount`、`itemCount`、`conditionalStandards[]`、`warnings[]`。`sourceDocuments[]` 包含来源文件名、来源规程编码、来源版本、包装阶段、解析工序数、解析项目数和 sha256。`conditionalStandards[]` 包含检测到的产品/客户特定标准线索，例如 `百瑞吉产品要求`，发布前必须由 QA 管理员绑定正式 `productId` 或删除该条件标准。

发布版本请求：

```json
{
  "regulationId": 2001,
  "draftVersionId": 3001,
  "applicabilityType": "PRODUCT_SET",
  "applicableProductIds": [1001, 1002],
  "conditionalStandardProductMap": {
    "sourceItemId:9002": [1015]
  },
  "expectedVersion": 4,
  "idempotencyKey": "publish-a-2026-01"
}
```

发布响应至少包含：`publishedVersionId`、`lifecycleStatus=PUBLISHED`、`sourceDocuments[]`、`applicabilityType`、`versionApplicableProductIds`、`conditionalStandards[]`、`processCount`、`itemCount`、`warnings[]`。存在 `condition_type=PRODUCT_SET_REQUIRED` 且未填写产品 ID 时必须失败。`versionApplicableProductIds` 表示整个版本可绑定的产品集合；`conditionalStandards[].conditionProductIds` 只表示该条件标准适用的产品集合，两者不能混用。

退休版本请求：

```json
{
  "regulationId": 2001,
  "regulationVersionId": 3001,
  "changeReason": "被新包装检验版本替代",
  "expectedVersion": 5,
  "idempotencyKey": "retire-a-2026-01"
}
```

退休响应至少包含：`regulationVersionId`、`lifecycleStatus=RETIRED`、停用绑定数量、审计事件 ID、`version`。幂等只在同一 `idempotencyKey + requestFingerprint` 重复提交时返回原结果；同 key 不同请求返回 `COMMON_REGULATION_IDEMPOTENCY_CONFLICT`；版本已由其它请求退休时返回 `COMMON_REGULATION_VERSION_ALREADY_RETIRED`，不得伪装为本次成功。

绑定变更请求：

```json
{
  "productId": 1001,
  "fromBindingId": 7001,
  "toRegulationId": 2002,
  "toRegulationVersionId": 3002,
  "changeReason": "产品切换包装检验 B",
  "expectedVersion": 2,
  "idempotencyKey": "change-pump-1001-to-b"
}
```

停用绑定请求：

```json
{
  "bindingId": 7001,
  "changeReason": "产品停止使用该通用包装规程",
  "expectedVersion": 2,
  "idempotencyKey": "disable-binding-7001"
}
```

绑定变更/停用响应均返回当前绑定状态、旧绑定状态、新绑定 ID、审计事件 ID 和 `version`。

`warnings[]` 结构：

```json
{
  "sourceFileName": "A-01-PQC-CR-003.docx",
  "code": "SOURCE_SCHEMA_STYLE_WARNING",
  "severity": "WARN",
  "blocking": false,
  "message": "styles.xml uiPriority 顺序问题"
}
```

仅 `blocking=false` 的非内容类 warning 允许发布；检验规则表不可读、必填表头缺失、必填单元格为空、条件标准未确认产品 ID 必须返回 `blocking=true` 并阻断发布。

## 权限

- 查询：`mes:qa-common-regulation:query`、`mes:qa-common-regulation-binding:query`
- 创建/编辑：`mes:qa-common-regulation:create`、`mes:qa-common-regulation:update`
- 发布/退休：`mes:qa-common-regulation:publish`、`mes:qa-common-regulation:retire`
- 绑定维护：`mes:qa-common-regulation-binding:create`、`mes:qa-common-regulation-binding:change`、`mes:qa-common-regulation-binding:disable`

最终 permission 字符串、菜单 ID 和角色绑定必须以真实菜单/权限 schema 核对后确定，本文不写死 ID。

## 服务校验

1. 当前租户上下文存在。
2. 产品、规程、版本都属于当前租户且未删除。
3. 版本状态为现有 `PUBLISHED`，scope 固定为 `COMMON_PACKAGING`，DCC 必须与规程主档一致。
4. 产品与版本关系未重复。
5. 目标产品没有其它启用绑定，或请求明确走 change-version 事务；客户端不得覆盖服务端派生的 scope/status/DCC。
6. 绑定版本内容完整。
7. 通用版本发布前至少有一个来源文件，且每个来源文件至少解析出一个工序和一个检验项目。
8. 多文件导入接受同义表头：`检具` 等价于 `检验器具及设备`，`检验规则` 等价于 `抽样方案`；不在白名单内的表头直接失败。
9. A 双来源版本必须在同一 `regulationVersionId` 下保留两条 source 记录；B 单来源版本不能继承或补齐 A 的第二来源。
10. 历史 Word 文件允许存在非内容类 OpenXML schema 警告，但导入响应必须记录 `warnings[]`；检验规则表不可读、必填列缺失或必填单元格为空仍为失败。
11. 来源内容出现产品/客户特定标准时，必须生成条件标准待确认项；发布前未绑定正式 `productId` 的条件标准阻断发布，禁止按“百瑞吉”“美联”等文本自动匹配产品。
12. 产品绑定时若版本适用性为 `PRODUCT_SET`，后端必须校验 `productId` 在版本适用产品表内。

## 事务与并发

- create：锁定产品绑定业务范围，查询当前启用绑定，唯一校验后插入。
- change-version：同一事务锁定产品绑定范围，旧绑定设为 `DISABLED`，新绑定设为 `ENABLED` 并立即生效。
- retire-version：锁定版本及全部绑定；存在启用绑定时先停用绑定再退休版本，任一失败全部回滚；幂等只按同一 key 与同一请求指纹返回原结果。
- 并发冲突返回明确业务错误，禁止取第一条继续。
- 数据库使用 `active_binding_key` 生成列和 `tenant_id + active_binding_key` 唯一索引；不支持生成列时使用 `SELECT ... FOR UPDATE` 锁定 `tenant_id + product_id`，冲突不自动重试。

## 错误

- `COMMON_REGULATION_NOT_EXISTS`
- `COMMON_REGULATION_VERSION_NOT_PUBLISHED`
- `COMMON_REGULATION_VERSION_IMMUTABLE`
- `COMMON_REGULATION_BINDING_DUPLICATE`
- `COMMON_REGULATION_BINDING_PRODUCT_NOT_EXISTS`
- `COMMON_REGULATION_BINDING_CONTENT_INVALID`
- `COMMON_REGULATION_BINDING_CONFLICT`
- `COMMON_REGULATION_PRODUCT_CONTEXT_MISSING`
- `COMMON_REGULATION_TENANT_MISMATCH`
- `COMMON_REGULATION_DCC_CONTEXT_MISMATCH`
- `COMMON_REGULATION_SOURCE_DOCUMENT_INVALID`
- `COMMON_REGULATION_SOURCE_DOCUMENT_DUPLICATE`
- `COMMON_REGULATION_SOURCE_STAGE_MISMATCH`
- `COMMON_REGULATION_CONDITIONAL_STANDARD_UNCONFIRMED`
- `COMMON_REGULATION_APPLICABILITY_MISMATCH`
- `COMMON_REGULATION_IDEMPOTENCY_CONFLICT`
- `COMMON_REGULATION_VERSION_ALREADY_RETIRED`

所有 page/get/product-options/version-options 自动注入当前 tenant 且过滤 `deleted=0`。草稿允许不完整保存，publish/retire 必须完整性校验；copy 请求包含 sourceVersionId、目标 versionNo、expectedVersion、idempotencyKey，versionNo 在 tenant+regulationId 内唯一。`dcc-options` 只返回可创建通用包装规程的 DCC 项目；page/get/version-options 响应必须显示 DCC 项目代码、名称、scope、版本适用产品集合、条件标准适用产品集合和来源摘要。

## 观测

记录创建、发布、退休、绑定、变更绑定、停用绑定、导入来源文件和条件标准确认的操作审计；日志只记录业务 ID、来源 sha256、warning code 和结果，不记录密码、token 或完整敏感配置。
