# Database Schema Evidence

## Data Change Goal

- Data: add a formal eDHR dynamic menu row `900434 / QA` so QA settings are visible in the left menu for local `芋道源码/admin`.
- Affected entities: `system_menu`, `system_tenant_package.menu_ids`, and `system_role_menu`.
- Non-goal: no schema table changes, no DCC menu/data changes, no controlled-file or document-control permissions.

## Migration

- Migration file: `IntRuoyiBackend/sql/mysql/20260804_mes_edhr_qa_menu.sql`.
- Database engine: MySQL 8 compatible SQL using `JSON_VALID`, `JSON_TABLE`, and `JSON_ARRAYAGG`.
- Migration tool path: project release migration policy gate plus direct local Docker MySQL apply for E2E data setup.
- Menu contract: `QA` uses id `900434`, permission `mes:pro-process-pool-team-leader:query`, path `/mes/pro/process-pool/qa-regulation`, component `mes/pro/processpool/QaRegulationPage`, and sort `1` under parent `900220`.

## Safety

- The migration is fail-fast: invalid package JSON, missing eDHR parent, missing retained menu rows, occupied menu id, or duplicate QA route abort with `SIGNAL SQLSTATE '45000'`.
- The migration updates only the five intended visible eDHR children and inserts or revives only the QA menu role binding.
- It does not delete, truncate, drop, or silently hide failures.
- Local apply was run only against Docker container `int-ruoyi-mysql` database `ruoyi-vue-pro`; no remote server was accessed.

## Rollback

- Rollback for local verification data is to rerun the prior menu ordering migration `20260715_mes_edhr_template_config_menu_removal.sql` or explicitly mark menu `900434` hidden/deleted in a task-approved rollback SQL.
- No destructive rollback was executed in this task because the user requested the QA menu to remain visible.

## BDD Scenario

- BDD: eDHR 左侧菜单显示 QA -> Given `芋道源码/admin` opens the eDHR menu, When `批记录表单` and `批次执行` are visible, Then `QA` appears between them and opens `/mes/pro/process-pool/qa-regulation`.

## RED Evidence

- RED: `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_mes_edhr_qa_menu_sql.py -q` -> FAIL, expected reason: missing `20260804_mes_edhr_qa_menu.sql`.

## GREEN Evidence

- GREEN: `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_mes_edhr_qa_menu_sql.py -q` -> PASS, `3 passed`.
- GREEN: `node IntRuoyiFronted/tests/e2e/mes-edhr-qa-menu-static.spec.js` -> PASS.
- GREEN: release migration policy gate on the 18-file dependency closure -> PASS, `migrationId=20260804_mes_edhr_qa_menu`.
- GREEN: local Docker MySQL apply of `20260804_mes_edhr_qa_menu.sql` -> PASS with no stderr.
- GREEN: local DB verification query -> `批记录表单 sort=0`, `QA sort=1`, `批次执行 sort=2`, `表单追溯 sort=3`, `表单日志 sort=4`; admin role bindings count `3`; tenant package bindings count `2`.

## Verification

- Static SQL contract covers release metadata, fail-fast guards, menu order, route/component consistency, tenant package binding, and admin role binding.
- Frontend static contract covers backend menu path and existing `remaining.ts` route agreement.
- Real browser menu-click E2E script `IntRuoyiFronted/tests/e2e/mes-edhr-qa-menu-real.e2e.js` passes `node --check` and real execution.
- Runtime E2E passed after local backend `48081` recovered: `芋道源码/admin` saw `QA` between `批记录表单` and `批次执行`, clicked it, and landed on `/mes/pro/process-pool/qa-regulation`.
- Local Docker MySQL verification also confirms menu order and admin/tenant package bindings.

## Blockers

- No database blocker remains for the eDHR QA menu migration. Repository closeout is still governed by the shared worktree/commit state outside this schema slice.
