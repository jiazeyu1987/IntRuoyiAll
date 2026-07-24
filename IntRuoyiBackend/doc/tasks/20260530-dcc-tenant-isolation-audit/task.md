# 任务：DCC 租户隔离与唯一键风险审计

## 任务目标

检查 DCC 与 NAS 转移相关系统中是否还存在类似“跨租户唯一 code 冲突”或“租户 1 数据被租户 2 可见”的潜在问题。重点审计数据库唯一约束、自动编码生成逻辑、DCC/NAS 转移查询与写入的 `tenant_id` 隔离条件。

## 前置任务检查

- 上一个同类 DHF 迁移任务 `20260530-dcc-dhf-nas-transfer-completion` 已标记 `completed`，提交为 `655e36b09d 任务: 完成DHF迁移修复验证`。
- 当前仓库存在与本审计无关的未提交改动，审计不回退、不提交这些改动。

## BDD 场景

- BDD: 自动生成的 DCC NAS 分类 code 必须租户隔离 -> Given 两个租户迁移相同 NAS 路径 / When 系统生成自动分类 code / Then 不同租户必须生成不同 code，不能因全局唯一键互相阻塞。
- BDD: DCC 用户只能看到本租户迁移结果 -> Given 租户 1 与租户 2 都存在 DCC 迁移数据 / When 用户在某个租户下查询 DCC 分类、受控文件或 NAS 转移任务 / Then 查询必须限制在当前租户，不能返回其它租户数据。
- BDD: 全局唯一约束必须有明确业务边界 -> Given 数据表带 `tenant_id` 且存在唯一索引 / When 唯一索引未包含 `tenant_id` / Then 必须确认该字段确实是全局唯一业务标识，否则记录为风险或缺陷。

## 里程碑

- [x] M1：建立任务文档并确认前置任务状态。
- [x] M2：扫描 DCC 相关唯一约束与 `code` 生成逻辑。
- [x] M3：核查 NAS 转移与 DCC 查询/写入是否带租户条件。
- [x] M4：记录发现、风险等级、验证证据；确认缺陷后完成 RED/GREEN 修复并迁移测试服。

## 预期验证

- 能列出 DCC 相关含 `tenant_id` 表上的唯一约束，区分“已包含租户”“全局唯一但有业务理由”“疑似风险”。
- 能列出 DCC/NAS 自动 code 生成点，并确认是否包含租户或全局命名空间。
- 能通过测试或代码证据确认本次 DHF 修复覆盖了已知问题。

## 审计结论

- 已确认并修复两个与 DHF NAS 分类相同性质的潜在跨租户冲突：`dcc_file_category.code`、`dcc_approval_position.code` 原为全局唯一，但同步 IntAuth 时会生成 `INTAUTH-<sourceId>`，不同租户同步同一来源 ID 会互相阻塞。
- 修复方式：将测试服与基线 schema 中两个唯一约束调整为 `tenant_id, code`，保持同一租户内唯一，同时允许不同租户使用相同来源 code。
- 测试服已确认所有 `dcc_%` 表都有 `tenant_id`；剩余不含 `tenant_id` 的非主键唯一索引均绑定 `category_id`、`task_id`、`plan_id`、`snapshot_id`、`descriptor_id`、`user_id` 等对象 ID 链路，未发现同类全局业务 code 风险。
- DCC/NAS 后台定时任务通过 `TenantUtils.execute(tenantId, ...)` 逐租户执行；前台 DCC 列表/管理接口未标注 `@TenantIgnore`，由 `TenantDatabaseInterceptor` 对 DCC 表自动追加租户条件。仅 OnlyOffice 文件读取接口使用 `@TenantIgnore + token`，属于脱离登录态的令牌读取入口，不是普通租户列表可见性路径。
- 当前结论：普通 DCC 迁移、分类、受控文件和任务列表不会让租户 2 看到租户 1 的数据；本次发现的“跨租户错误”是数据库唯一键冲突，不是跨租户可见。

## 验证结果

- GREEN：`mvn -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldScopeDccCodeUniquenessByTenant" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。
- REGRESSION：`mvn -pl yudao-module-infra,yudao-module-dcc -am "-Dtest=S3FileClientPathTest,DccBaseSchemaTest,DccControlledFileNasTransferServiceTest,DccFileCategoryAdminServiceImplTest,DccApprovalPositionAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过，42 tests。
- REGRESSION：`python -X utf8 -m pytest script/tests/test_dcc_nas_acl_snapshot_restore_sql.py script/tests/test_tenant_clone_schema.py -q` 通过，7 tests。
- REGRESSION：`python -X utf8 -m pytest script/tests/test_dcc_sql_scripts.py script/tests/test_dcc_nas_acl_snapshot_restore_sql.py script/tests/test_tenant_clone_schema.py -q` 通过，13 tests。
- TEST-SERVER：测试服 `172.30.30.58` 已应用 `20260530_dcc_tenant_scoped_code_indexes.sql`；`dcc_file_category` 与 `dcc_approval_position` 唯一键均为 `tenant_id,code`。
- TEST-SERVER：事务烟雾测试确认不同租户同 `code` 可插入，回滚后 `CODEX_SMOKE` 行数为 0。
- TEST-SERVER：`http://172.30.30.58:48081/actuator/health` 返回 `{"status":"UP"}`。

## Current Status

completed
