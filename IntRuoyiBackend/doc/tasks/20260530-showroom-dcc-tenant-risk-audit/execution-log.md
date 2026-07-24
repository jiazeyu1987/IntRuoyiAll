# 执行日志：展厅与 DCC 租户唯一键风险只读审计

- BDD: 多租户展厅业务编号不能全局互相阻塞 -> Given 两个租户维护同名或同编号展厅业务对象 / When 写入公司、产品、展厅、版本、发布资产等数据 / Then 唯一约束必须限定在租户或明确的全局命名空间内。
- BDD: 多租户 DCC 业务编号不能全局互相阻塞 -> Given 两个租户同步相同来源的 DCC 分类、岗位、目录或文件编号 / When 写入带 `tenant_id` 的 DCC 表 / Then 唯一约束不能只依赖全局业务 code。
- BDD: 普通管理查询不能跨租户可见 -> Given 租户 1 与租户 2 都有展厅或 DCC 数据 / When 用户使用普通管理接口查询 / Then 查询应由租户上下文限制，不返回其它租户数据。
- PRECHECK: 上一个 DCC 租户隔离审计任务 `20260530-dcc-tenant-isolation-audit` 为 `completed`，提交 `80e1943800`；当前存在 unrelated dirty changes，本任务只读审计，不回退不提交无关改动。
- READONLY: 本地扫描展厅与 DCC SQL 文件，列出带 `tenant_id` 表中不含 `tenant_id` 的非主键唯一索引。
- FINDING: DCC `dcc_file_category` 与 `dcc_approval_position` code 唯一键已包含 `tenant_id`；剩余 DCC 唯一键主要由 `parent_id/category_id/task_id` 等租户内对象 ID 间接限定。
- FINDING: DCC OnlyOffice 两个 `@TenantIgnore` 文件读取接口均依赖签名 token 读取单个文件，未发现普通管理列表或查询路径跨租户可见。
- FINDING: DCC NAS 迁移自动分类 code 生成包含 `tenantId` 与路径哈希，避免同路径跨租户 code 冲突。
- FINDING: 展厅 `showroom_release.release_id` 全局唯一；`ShowroomReleaseAssembler.buildReleaseId()` 未包含 `tenantId/siteKey/stage`，存在不同租户同秒同快照发布互相阻塞风险。
- FINDING: 展厅新的 scoped 公共接口通过 `siteKey/stage` 解析租户并切入租户上下文；旧 `/showroom/release/**` 与 `/showroom/assets/**` 仍存在按全局 ID 查询 manifest/document/asset 的边界风险。
- FINDING: `showroom_image_prompt_version` 表带 `tenant_id`，但 DO 与服务按全局 `scene_code/version_no` 维护；该点需要确认是平台级共享配置还是应租户隔离。
- TEST-SERVER: 测试服 Showroom 与 DCC 表均存在 `tenant_id` 字段；不含 `tenant_id` 的唯一键与本地 schema 风险一致。
- TEST-SERVER: 测试服 `showroom_release` 未发现 `release_id` 当前重复候选；`showroom_asset` 存在 1009 组同 `asset_id/content_hash` 跨 scope 重复，语义上是相同二进制资产复用，但旧 unscoped asset 路径存在歧义。
- TEST-SERVER: 测试服存在历史 `tenant_id=0` 记录：`showroom_release` 22 条、`dcc_file_category` 48 条；当前普通租户过滤应隐藏，但建议后续数据治理。
- READONLY: 全库扫描发现 ERP/Kingdee、MES、Jimu 等带 `tenant_id` 表也有不含 `tenant_id` 的唯一键；当前多数候选表只有单租户数据，未确认现场冲突。
- VERIFICATION: 本轮按用户要求只检查不改代码，未执行 RED/GREEN；审计结果已记录到 task.md。
