# Database Schema Evidence

## Data change goal and affected entities

目标：按用户授权，在本地库为生产订单 `SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891` 复制一份生产用料清单测试数据，使真实页面可验证“生产用料清单”tab。

影响实体：`mes_kingdee_production_material_list`。

## Database engine and migration tool

数据库：MySQL 8，容器 `int-ruoyi-mysql`，库 `ruoyi-vue-pro`。

迁移工具：无正式迁移文件；本次为用户授权的本地测试数据写入，通过事务 SQL 执行。

## Schema, migration, fixture, seed, index, or constraint changes

未变更 schema、索引或约束。

新增本地测试数据：

- 来源生产订单：`881MO101365`
- 来源单据：`881PPBOM00001983`
- 来源产品：`AW.107.02.01.2036 / 球囊扩张压力泵`
- 目标生产订单：`SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891`
- 目标单据：`SIM-PML-CODX-PQC-WO05-001`
- 目标产品：`IDI / 按压式球囊扩充压力泵`
- 最终保留：租户 1，11 行，11 个物料编码

## Data safety analysis

Safety:

- 写入前确认目标生产订单生产用料清单数量为 0。
- 插入使用新 `source_bill_no`，不覆盖已有生产用料清单。
- 插入后发现来源跨租户复制 44 行，已删除非目标租户 121、122、162 的 33 行，只保留目标工单租户 1。
- 未修改 BOM、领料单、补料单或生产提交数据。

## Rollback or recovery plan

可回滚 SQL：

```sql
DELETE FROM mes_kingdee_production_material_list
WHERE production_order_no = 'SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891'
  AND source_bill_no = 'SIM-PML-CODX-PQC-WO05-001';
```

## BDD scenarios

BDD: 授权复制生产用料清单测试数据 -> Given 目标 SIM-COPY 生产订单没有正式生产用料清单 When 用户授权从同类压力泵生产用料清单复制一份并改为目标生产订单 Then 本地库只新增目标租户下的新生产用料清单，页面按生产订单号可查询并展示。

## RED command and expected failure

RED: `SELECT COUNT(*) FROM mes_kingdee_production_material_list WHERE production_order_no='SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891';` -> 0，目标订单复制前没有生产用料清单。

## GREEN command and passing result

GREEN: `INSERT INTO mes_kingdee_production_material_list SELECT ... FROM 881MO101365 / 881PPBOM00001983` -> PASS，最终目标租户保留 11 行。

GREEN: `PML_E2E_WORK_ORDER_CODE=SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891 node doc\tasks\20260908-production-material-list-tab\production-material-list-tab-real.e2e.cjs` -> PASS，真实页面接口返回 11 条并显示单据 `SIM-PML-CODX-PQC-WO05-001`。

## Migration verification

Verification:

只读核验：

```sql
SELECT tenant_id, COUNT(*) cnt
FROM mes_kingdee_production_material_list
WHERE production_order_no='SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891'
  AND source_bill_no='SIM-PML-CODX-PQC-WO05-001'
GROUP BY tenant_id;
```

结果：`tenant_id=1, cnt=11`。

## Blockers

无当前阻塞。
