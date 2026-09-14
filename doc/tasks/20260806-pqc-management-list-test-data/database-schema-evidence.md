# Database Schema Evidence

## Data Change Goal

- 在本机 tenant `1` 的正式 MES 工序池/PQC 读模型中添加一条 `芋道源码/admin` 可见的 PQC 管理列表测试数据。
- 测试 marker：`PQC_TEST_20260806_MGMT_LIST_20260806181357559250`。

## Affected Entities

- `mes_pqc_inspection_task`：新增 PQC 任务 `189`，巡检 `PATROL`，检验数量 `30`。
- `mes_pro_process_pool_event`：新增 PQC 提交事件 `160`，`actual_employee_id=1`，`template_type=PQC_SIMPLIFIED`。
- `mes_pro_process_pool_pqc_record`：新增 PQC 记录 `103`，结果 `FAILURE`。
- `mes_pqc_inspection_piece_detail`：新增逐件明细 `90` 条，3 个检验项，每项 30 个样本。

## Database Engine And Tooling

- Engine: local Docker MySQL `8.0.39`, database `ruoyi-vue-pro`, character set connection `utf8mb4` / collation `utf8mb4_0900_ai_ci`.
- Execution path: copied task-owned SQL files into container and executed with `mysql --default-character-set=utf8mb4`.

## Migration

- No schema migration was introduced.
- This task performed a task-owned data fixture insert into existing verified tables only.

## Schema Evidence

- Verified target columns through `information_schema.COLUMNS` for:
  - `mes_pro_process_pool_event`
  - `mes_pro_process_pool_pqc_record`
  - `mes_pqc_inspection_task`
  - `mes_pqc_inspection_piece_detail`
  - `mes_pro_process_pool`
  - `mes_pro_process_pool_team_leader_scope`
- Verified `mes_pro_process_pool_event.pqc_task_id` is a stored generated column extracted from `raw_payload.$.pqcTaskId`.
- Verified list read model uses `mes_pro_process_pool_event`, joins `mes_pqc_inspection_task` by generated `pqc_task_id`, joins `mes_pro_process_pool_pqc_record` by `event_id`, and filters responsible employees through `actual_employee_id`.

## BDD Scenarios

- BDD: PQC 管理列表显示测试提交 -> Given 本机 `芋道源码/admin` 打开 PQC 管理列表 / When 今天存在一条 admin 负责范围内的 PQC 测试提交 / Then 列表能显示生产工单、工序、PQC 检验员、检验项、检验数量、损耗数量和逐件样本值。
- BDD: 测试数据可追踪可清理 -> Given 测试提交写入正式库 / When 后续需要清理 / Then 可通过任务标识定位并删除本次事件和关联 PQC 记录，不影响其它业务数据。

## RED Evidence

- RED: SQL read before insert for marker `PQC_TEST_20260806_MGMT_LIST_%` -> `existing_marker_count=0`, `admin_visible_marker_count=0`.
- RED: API/page verification before backend restart -> `http://127.0.0.1:48081` connection refused because backend was not listening.

## GREEN Evidence

- GREEN: `insert-pqc-test-data.sql` -> inserted task `189`, event `160`, PQC record `103`, piece detail count `90`, source production submit event `158`.
- GREEN: `fix-pqc-test-payload-json.sql` -> corrected payload arrays to real JSON arrays; `pqcItemDetails` type `ARRAY`, count `3`; pressure sample count `30`; pressure sample #12 `53.00`.
- GREEN: SQL admin-visible list口径 -> `admin_visible_count=1` for event `160`, work order `RRM-20260801-PP-MO-001`, process `清洗工序`, inspection result `FAILURE`.
- GREEN: quantity integrity -> `scrapQuantity=1`, `lossReasonDetails.quantity sum=1`, reason `外观不合格`.
- GREEN: abnormal parameter evidence -> pressure sample #12 `53.00`, upper limit `52.0`, so frontend out-of-range logic has the data needed to render the value red while still allowing submission.

## Runtime API Verification

- Backend health check passed: `http://127.0.0.1:48081/actuator/health` returned `{"status":"UP"}` with listener PID `2548`.
- Authenticated local admin API verification passed for `GET /admin-api/mes/pro/process-pool/team-leader/submission/page?leaderType=PQC&submitDate=2026-08-06&workOrderCode=RRM-20260801-PP-MO-001&templateType=PQC_SIMPLIFIED`.
- API result summary: tenant `1`, login code `0`, list code `0`, total `1`, returned rows `1`, found event `160`, work order `RRM-20260801-PP-MO-001`, process `清洗工序`, template `PQC_SIMPLIFIED`, PQC task `189`, result `FAILURE`.
- Runtime payload evidence: marker present in `originalPayloadJson`, `pqcItemDetails` count `3`, pressure sample #12 `53.00`, pressure item `standardUpperLimit=52.0`, loss reason quantity sum `1.0`.
- Browser page screenshot verification was not run in this turn; SQL and authenticated API evidence prove the record is available to the admin/PQC list read model.

## Data Safety And Rollback

- Scope is tenant `1`, task-owned marker only.
- No existing production rows were updated except raw JSON payload correction for the newly inserted event `160` and PQC record `103`.
- Cleanup should delete child rows first:

```sql
START TRANSACTION;
DELETE FROM mes_pqc_inspection_piece_detail WHERE tenant_id = 1 AND task_id = 189;
DELETE FROM mes_pro_process_pool_pqc_record WHERE tenant_id = 1 AND id = 103 AND event_id = 160;
DELETE FROM mes_pro_process_pool_event WHERE tenant_id = 1 AND id = 160 AND event_idempotency_key = 'PQC_TEST_20260806_MGMT_LIST_20260806181357559250';
DELETE FROM mes_pqc_inspection_task WHERE tenant_id = 1 AND id = 189;
COMMIT;
```

## Blockers

- No current blocker for the inserted data or admin/PQC list API visibility.
