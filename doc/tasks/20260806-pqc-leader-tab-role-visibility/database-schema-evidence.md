# Database Schema Evidence

## Data Change Goal

- Add a reversible menu-role migration that restricts visible menu `900435 / PQC组长` to role code `pqc_leader_permission`.

## Migration

- New migration: `IntRuoyiBackend/sql/mysql/20260806_mes_pqc_leader_role_permission_tab.sql`.
- It updates `system_menu.permission` for `900435`, creates/recovers the `pqc_leader_permission` role per target tenant, adds hidden permission menu `900439`, grants required menu/button permissions to that role, and soft-deletes active `900435` bindings from non-PQC-leader roles.

## Safety

- No remote environment access.
- No secret values are recorded.
- Non-target role-menu rows are soft-deleted only for visible menu `900435`; no hard delete/truncate is used.

## Rollback

- Revert the migration file and reapply the previous menu/role grant policy if rollback is requested.
- Local data rollback would restore previous `system_role_menu.deleted` values for `900435` and reset `system_menu.permission` for `900435`.

## BDD:

- BDD: PQC组长页签仅对 PQC组长权限角色可见 -> Given role code `pqc_leader_permission` exists / When migration runs / Then only that role has active `900435` binding.

## RED:

- `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_pqc_leader_role_permission_tab_sql.py IntRuoyiBackend\script\tests\test_mes_edhr_qa_menu_sql.py -q` -> FAIL，缺少 PQC 组长角色权限迁移，且 `900435` 仍使用通用组长查询权限。

## GREEN:

- `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_pqc_leader_role_permission_tab_sql.py IntRuoyiBackend\script\tests\test_mes_edhr_qa_menu_sql.py IntRuoyiBackend\script\tests\test_mes_qa_role_permission_tab_sql.py -q` -> PASS，`12 passed`。
- 本机 Docker MySQL `int-ruoyi-mysql / ruoyi-vue-pro` 应用 `20260806_mes_pqc_leader_role_permission_tab.sql` -> PASS。

## Verification

- 菜单复核：`900435 / PQC组长` permission = `mes:pro-process-pool-pqc-leader:query`，`900439 / PQC组长通用查询` permission = `mes:pro-process-pool-team-leader:query`。
- 角色绑定复核：非 `pqc_leader_permission` 有效绑定 `900435` 计数为 `0`；`pqc_leader_permission` 拥有 `900435,900439,900312,900313,900314`。
- admin 复核：tenant `1` `admin` 对 `pqc_leader_permission` 的有效绑定计数为 `1`。
- 发布门禁：`run-release-migration-policy-gate.py` 被既有无关 `20260805_erp_nas_table_auto_sync.sql` 的 `type=schema,job` 阻塞。

## Blockers

- 发布迁移总门禁失败原因不在本次新增迁移：`20260805_erp_nas_table_auto_sync.sql` release metadata `type=schema,job` 不合法。
