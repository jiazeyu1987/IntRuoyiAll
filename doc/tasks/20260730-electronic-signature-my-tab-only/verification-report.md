# Verification Report

## Result

READY FOR CLOSEOUT. Implementation and required static/type/migration verification passed. Final commit/push closeout remains blocked by unrelated concurrent repository state.

Follow-up runtime data repair completed for the reported `zhaojie` screenshot: local role-menu data now returns only the electronic-signature root and “我的签名” for `zhaojie` in tenant 1.

## Commands

- `node tests/e2e/electronic-signature-my-tab-only-static.spec.js` -> PASS
- `python -m pytest script/tests/test_signature_regular_users_my_signature_only_sql.py` -> PASS, 3 passed
- `node tests/e2e/signature-governance-records-static.spec.js` -> PASS
- `node tests/e2e/signature-governance-e2e-static.spec.js` -> PASS
- `node --test scripts/signature-governance-page-contract.test.mjs` -> PASS, 12 passed
- `python -m pytest script/tests/test_signature_my_signature_admin_menu_sql.py script/tests/test_admin_full_scope_role_standardization_sql.py` -> PASS, 12 passed
- `pnpm ts:check` -> PASS
- `python script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` -> PASS, migrationCount=398
- `docker exec int-ruoyi-mysql mysql apply IntRuoyiBackend/sql/mysql/20260730_signature_regular_users_my_signature_only.sql` -> PASS
- `docker exec int-ruoyi-mysql mysql read-only zhaojie signature menu query` -> PASS, effective menu ids `900218, 900418`

## Scope Verified

- 普通用户电子签名入口默认收敛到“我的签名”。
- 未授权用户不会 mount 全量签名记录/治理页签组件。
- 动态路由合并不会补回电子签名未授权隐藏静态子路由。
- 菜单 SQL 软收回普通角色治理页签，只保留根入口和“我的签名”。
- 本机运行态中，`zhaojie` 的租户 1 `wenkong` 角色旧治理菜单授权已软删除 14 条，左侧电子签名菜单数据源只剩“电子签名 / 我的签名”。

## Closeout Blocker

- `git status --short --branch --untracked-files=all` 显示分支 `int_main` 为 `[ahead 12, behind 8]`，且存在 unrelated dirty/untracked 任务文档改动。Per task ownership rules, this task cannot safely commit/push or mark `completed` in this turn.
