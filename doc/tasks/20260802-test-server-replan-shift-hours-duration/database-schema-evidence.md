# Database Schema Evidence

## Data

- Goal: 将测试服芋道源码租户 `tenant_id=1` 中 `ROUTE-XLSX-00002` 的 26 条未绑定路线工序绑定到按相同 `process_id` 唯一匹配的有效工作站。
- Affected entities: `mes_pro_route`、`mes_pro_route_process`、`mes_md_workstation`，以及本轮备份表 `zz_codex_backup_route_process_bind_20260802_211009`。
- Database engine: MySQL 8.0.39 in remote Docker container `intruoyi-mysql` on test server `172.30.30.58`.

## Migration

- Schema changes: none to production schema.
- Data changes: update `mes_pro_route_process.workstation_id` for exactly 26 target rows; set `updater='codex'` and `update_time=NOW()`.
- Migration tool: direct audited SQL through remote Docker MySQL client using container `MYSQL_ROOT_PASSWORD` environment variable; no plaintext password was written to task evidence.

## Safety

- Precheck confirmed one target route, exactly 26 target route processes, all 26 unbound before update, and exactly 26 unique active workstation matches.
- Precheck confirmed matched workstation `shift_hours` values are positive and currently all `15.00`.
- SQL procedure failed fast if route count, target row count, unique match count, backup row count, updated row count, or post-update null binding count deviated from expected values.

## Rollback

- Rollback source: `zz_codex_backup_route_process_bind_20260802_211009`, containing 26 pre-update route process rows.
- Rollback method: restore `mes_pro_route_process.workstation_id`, `updater`, and `update_time` for target IDs from the backup table if user explicitly requests rollback.
- No destructive cleanup was performed.

## BDD

- BDD: bind current route processes to unique workstations -> Given 测试服芋道源码租户 `ROUTE-XLSX-00002` 当前 26 道路线工序均未绑定工作站, When 按相同 `process_id` 匹配唯一有效工作站并绑定, Then 26 道路线工序应全部拥有有效工作站且候选工作站班时均为当前配置 `15.00`。

## Verification

- RED: 只读 SQL 复核测试服 `ROUTE-XLSX-00002` -> FAIL, `process_count=26`、`null_ws_count=26`、`distinct_ws_count=0`，确认当前未绑定。
- GREEN: 绑定事务 -> PASS, `BIND_OK updated_rows=26 backup_rows=26 unique_matches=26`。
- Verification: 绑定后复核 -> PASS, `process_count=26`、`null_ws_count=0`、`distinct_ws_count=26`、`min_shift_hours=15.00`、`max_shift_hours=15.00`、`invalid_binding=0`。

## Blockers

- 本轮只执行用户要求的绑定，未执行重排。
- 测试服仍运行旧后端镜像；未发布本机代码修复。
