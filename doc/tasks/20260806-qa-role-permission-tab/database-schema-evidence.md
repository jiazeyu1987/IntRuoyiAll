# Database Schema Evidence

## Data Change Goal And Affected Entities

- Goal: Seed or verify QA permission role and admin QA selection permission where missing.
- Affected entities: `system_menu`, `system_role`, `system_role_menu`, `system_user_role`, `system_tenant_package` read scope.

## Database Engine And Migration Tool

- Engine: MySQL.
- Migration/seed path: SQL migrations under `IntRuoyiBackend/sql/mysql`.

## Schema, Migration, Fixture, Seed, Index, Or Constraint Changes

- Updated fresh seed `20260804_mes_edhr_qa_menu.sql` so QA menu `900434` uses `mes:qa-inspection-regulation:query`.
- Added incremental migration `20260806_mes_qa_role_permission_tab.sql`.
- Incremental migration restores or creates role code `qa`, grants menus `900434`, `5631`, and `5633`, assigns tenant 1 `admin` user to role `qa`, and soft-disables QA menu `900434` from non-`qa` roles.
- Tenant 1 is explicitly included in the QA role target set because seed data uses `system_tenant.package_id = 0` for tenant 1, so admin assignment must not depend on tenant-package menu scanning.

## Data Safety Analysis

- Additive role/user-role grants plus soft-delete of non-QA QA-menu grants only.
- No `DELETE`, `TRUNCATE`, or destructive schema operations.
- Duplicate `qa` role codes per target tenant fail fast instead of guessing a role.

## Rollback Or Recovery Plan

- Rollback by restoring `system_menu.permission` for menu `900434`, reactivating previously soft-deleted non-QA `system_role_menu` rows if business approval requires broader visibility, and removing task-created `qa` role/user-role/menu rows after confirming no QA users depend on them.

## BDD Scenarios

- BDD: QA role seed exists -> Given migration/seed files are applied When roles and menu permissions are loaded Then a QA permission role is present.
- BDD: admin has QA selection permission -> Given QA-specific selection permission is required When admin role is seeded Then admin receives that permission.

## RED Command And Expected Failure

- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_qa_role_permission_tab_sql.py` -> FAIL, expected because `20260806_mes_qa_role_permission_tab.sql` did not exist.
- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_qa_role_permission_tab_sql.py` -> FAIL, expected because tenant 1 admin was not explicitly included outside tenant-package scanning.

## GREEN Command And Passing Result

- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_qa_role_permission_tab_sql.py IntRuoyiBackend\script\tests\test_mes_edhr_qa_menu_sql.py` -> PASS, 7 passed.

## Migration Verification

- `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file <19-file dependency closure>` -> PASS, migrationCount=19.
- Full SQL directory gate is blocked by unrelated existing `20260805_erp_nas_table_auto_sync.sql` metadata (`invalid type: schema,job`), so verification used the exact dependency closure for the current migration.

## Blockers

- No migration blocker for the current dependency closure.
- Closeout blocker: shared worktree has unrelated dirty changes; commit/push not performed.
