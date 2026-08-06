# Verification Report

## Result

PASS. Tenant `1` user `1/admin` is bound as route-start production leader for both current target pressure-pump active route versions.

## Database Verification

- Initial apply transaction result: `updated_rows=2`, `verified_rows=2` for the then-active versions `448 / route 922119` and `622 / route 980091`.
- Current active state on 2026-08-07: route `922119` active version is now `490 / ACTIVE`; original repaired version `448` is `SUPERSEDED`; route `980091` remains active version `622`.
- Current GREEN verification result: active versions `490 / route 922119 / RT000028 / 球囊扩张压力泵` and `622 / route 980091 / RT000028-IDI / 按压式球囊扩充压力泵` both contain `candidateSourceType=USERS`, `candidateSourceIds=[1]`, `candidateSourceNames=["瑛泰管理员（admin）"]`, and `productionLineId` equal to the route ID.
- Non-target verification: tenant `122` route `922273` remains without `routeStartProductionLeaders`; current target DRAFT versions remain without leader snapshots.

## API Verification

- Identity: local `芋道源码/admin`, tenant `1`, user ID `1`.
- Endpoint: `/admin-api/mes/pro/process-pool/team-leader/process-config/list`.
- Recheck result: business code `0`, `28` rows returned.
- Route IDs returned: `922119`, `980091`.
- The previous “当前账号没有可新增的路线工序” condition is no longer reproduced by the add-dialog data source.

## Safety And Recovery

- No frontend, backend, permission, role, remote environment, or tenant `122` data was changed. Later route publication changed the active version for route `922119`; no rollback was applied because the current active version contains the intended admin leader snapshot.
- Backup: `db-backup/route-version-448-622-before.sql`.
- Rollback must first confirm no later route publication or task legitimately changed versions `448`, `490`, or `622`, then restore only the approved rows.

## Closeout Verification

- Database evidence validator: PASS.
- Cleanup apply: PASS; retained rollback backup and SQL evidence, removed temporary API script and temporary database evidence file.
- 2026-08-07 recheck: updated retained GREEN SQL to verify current active target versions and reran SQL/API verification successfully.
- Project experience: updated existing backend-development gate with the process-config add-dialog verification endpoint boundary.
