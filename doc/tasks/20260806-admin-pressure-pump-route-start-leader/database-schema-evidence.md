# Database Schema Evidence

## Data Change Goal

Bind tenant `1` user `1/admin` as `routeStartProductionLeaders` for active route versions:

- `448 / route 922119 / RT000028 / 球囊扩张压力泵`
- `622 / route 980091 / RT000028-IDI / 按压式球囊扩充压力泵`

## Engine And Scope

- Engine: local Docker MySQL container `int-ruoyi-mysql`, database `ruoyi-vue-pro`.
- Scope: local `int_main` only; no remote server, no tenant `122`, no draft or historical route versions.

## Migration

- This is a scoped local data repair, not a schema migration.
- Apply artifact: `db-repair/apply-route-start-leaders.sql`.
- Rollback artifact: `db-backup/route-version-448-622-before.sql`.

## Data Safety

- Precondition SQL checks exact user, tenant, route, active version, JSON validity, and missing current leader snapshot.
- Update SQL runs in one transaction and uses `SIGNAL SQLSTATE '45000'` for all failed preconditions.
- Chinese values are written using `CONVERT(UNHEX(... ) USING utf8mb4)` to avoid PowerShell/client encoding ambiguity.
- A full-row `mysqldump --replace` backup is stored at `db-backup/route-version-448-622-before.sql`.

## BDD

- BDD: admin can add pressure pump route processes -> Given tenant `1` admin user `1`, When the production leader process-config list is loaded, Then both target active routes are returned.
- BDD: data repair stays inside target versions -> Given exact target versions `448` and `622`, When the repair runs, Then tenant `122`, draft `490`, and historical versions remain unchanged.

## RED

- RED: `db-repair/red-missing-route-start-leaders.sql` -> FAIL with `RED expected missing routeStartProductionLeaders count=2`.

## GREEN

- GREEN: `db-repair/apply-route-start-leaders.sql` -> PASS, `updated_rows=2`, `verified_rows=2`.
- GREEN: `db-repair/verify-route-start-leaders.sql` -> PASS for versions `448` and `622`, with non-target changed count `0`.
- GREEN: `api-verify/verify-switchable-processes.cjs` -> PASS, tenant `1` user `1/admin` receives `28` process-config rows across routes `922119` and `980091`.

## Verification

- JSON snapshot verification confirms `candidateSourceType=USERS`, `candidateSourceIds=[1]`, and snapshot name `瑛泰管理员（admin）`.
- API verification uses the actual production leader add-dialog data source `/mes/pro/process-pool/team-leader/process-config/list`.
- The unrelated frontline device-account endpoint was not used as completion evidence because it has an additional formal workstation-binding contract.

## Rollback Plan

If rollback is needed, restore the backed-up rows for versions `448` and `622` from `db-backup/route-version-448-622-before.sql`, then rerun the verification query confirming `routeStartProductionLeaders` is back to the prior state.

## Blockers

None currently.
