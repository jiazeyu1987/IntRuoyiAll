# 数据库 Schema 证据

## 变更目标

为流程8四份材料门禁增加服务端权威 `MATERIALS_READY` receipt 持久化，供流程10按租户、批次、来源快照和回执 hash 读取。该表只追加 receipt 版本，不更新或删除历史回执。

## 数据库与迁移工具

- 数据库：MySQL/InnoDB。
- 迁移承载：`IntRuoyiBackend/sql/mysql/` 中带 `release-migration` 头的 SQL 文件。
- 当前环境未发现宿主机 `mysql`、`mariadb`、`flyway`、`liquibase` 或 `sqlcmd` 命令；使用已运行的本地 Docker MySQL `int-ruoyi-mysql` 执行正式 migration 文件，不访问远端。

## Schema 变更

- 文件：`IntRuoyiBackend/sql/mysql/20260826_mes_edhr_material_gate_receipt.sql`。
- 新增表：`mes_pro_edhr_material_gate_receipt`。
- 约束：租户+receiptId 唯一；租户+batchExecutionId+version 唯一；批次版本查询索引。
- 完整性字段：四材料类型集合、manifest hash、source snapshot hash、材料版本集合 hash、receipt hash、签发人、审计事件和版本。

## BDD

BDD: 四份材料 receipt 持久化 -> Given 四份材料均已批准且附件 hash 和来源快照有效；When 门禁返回 `MATERIALS_READY`；Then 追加一条不可变版本 receipt，重复同一来源和 manifest 只返回同一版本。

BDD: receipt 篡改阻断 -> Given 持久化 receipt 的租户、批次、来源或 hash 任一不匹配；When 流程10读取 receipt；Then 返回空权威结果并阻断放行。

## RED/GREEN

- RED: `MesReleaseMaterialGateReceiptPortImplTest` -> 正式 DO、Mapper、adapter 不存在，测试编译失败。
- GREEN: `MesReleaseMaterialGateReceiptPortImplTest` -> `2/2 PASS`。
- GREEN: `MesReleaseMaterialGateReceiptSqlContractTest` -> `1/1 PASS`。
- GREEN: `mvn -o -pl yudao-module-mes -am -DskipTests compile` -> `BUILD SUCCESS`。
- GREEN: 本地 Docker MySQL 执行 `source /tmp/20260826_mes_edhr_material_gate_receipt.sql` -> exit 0；表为 `InnoDB/utf8mb4_unicode_ci`，18 个字段，索引为 `PRIMARY`、`uk_mes_edhr_material_gate_receipt_id`、`uk_mes_edhr_material_gate_receipt_version`、`idx_mes_edhr_material_gate_receipt_batch`，现有行数 0。
- GREEN: 同一正式 migration 第二次执行 -> exit 0，证明 `CREATE TABLE IF NOT EXISTS` 幂等；未执行 DROP 或业务数据写入。

## 数据安全与回滚

- 迁移为 `CREATE TABLE IF NOT EXISTS`，不改写历史业务表，不删除数据。
- 回滚边界：仅在确认没有 release decision 或审计引用后，通过受控迁移流程处理新增表；本任务不执行 DROP。
- 旧 receipt 不更新；材料替换通过新版本和新 receipt 表达，旧 receipt 不能通过新的 source snapshot 校验。

## 当前阻塞

- 真实文件上传、真实租户和 Playwright 放行链路未执行；单元测试和 schema 迁移不替代真实 E2E。
- 真实 Tx-C outbox 写入闭环未执行，缺少可清理的测试批次和正式来源数据。
