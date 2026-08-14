# Frontend Feature Evidence

## Feature Goal

- Restrict the visible `PQC组长` navigation tab to users with `pqc_leader_permission`.

## Non-Goals

- Do not redesign the PQC leader workbench.
- Do not alter production leader page behavior.
- Do not introduce CSS hiding or fallback empty-data behavior.

## Requirements

- Acceptance: `PQC组长` route permission must use `mes:pro-process-pool-pqc-leader:query`.
- Acceptance: static contracts must prove the route and SQL migration use the same dedicated permission.
- Acceptance: static contracts must prove the PQC leader role, not broad admin or generic team-leader role bindings, owns the visible menu.

## UI Entry Points

- Route: `/mes/pro/process-pool/pqc-leader`.
- Component: `IntRuoyiFronted/src/views/mes/pro/processpool/PqcLeaderWorkbenchPage.vue`.
- Route file: `IntRuoyiFronted/src/router/modules/remaining.ts`.

## API Contracts and Data States

- Menu visibility comes from backend dynamic menus and `system_role_menu`.
- Existing page APIs still use `mes:pro-process-pool-team-leader:*`, so generic API permissions are granted through hidden/button menu rows, not the visible PQC tab menu.

## BDD Scenarios

- BDD: PQC组长页签仅对 PQC组长权限角色可见 -> Given user has `pqc_leader_permission` / When dynamic menus load / Then `PQC组长` is visible.
- BDD: 非 PQC组长权限角色不可见 -> Given user lacks `pqc_leader_permission` / When dynamic menus load / Then `PQC组长` is not visible.

## RED:

- `node IntRuoyiFronted\tests\e2e\mes-edhr-qa-menu-static.spec.js` -> FAIL，PQC 动态菜单合同期待专属权限，但当前 SQL 仍为 `mes:pro-process-pool-team-leader:query`。

## GREEN:

- `node IntRuoyiFronted\tests\e2e\mes-edhr-qa-menu-static.spec.js` -> PASS，确认 `PQC组长` route meta 与动态菜单均使用 `mes:pro-process-pool-pqc-leader:query`。
- `git diff --check -- IntRuoyiFronted/src/router/modules/remaining.ts IntRuoyiFronted/tests/e2e/mes-edhr-qa-menu-static.spec.js` -> PASS，仅 CRLF 提示。

## Responsive Accessibility Loading Empty Error Permission Checks

- Permission check is the primary scope: route meta and dynamic menu permission must match the dedicated PQC leader permission.
- No layout or responsive behavior is changed.

## E2E or Component Verification Path

- Static contract: `node tests/e2e/mes-edhr-qa-menu-static.spec.js`.
- Static contract: `python -X utf8 IntRuoyiBackend/script/tests/test_mes_pqc_leader_role_permission_tab_sql.py`.

## Blockers and Follow-Up Skills

- `node IntRuoyiFronted\tests\e2e\mes-process-pool-team-leader-static.spec.js` 当前仍失败在既有无关稳定选择器断言；本次权限收敛合同已通过。
