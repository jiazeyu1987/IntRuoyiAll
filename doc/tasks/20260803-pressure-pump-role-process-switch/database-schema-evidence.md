# Database Schema Evidence: Pressure Pump All-Process Permission

## Data Change Goal And Affected Entities

- Goal: add a role-assignable permission menu for one-line production pressure-pump all-process switching.
- Affected entities: `system_menu`, `system_tenant_package.menu_ids`, `system_role_menu`.

## Database Engine And Migration Tool

- Engine: MySQL-compatible migration script under `IntRuoyiBackend/sql/mysql`.
- Migration gate: `IntRuoyiBackend/script/release/run-release-migration-policy-gate.py`.

## Schema, Migration, Fixture, Seed, Index, Or Constraint Changes

- Migration file: `IntRuoyiBackend/sql/mysql/20260803_mes_frontline_pressure_pump_all_process_permission.sql`.
- The migration creates or updates menu id `900450` under parent menu `5550`.
- The permission value is `mes:pro-feedback:frontline-pressure-pump:all-processes`.
- The migration merges the new menu id into tenant packages that already include the parent menu and grants it to tenant administrator roles in those packages.

## Data Safety Analysis

- The script fails fast if parent menu `5550` is missing.
- The script fails fast if menu id `900450` is already used by another active menu.
- The script fails fast if the same permission exists under a different active id.
- The script fails fast if any active tenant package has invalid `menu_ids` JSON.
- Chinese menu name is written with UTF-8 hex conversion instead of relying on client literal encoding.

## Rollback Or Recovery Plan

- Low-risk permission migration. Recovery is to mark menu id `900450` deleted and remove it from affected package/role menu assignments through a controlled rollback migration.
- No destructive schema changes, data drops, or table rewrites are performed.

## BDD Scenarios

- BDD: permission menu is formally assignable -> Given MES feedback parent menu exists, When migration runs, Then pressure-pump all-process permission exists as a system menu permission.
- BDD: tenant packages stay consistent -> Given a tenant package already includes the parent MES feedback menu, When migration runs, Then the new permission id is merged into the package menu id JSON.
- BDD: invalid or conflicting menu data fails fast -> Given parent menu, menu id, permission uniqueness, or package JSON prerequisites are invalid, When migration runs, Then the procedure signals an error instead of silently inserting partial permissions.

## RED

- RED: permission lookup before migration -> FAIL, expected because no committed migration existed to create `mes:pro-feedback:frontline-pressure-pump:all-processes`.

## GREEN

- GREEN: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file IntRuoyiBackend\sql\mysql\20260803_mes_frontline_pressure_pump_all_process_permission.sql --output doc\tasks\20260803-pressure-pump-role-process-switch\migration-policy-gate.json` -> PASS, 1 migration, sha256 `4ff6ac8bc5cf101d1a4bdb453451860b39735773191cb790d29dd253b1d2bf46`.

## Migration Verification

- Release migration policy gate passed for migration id `20260803_mes_frontline_pressure_pump_all_process_permission`.
- The migration metadata allows `test`, `backup`, and `prod`, type `permission`, risk `low`.

## Blockers

- No database-schema blocker remains for committing this migration.
