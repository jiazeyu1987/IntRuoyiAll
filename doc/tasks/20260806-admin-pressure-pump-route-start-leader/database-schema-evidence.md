# Database Schema Evidence

## Data Change Goal

Bind tenant `1` user `1/admin` as `routeStartProductionLeaders` for active route versions:

- `448 / route 922119 / RT000028 / 球囊扩张压力泵`
- `622 / route 980091 / RT000028-IDI / 按压式球囊扩充压力泵`

## Engine And Scope

- Engine: local Docker MySQL container `int-ruoyi-mysql`, database `ruoyi-vue-pro`.
- Scope: local `int_main` only; no remote server, no tenant `122`, no draft or historical route versions.

## Data Safety

- Precondition SQL checks exact user, tenant, route, active version, JSON validity, and missing current leader snapshot.
- Update SQL runs in one transaction and uses `SIGNAL SQLSTATE '45000'` for all failed preconditions.
- Chinese values are written using `CONVERT(UNHEX(... ) USING utf8mb4)` to avoid PowerShell/client encoding ambiguity.
- A full-row `mysqldump --replace` backup is stored at `db-backup/route-version-448-622-before.sql`.

## BDD / RED / GREEN

- BDD scenarios are recorded in `execution-log.md`.
- RED command is `db-repair/red-missing-route-start-leaders.sql`.
- GREEN command is `db-repair/verify-route-start-leaders.sql`.

## Rollback Plan

If rollback is needed, restore the backed-up rows for versions `448` and `622` from `db-backup/route-version-448-622-before.sql`, then rerun the verification query confirming `routeStartProductionLeaders` is back to the prior state.

## Blockers

None currently.
