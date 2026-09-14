# Data

SQL data change updates eDHR child menu seed/migration data for QA, production leader, PQC leader, batch execution, trace, and log visibility under parent menu `900220`.

## Migration

- File: `IntRuoyiBackend/sql/mysql/20260804_mes_edhr_qa_menu.sql`.
- Engine/tooling: MySQL migration SQL; applied to local `int-ruoyi-mysql` / `ruoyi-vue-pro` for current admin visibility, and retained as release migration for test/backup/prod.
- New/updated menu contract: `900434 QA`, `900436 生产组长`, `900435 PQC组长`, plus adjusted visible sort order for existing eDHR children.
- Package/role binding logic includes QA plus both leader menus for formal admin/existing eDHR role scopes.

## Safety

- No destructive table or column changes.
- Procedure fails fast if menu ids or paths are already occupied by incompatible active menus.
- Tenant package and role-menu checks raise SQL errors if required menu bindings are missing.

## Rollback

- Rollback path is to revert this menu seed/migration file before deployment or apply a targeted menu data rollback approved by release/database owner; no automatic fallback or silent downgrade is introduced.

## BDD

- BDD: 菜单顺序 -> Given eDHR parent menu `900220`, When visible child menu order is materialized, Then order is `批记录表单`, `QA`, `生产组长`, `PQC组长`, `批次执行`, `表单追溯`, `表单日志`.
- BDD: 生产组长菜单 -> Given production leader menu exists, When route/component are inspected, Then it uses `/mes/pro/process-pool/production-leader` and `mes/pro/processpool/ProductionLeaderWorkbenchPage`.
- BDD: PQC组长菜单 -> Given PQC leader menu exists, When route/component are inspected, Then it uses `/mes/pro/process-pool/pqc-leader` and `mes/pro/processpool/PqcLeaderWorkbenchPage`.

## RED

- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_qa_menu_sql.py` -> FAIL, expected reason: SQL did not include production leader menu id `900436` and seven-entry visible order.

## GREEN

- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_qa_menu_sql.py` -> PASS, 3 passed.

## Verification

- Static SQL contract verifies id/path/component/name/order, tenant package inclusion, and role-menu bindings for QA plus both leader menus.
- Local DB readback verified tenant 1 `admin` and tenant 122 `admin` each have effective bindings for `900434`, `900435`, and `900436`.
- A transient over-broad local binding attempt was soft-deleted; follow-up readback showed no non-admin role without existing eDHR anchors retained these new menu bindings.

## Blockers

- Remote/test-server/prod migration execution was not performed in this turn; only the local Docker MySQL runtime was updated.
- Commit/push closeout is blocked by unrelated dirty/ahead workspace state.
