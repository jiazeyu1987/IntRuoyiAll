# Execution Log

## User Intent

排产员要有可以操作工艺流程下的列表权限。截图显示工艺流程列表“操作”列为空，预期排产员能看到并使用该列表下允许的操作。

## Milestone Log

- Created task directory and initial task documentation.
- Baseline: pre-existing dirty worktree committed as `de9da136 chore: baseline pre-existing dirty worktree`.
- Root cause: scheduler role scope SQL granted route page/query (`5720/5721`) and old schedule-route config permissions, but did not grant route-flow list operation permissions used by the current frontend action buttons (`5723` update and `5730` version query).
- Implementation: added RED static contracts, updated canonical scheduler role scope SQL, and added idempotent `20260728_mes_scheduler_route_flow_list_permission.sql` migration for already-applied environments.
- Scope boundary: granted non-delete route-flow list operation permissions only; delete/export/create permissions were not added.
- Concurrent state: `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordRouteGenerationService.java` became dirty after baseline and is not part of this task.

## BDD / TDD Evidence

- BDD: Scheduler can operate route-flow list -> Given a user with the scheduler role, When the user opens the MES route-flow list, Then the role must include the formal list-operation permissions needed for row operations instead of showing an empty operation column.
- RED: `python -X utf8 -m pytest script/tests/test_mes_smart_scheduling_role_scope_sql.py script/tests/test_mes_scheduler_route_flow_list_permission_sql.py` -> FAIL, expected because scheduler block lacked `5723/5730` and `20260728_mes_scheduler_route_flow_list_permission.sql` did not exist.
- GREEN: `python -X utf8 -m pytest script/tests/test_mes_smart_scheduling_role_scope_sql.py script/tests/test_mes_scheduler_route_flow_list_permission_sql.py` -> PASS, `29 passed in 2.33s`.
- REGRESSION: `python -X utf8 -m pytest script/tests/test_mes_route_version_permission_menu_sql.py script/tests/test_mes_route_flow_config_migration_sql.py` -> PASS, `11 passed in 0.35s`.
- GREEN: experience-preflight -> PASS, `docs/experience-index.md` read and task gate summary copied into `task.md`.
- Experience consolidation: existing `docs/database-rules.md#租户和菜单权限` already covers the reusable lesson to verify `system_menu` + role menu binding + tenant package; no new long-term document needed.

## Verification Evidence

- Targeted migration policy gate: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file ... --output E:\IntRuoyi\doc\tasks\20260728-scheduler-route-flow-list-permission\migration-policy-gate-targeted.json` -> PASS, `migrationCount=10`.
- Full migration policy gate: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --output E:\IntRuoyi\doc\tasks\20260728-scheduler-route-flow-list-permission\migration-policy-gate.json` -> FAIL due pre-existing unrelated metadata `20260725_mes_edhr_recordbook_global_setting.sql: config-seed`.
- Evidence validators: `validate_bug_regression.py --evidence ...\bug-regression-evidence.md` -> PASS; `validate_database_schema.py --evidence ...\database-schema-evidence.md` -> PASS.

## Blockers

- No current task blocker.
- Unrelated full SQL gate blocker remains in existing migration metadata: `20260725_mes_edhr_recordbook_global_setting.sql` uses invalid type `config-seed`.
