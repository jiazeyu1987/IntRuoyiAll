# Database Schema Evidence

## Data Change Goal

新增 Codex 自动测试管理持久化模型、菜单、按钮权限、测试管理员角色和 tenant 1 `admin` 赋权。

## Engine And Migration

- Database engine: MySQL migration under `IntRuoyiBackend/sql/mysql/20260724_system_codex_test_management.sql`.
- Test fixture: H2 schema under `IntRuoyiBackend/yudao-module-system/src/test/resources/sql/create_tables.sql`.
- Migration metadata: `allowedEnvironments=test,backup,prod; dependsOn=20260721_admin_full_scope_role_standardization; type=schema; riskLevel=medium`.

## Schema Changes

- Added `system_codex_test_case`, `system_codex_test_checkpoint`, `system_codex_test_execution`, `system_codex_test_execution_case`, `system_codex_test_checkpoint_result`, `system_codex_test_artifact`, `system_codex_test_runner_session`.
- Expanded `system_tenant_package.menu_ids` to `LONGTEXT`.
- Seeded `系统管理 > 测试管理` menu and button permissions `system:codex-test:*`.
- Seeded role code `codex_test_admin` and assigned it to enabled tenant 1 `admin`.

## Data Safety

- Menu and role binding use stable business keys: permission code and role code.
- Admin assignment resolves user and role dynamically; no fixed user-role relation ID is used.
- Tenant package menu merge validates JSON before updating menu IDs.

## Rollback

- Rollback requires an explicit reverse migration for the seven `system_codex_test_*` tables, `system:codex-test:*` menus, role-menu bindings, and `codex_test_admin` user-role binding.
- No destructive rollback was executed in this task.

## BDD

- BDD: 测试管理员菜单权限迁移 -> Given 系统管理菜单和 tenant 1 admin 存在 / When migration applies / Then `codex_test_admin` role, menu permissions, tenant package menu IDs, and admin role binding exist.

## Verification

- RED: `python -X utf8 -m pytest script\tests\test_codex_test_management_migration.py -q` failed before migration existed.
- GREEN: `python -X utf8 -m pytest script\tests\test_codex_test_management_migration.py -q` passed with 2 tests.

## Blockers

- Real environment SQL execution was not performed in this task; remote/server/database operation was not authorized.
