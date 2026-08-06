# Execution Log

## User Intent

用户要求按照已确认计划实施：在本机 `int_main` 中给 `芋道源码/admin` 绑定 `球囊扩张压力泵` 和 `按压式球囊扩充压力泵` 两条产线的工序开始生产组长。

## BDD

- BDD: admin can add pressure pump route processes -> Given 本机 tenant `1` 的 `admin` 登录生产组长工作台, When 点击新增工序候选, Then 候选应包含 route `922119 / RT000028 / 球囊扩张压力泵` 与 route `980091 / RT000028-IDI / 按压式球囊扩充压力泵` 的 active 路线工序。
- BDD: binding is scoped to active tenant 1 routes only -> Given 数据修复只允许本机 tenant `1`, When 写入工序开始生产组长快照, Then 原事务只能更新执行时 active version `448` 与 `622`，后续复验必须以当前 active 版本为准且不得修改 tenant `122` 或当前 draft version。
- BDD: missing route start leader snapshot fails fast -> Given active route snapshot 缺少 `routeStartProductionLeaders`, When 执行写入前 RED 校验, Then 校验必须失败并指出缺失快照，禁止用角色权限或空列表成功替代。

## Evidence

- Skill: `database-schema-delivery` loaded.
- Trigger docs read: `docs/database-rules.md`, `docs/powershell-encoding.md`, `docs/task-closeout-rules.md`, `docs/backend-development.md#生产组长工序配置维护权限不得被工序开始快照误拦`.
- Schema evidence: `mes_pro_route_version.route_snapshot_json` is `mediumtext`, collation `utf8mb4_unicode_ci`; target writes use container mysql `--default-character-set=utf8mb4` and ASCII-safe `UNHEX` for Chinese JSON values.
- Git baseline note: existing workspace already contains many unrelated dirty changes from other tasks. Current task does not modify those files and only owns `doc/tasks/20260806-admin-pressure-pump-route-start-leader/` plus the local DB rows originally repaired by this task.

## RED

- RED: `docker exec int-ruoyi-mysql ... mysql ... < db-repair/red-missing-route-start-leaders.sql` -> FAIL, expected reason: route versions `448` and `622` are missing `routeStartProductionLeaders`.

## GREEN

- GREEN: `docker exec int-ruoyi-mysql ... mysql ... < db-repair/apply-route-start-leaders.sql` -> PASS, `APPLIED / updated_rows=2 / verified_rows=2`.
- GREEN: `docker exec int-ruoyi-mysql ... mysql ... < db-repair/verify-route-start-leaders.sql` -> PASS, versions `448` and `622` each contain one `USERS` item for user `1`; tenant `122` route and draft `490` changed count is `0`.
- GREEN: `node api-verify/verify-switchable-processes.cjs` -> PASS, `芋道源码/admin` returned `28` process-config rows and route IDs `[922119, 980091]`.
- API scope note: `/mes/pro/feedback/frontline/device-account/processes` was initially checked but is not the production group leader add-dialog source and correctly enforced a separate workstation binding requirement. The actual UI data source `/mes/pro/process-pool/team-leader/process-config/list` passed.

## 2026-08-07 Active-Version Recheck

- RECHECK: read-only SQL on `mes_pro_route_version` showed route `922119` current active version is now `490 / ACTIVE`; original repaired version `448` is now `SUPERSEDED`; route `980091` remains active version `622`.
- GREEN: updated `db-repair/verify-route-start-leaders.sql` to verify current active target route versions dynamically instead of treating `490` as a non-target draft after it became active.
- GREEN: `Get-Content -Encoding utf8 -Raw db-repair/verify-route-start-leaders.sql | docker exec -i int-ruoyi-mysql ... mysql ...` -> PASS, current active versions `490 / route 922119` and `622 / route 980091` both contain `candidateSourceType=USERS`, `candidateSourceIds=[1]`, `candidateSourceNames=["瑛泰管理员（admin）"]`, and `productionLineId` equal to each route ID; tenant `122` route `922273` remains without leader snapshot.
- GREEN: inline Node login/API verification using local `.env` credentials without printing password/token -> PASS, `/admin-api/mes/pro/process-pool/team-leader/process-config/list` returned business code `0`, `28` rows, and target route IDs `[922119, 980091]`.

## Rollback

- Full row backup: `db-backup/route-version-448-622-before.sql`.
- Rollback command, if required: apply the backup file back to local Docker MySQL only after confirming no later route publish or task legitimately changed route versions `448`, `490`, or `622`.

## Closeout

- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260806-admin-pressure-pump-route-start-leader\database-schema-evidence.md` -> PASS, `Database schema evidence is valid.`
- EXPERIENCE: Updated `docs/backend-development.md#生产组长工序配置维护权限不得被工序开始快照误拦` to record that add-dialog verification must use `/mes/pro/process-pool/team-leader/process-config/list`, not the frontline device-account endpoint, and that SQL data-repair rechecks must resolve the current active route version after route publication.
- CLEANUP PREVIEW: `task_closeout.py --task-id 20260806-admin-pressure-pump-route-start-leader --mode preview` -> PASS after retaining rollback backup and SQL evidence.
- CLEANUP APPLY: `task_closeout.py --task-id 20260806-admin-pressure-pump-route-start-leader --mode apply` -> PASS; deleted only `api-verify/verify-switchable-processes.cjs` and `database-schema-evidence.md` after copying key conclusions into `verification-report.md`.
- STATUS: task marked `completed`.
