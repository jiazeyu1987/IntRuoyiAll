# Verification Report

## Result

PASS. Tenant `1` user `1/admin` is bound as route-start production leader for both target pressure-pump active route versions.

## Database Verification

- Version `448 / route 922119 / RT000028 / 球囊扩张压力泵` now contains:
  - `candidateSourceType=USERS`
  - `candidateSourceIds=[1]`
  - `candidateSourceNames=["瑛泰管理员（admin）"]`
  - `productionLineId=922119`
- Version `622 / route 980091 / RT000028-IDI / 按压式球囊扩充压力泵` contains the same admin source with `productionLineId=980091`.
- Apply transaction result: `updated_rows=2`, `verified_rows=2`.
- GREEN verification result: `routeStartProductionLeaders` valid for both target versions.
- Non-target verification: tenant `122` route `922273` and draft version `490` leader-snapshot count remains `0`.

## API Verification

- Identity: local `芋道源码/admin`, tenant `1`, user ID `1`.
- Endpoint: `/admin-api/mes/pro/process-pool/team-leader/process-config/list`.
- Result: business code `0`, `28` rows returned.
- Route IDs returned: `922119`, `980091`.
- The previous “当前账号没有可新增的路线工序” condition is no longer reproduced by the add-dialog data source.

## Safety And Recovery

- No frontend, backend, permission, role, remote environment, tenant `122`, draft, or historical route data was changed.
- Backup: `db-backup/route-version-448-622-before.sql`.
- Rollback must first confirm no later task legitimately changed versions `448` or `622`, then restore the backed-up rows.
