# Verification Report

## Summary

- PASS: 真实前端路径 `http://127.0.0.1:8081/mes/pro/process-pool/production-leader` 登录 `芋道源码/admin`，打开“活跃订单池”，新增 2 条 `球囊扩张压力泵` 活跃订单。
- PASS: 页面负责路线包含 `球囊扩张压力泵` 与 `按压式球囊扩充压力泵`。
- PASS: 活跃订单池最终共 7 条 ACTIVE：5 条 `按压式球囊扩充压力泵`，2 条 `球囊扩张压力泵`。

## Data Corrections

- PASS: 使用正式移除接口将 5 条任务残留活跃订单 `41..45` 置为 REMOVED；这些行均为 `CODX-PQC-20260807-SP` 任务数据且指向已删除路线。
- PASS: 精确软删除 3 条孤儿 `mes_pro_route_product` 绑定 `922291..922293`，它们指向已删除 E2E 路线 `922138`；事务计数 `target=3 affected=3 remaining=0 COMMIT`。
- PASS: 将同产品 V21 已发布 QA 规程复制到当前 ACTIVE V27 发布快照身份：14 条规程、14 条版本、78 条项目、32 条设备绑定全部提交。
- PASS: V27 QA 规程对齐发布快照 `routeProcessId=980645..980658`，并修正 FINAL/FIRST 固定数量，使候选校验通过。

## Frontend Evidence

- GREEN: `node doc/tasks/20260808-pressure-pump-active-orders/add-pressure-pump-active-orders.e2e.cjs` -> PASS。
- Added: `PQC-E2E-FS-20260804` / workOrderId `980019` -> activeOrderId `48`。
- Added: `881MO090889` / workOrderId `923889` -> activeOrderId `49`。
- Active after: 7 rows, including route `球囊扩张压力泵` V27 and route `按压式球囊扩充压力泵` V1.

## Database Evidence

- PASS: activeOrderId `48` route `922119` / V27, ERP quantity `100`, status `ACTIVE`。
- PASS: activeOrderId `49` route `922119` / V27, ERP quantity `2248`, status `ACTIVE`。
- PASS: each new active order has 14 process snapshots.
- PASS: each new active order has 56 PQC tasks: 14 FIRST, 28 PATROL, 14 FINAL; business date `2026-08-08`。

## Residual Notes

- Existing candidate `RRM-20260801-PP-MO-001` remains ineligible because its historical schedule process rows lack `plan_date`; this was not changed because it would require inferring process dates.
- Several other product-code variants still lack published QA regulations or formal product route bindings; they were not modified because the requested active orders were satisfied with valid `902149` product orders.
