# 任务：展厅与 DCC 租户唯一键风险只读审计

## 任务目标

只读检查系统其它模块是否存在类似“带租户表使用全局唯一 code/编号导致跨租户互相阻塞”或“查询路径绕过租户导致其它租户可见”的潜在问题。重点范围为展厅模块和 DCC 模块；本任务不修改业务代码。

## 前置任务检查

- 上一个 DCC 租户隔离审计任务 `20260530-dcc-tenant-isolation-audit` 已标记 `completed`，提交为 `80e1943800 任务: 修复DCC租户内code唯一约束`。
- 当前仓库存在与本审计无关的未提交改动，审计不回退、不提交这些改动。

## BDD 场景

- BDD: 多租户展厅业务编号不能全局互相阻塞 -> Given 两个租户维护同名或同编号展厅业务对象 / When 写入公司、产品、展厅、版本、发布资产等数据 / Then 唯一约束必须限定在租户或明确的全局命名空间内。
- BDD: 多租户 DCC 业务编号不能全局互相阻塞 -> Given 两个租户同步相同来源的 DCC 分类、岗位、目录或文件编号 / When 写入带 `tenant_id` 的 DCC 表 / Then 唯一约束不能只依赖全局业务 code。
- BDD: 普通管理查询不能跨租户可见 -> Given 租户 1 与租户 2 都有展厅或 DCC 数据 / When 用户使用普通管理接口查询 / Then 查询应由租户上下文限制，不返回其它租户数据。

## 里程碑

- [x] M1：建立只读审计任务文档并确认前置任务状态。
- [x] M2：扫描展厅模块 schema 与代码路径的唯一键、code/编号生成和租户忽略点。
- [x] M3：复核 DCC 模块剩余唯一键、手写 SQL、租户忽略点和测试服真实索引。
- [x] M4：汇总风险分级、证据、影响和建议；不进入代码修复。

## 预期验证

- 列出展厅和 DCC 带 `tenant_id` 表中不包含 `tenant_id` 的非主键唯一索引。
- 区分“同类缺陷风险”“由租户内父对象 ID 限定”“明确全局对象/令牌路径”“需要后续确认”。
- 记录只读命令和测试服查询证据，不修改业务代码。

## 审计结论

### DCC 模块

- 未发现新的确认同类缺陷。此前 DCC 分类与审批岗位 code 唯一键已经改为租户内唯一：`uk_dcc_file_category_tenant_code(tenant_id, code)`、`uk_dcc_approval_position_tenant_code(tenant_id, code)`。
- DCC 剩余不含 `tenant_id` 的唯一键主要由父对象或租户内对象 ID 间接限定，例如目录 `(parent_id, code)`、受控文件 `(category_id, file_name)`、NAS 迁移明细 `(task_id, nas_path)`。这些不属于“两个租户相同 code 互相阻塞”的同类问题。
- DCC 发现两个 `@TenantIgnore` OnlyOffice 文件读取接口，但它们依赖签名 token 读取单个预览文件，不是普通列表或管理查询路径；本次只读检查未确认存在普通跨租户可见问题。
- NAS 迁移自动生成分类 code 已包含 `tenantId` 与路径哈希，避免了不同租户同路径 DMR/DCC 分类 code 冲突。

### 展厅模块

- 确认存在一个需要修复的潜在同类风险：`showroom_release.release_id` 是全局唯一，`ShowroomReleaseAssembler.buildReleaseId()` 只由发布时间秒级值与展厅快照哈希组成，未包含 `tenantId/siteKey/stage`。如果两个租户在同一秒发布相同展厅快照，会被全局 `release_id` 唯一键互相阻塞；同一 `release_id` 还级联影响 snapshot/document/legacy projection 的全局唯一键。
- 确认存在一个旧公开路径边界风险：新的 scoped 公共接口会通过 `siteKey/stage` 解析租户并进入租户上下文；但旧 `/showroom/release/**` 与 `/showroom/assets/**` 仍允许按全局 `releaseId` 或 `assetId/contentHash` 查询 manifest/document/asset。`/showroom/release/current` 已要求站点选择器，风险主要留在旧的按 ID 查询路径。
- 发现一个需要产品确认的全局配置点：`showroom_image_prompt_version` 表带 `tenant_id`，但 DO 使用 `BaseDO + @TenantIgnore`，服务按 `scene_code/version_no` 全局维护提示词版本。如果提示词模板应租户隔离，这是同类问题；如果它是平台级共享配置，应补充设计说明并考虑清理表结构语义。
- `showroom_public_site_binding(site_key, stage)` 全局唯一更像设计约束：一个公开站点 key/stage 只能映射一个租户。只有当业务要求不同租户可复用相同站点 key/stage 时才会成为同类阻塞。
- 测试服仍有历史 `tenant_id = 0` 展厅发布记录 22 条，另有 DCC 分类历史 `tenant_id = 0` 记录 48 条。它们会被普通租户过滤隐藏，当前不是确认可见问题，但属于后续数据治理项。

### 其它模块候选

- 全库只读扫描还发现 ERP、MES、Jimu 等带 `tenant_id` 表存在不含 `tenant_id` 的唯一键。当前测试服多数候选表只有一个租户数据，未形成现场冲突证据。
- ERP/Kingdee 与 MES/Kingdee 同步记录以来源单号或来源 FID 全局唯一；若不同租户连接不同账套但来源编号可能重复，存在同类跨租户阻塞风险。
- MES 生产线 code/name、批记录模板 code、执行编号、报表编号等看起来是租户内业务编号，建议在后续专项中确认是否需要改为租户内唯一。
- Jimu 报表 code、字典 code 可能是平台级全局资源，也可能需要租户隔离；需结合该模块设计确认。

## 验证结果

- 已完成本地 schema 与 Java 代码只读扫描。
- 已完成测试服 MySQL 只读索引与数据分布查询。
- 未执行 RED/GREEN 测试，因为用户要求本轮“不要改代码，先检查”；本轮没有生产代码、SQL 或测试代码变更。
- 结论：DCC 暂无新的确认同类缺陷；展厅发布 ID 与旧公开接口存在需要优先处理的租户边界风险；其它模块存在候选风险，建议按 ERP/Kingdee、MES 业务编号、Jimu 平台资源顺序专项复核。

## Current Status

completed
