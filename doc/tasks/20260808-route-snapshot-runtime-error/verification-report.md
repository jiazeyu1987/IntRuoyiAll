# Verification Report

## Summary

- Route snapshot runtime error is fixed in the running local backend: the PQC active-order process API returns `code=0` and no longer includes `routeVersion.routeSnapshotJson.routeProcessId`.
- Active order process mapping now uses frozen active-order process snapshots, so `routeProcessId=980645 / processId=922985` is returned under `activeOrderId=48`.
- QA/PQC task mapping exists for the user-highlighted cleaning process: “清洗工序” returns `pqcTaskId=240` with 4 PQC task options.
- Optional-equipment data repair is prepared but not applied to the local database because there is no explicit database write authorization.

## Commands

- `java "@E:\IntRuoyi\IntRuoyiBackend\yudao-module-mes\target-pqc-route-snapshot\junit-console.args"` -> PASS, 36 tests successful.
- `python -X utf8 -m pytest E:\IntRuoyi\IntRuoyiBackend\script\tests\test_mes_qa_optional_equipment_items_sql.py` -> RED before migration file, then GREEN after adding migration, 3 passed.
- `python -X utf8 E:\IntRuoyi\IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root E:\IntRuoyi\IntRuoyiBackend\sql\mysql --output E:\IntRuoyi\doc\tasks\20260808-route-snapshot-runtime-error\migration-policy-gate.json` -> PASS.
- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> `UP`.
- Authenticated API: `/admin-api/mes/pro/feedback/frontline/device-account/pqc/active-order/processes?workOrderId=980019&routeId=922119` -> `code=0`, `activeOrderIds=[48]`, `processCount=14`, target `routeProcessId=980645`, `processId=922985`, `pqcTaskId=232`, `pqcTaskOptionCount=4`, old error absent.

## Runtime Evidence

- Running Jar: `E:\IntRuoyi\output\runtime\int_main\backend-latest-20260808-1524-pqc-snapshot-process-hotfix.jar`
- PID: `66736`
- SHA256: `2C8BB890FE22A6020F89F86A7BA5BD4C663C3E0239F6CE060A51BDAFD20CD20F`
- Class compatibility: JDK17 major version 61.

## Data Evidence

- Read-only DB check for activeOrderId=48 found 110 mapped QA/PQC item rows across 14 process snapshots.
- Current data has 64 items with equipment bindings and 46 items without equipment bindings but still marked `equipment_required=1`.
- Read-only projection of `20260808_mes_qa_optional_equipment_items.sql` shows it would update 46 rows to optional equipment and keep 64 rows required.

## Remaining Blocker

- The database migration `IntRuoyiBackend/sql/mysql/20260808_mes_qa_optional_equipment_items.sql` has not been applied to the local database. Applying it requires explicit authorization because it changes real local QA regulation data.
