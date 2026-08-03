# Database Schema Evidence

## Data

- Data change goal: store route start production leader configuration as part of route candidate snapshot data.
- Affected entity: `route_snapshot_json.configSnapshots.routeStartProductionLeaders` in existing route version snapshot payload.

## Migration

- Migration: no physical table, column, index, seed, tenant binding, or menu permission migration added in this task.
- `20260803_mes_frontline_pressure_pump_all_process_permission.sql` is not used as the switching authorization source after the user changed the requirement; batch execution tab visibility remains existing menu permission behavior.

## Safety

- Snapshot-only design avoids destructive schema changes and keeps draft/candidate route version semantics aligned with existing route config snapshots.
- Save validation rejects unknown production lines, empty leader configuration, invalid source type, and non-existing users/roles.

## Rollback

- Rollback: remove or edit `routeStartProductionLeaders` entries from the candidate route snapshot before publication; no schema rollback required.

## BDD:

- BDD: 生产组长配置随路线候选版本保存 -> Given 路线候选版本存在 When 保存生产组长配置 Then 配置写入候选路线快照。
- BDD: 不新增菜单权限授权 -> Given 批次执行菜单权限存在 When 配置生产组长 Then 切换授权不依赖额外压力泵全工序菜单权限 SQL。

## RED:

- RED: backend/front-end static tests failed before `routeStartProductionLeaders` snapshot/API/UI contract existed.

## GREEN:

- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineDeviceAccountContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.

## Verification

- Verified no new SQL file was required for this task-owned implementation.
- Verified backend service stores through existing route candidate snapshot mechanism and parses active route snapshot at runtime.

## Blockers

- No database-schema blocker. Target database execution of prior pressure-pump all-process menu SQL is not part of this changed requirement and was not executed in this task.
