# Verification Report

## Summary

- Test data has been inserted into the formal local MES/PQC read model for `芋道源码/admin`.
- SQL verification confirms the row is visible under the admin/PQC today-list scope.
- Runtime authenticated API verification confirms the PQC 管理列表 can return the inserted row.
- Browser verification confirms `PQC组长 > PQC管理` now loads with visible `提交日期=2026-08-06` and displays the inserted row.
- Static UI verification confirms `逐件/样本值` is no longer a PQC 管理 list column, while detail still renders sample values and the detail drawer is widened from `620px` to `1240px`.

## Commands

- `docker exec int-ruoyi-mysql ... information_schema.COLUMNS ...` -> schema verified.
- `docker exec int-ruoyi-mysql ... insert-pqc-test-data.sql` -> inserted event/task/record/detail rows.
- `docker exec int-ruoyi-mysql ... fix-pqc-test-payload-json.sql` -> corrected JSON array payload.
- `docker exec int-ruoyi-mysql ... SELECT ... event_idempotency_key='PQC_TEST_20260806_MGMT_LIST_20260806181357559250'` -> SQL verification passed.
- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> `{"status":"UP"}`.
- Authenticated `GET /admin-api/mes/pro/process-pool/team-leader/submission/page?leaderType=PQC&submitDate=2026-08-06&workOrderCode=RRM-20260801-PP-MO-001&templateType=PQC_SIMPLIFIED` -> code `0`, total `1`, event `160`.
- `node tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs` -> PASS.
- `node tests/e2e/pqc-leader-sample-values-detail-only-static.spec.cjs` -> PASS.
- `node tests/e2e/pqc-leader-item-snapshot-static.spec.js` -> PASS.
- `node tests/e2e/mes-process-pool-team-leader-static.spec.js` -> PASS.
- `node doc/tasks/20260806-pqc-management-list-test-data/verify-pqc-management-list-real.e2e.cjs` -> PASS.
- `pnpm ts:check` from `IntRuoyiFronted` -> PASS.
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/pqc-leader-sample-values-detail-only-static.spec.cjs doc/tasks/20260806-pqc-management-list-test-data` -> PASS.

## Data Created

- Marker: `PQC_TEST_20260806_MGMT_LIST_20260806181357559250`.
- `mes_pqc_inspection_task.id=189`.
- `mes_pro_process_pool_event.id=160`.
- `mes_pro_process_pool_pqc_record.id=103`.
- `mes_pqc_inspection_piece_detail`: 90 rows for task `189`.
- Context: work order `RRM-20260801-PP-MO-001`, process `清洗工序`, inspection quantity `30`, loss/scrap quantity `1`, loss reason quantity sum `1`.
- Structured values: 3 PQC items, each with 30 samples; pressure sample #12 `53.00` exceeds upper limit `52.0`.

## API Verification

- Backend health: `UP` on `48081`, listener PID `2548`.
- Admin/PQC list API: tenant `1`, login code `0`, list code `0`, total `1`, returned rows `1`.
- Returned row: event `160`, work order `RRM-20260801-PP-MO-001`, process `清洗工序`, template `PQC_SIMPLIFIED`, PQC task `189`, PQC result `FAILURE`, loss quantity `1.0`.
- Runtime payload evidence: marker present in `originalPayloadJson`, `pqcItemDetails` count `3`, pressure sample #12 `53.00`, `standardUpperLimit=52.0`, loss reason quantity sum `1.0`.
- Evidence validator: `Database schema evidence is valid.`
- Evidence validator: `Frontend feature evidence is valid.`
- Cleanup preview: ready; keep task records plus SQL scripts, delete only temporary evidence/runtime-inspect artifacts, blocked/warnings none.

## Browser Verification

- Route: `http://127.0.0.1:8081/mes/pro/process-pool/pqc-leader`.
- Identity label: local `芋道源码/admin`; credentials were read at runtime from local `.env` and were not written to artifacts.
- Action: clicked `PQC管理`.
- Captured request: `/admin-api/mes/pro/process-pool/team-leader/submission/page?pageNo=1&pageSize=10&leaderType=PQC&submitDate=2026-08-06`.
- Captured response: HTTP `200`, business `code=0`, total `1`, work order `RRM-20260801-PP-MO-001`.
- Page assertion: visible row contains `RRM-20260801-PP-MO-001`; captured API row confirms `processName=清洗工序`.
- Detail assertion: clicked detail for event `160`; drawer width measured `1240px`; left label column measured `400px`; detail sample values include seeded `53.00`; `结构化报工内容` and `原始提交内容` are not visible.
- Screenshot artifact: `doc/tasks/20260806-pqc-management-list-test-data/pqc-management-list-real.png`.
- Screenshot artifact: `doc/tasks/20260806-pqc-management-list-test-data/pqc-management-detail-real.png`.

## List / Detail Display Verification

- List behavior: `pieceSampleValues` / `逐件/样本值` removed from the PQC management table render path and from `pqcSubmissionDefaultColumns`.
- Detail behavior: PQC item snapshot table still renders `样本值` through `formatPqcSnapshotSampleValues(row)` with marker `data-pqc-leader-detail-sample-values`.
- Drawer behavior: submission detail drawer now uses `size="1240px"` with marker `data-team-leader-submission-detail-drawer`, doubling the prior `620px` width.
- Detail cleanup behavior: `结构化报工内容` and `原始提交内容` are removed from the drawer; description labels use `team-leader-workbench__detail-descriptions` and enforce `400px` width.
- Static regression: `pqc-leader-sample-values-detail-only-static.spec.cjs` passed and locks list-hidden/detail-visible behavior.
- Real regression: `verify-pqc-management-list-real.e2e.cjs` passed and measured the actual drawer width as `1240px` and label width as `400px`.

## Cleanup

- Keep data unless the user asks to remove it.
- Cleanup SQL:

```sql
START TRANSACTION;
DELETE FROM mes_pqc_inspection_piece_detail WHERE tenant_id = 1 AND task_id = 189;
DELETE FROM mes_pro_process_pool_pqc_record WHERE tenant_id = 1 AND id = 103 AND event_id = 160;
DELETE FROM mes_pro_process_pool_event WHERE tenant_id = 1 AND id = 160 AND event_idempotency_key = 'PQC_TEST_20260806_MGMT_LIST_20260806181357559250';
DELETE FROM mes_pqc_inspection_task WHERE tenant_id = 1 AND id = 189;
COMMIT;
```

## Residual Scope

- Adjacent list-structure contracts still need a separate cleanup pass: one production-report column contract fails on pre-existing `workOrder` default column, and one older PQC standard-list contract still expects an empty default condition instead of the backend-required visible `submitDate`.
- No commit, push, or task-closeout-cleanup apply was performed to avoid mixing this task with unrelated current workspace changes.
