# Verification Report

## Result

- Scope complete for PQC 组长页签权限收敛：前端 route、动态菜单权限、PQC 组长角色和本机数据库有效菜单绑定均已收敛。
- Release closeout remains blocked by an existing unrelated migration metadata failure in `20260805_erp_nas_table_auto_sync.sql`.

## Evidence

- Frontend: `node IntRuoyiFronted\tests\e2e\mes-edhr-qa-menu-static.spec.js` -> PASS。
- Backend SQL: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_pqc_leader_role_permission_tab_sql.py IntRuoyiBackend\script\tests\test_mes_edhr_qa_menu_sql.py IntRuoyiBackend\script\tests\test_mes_qa_role_permission_tab_sql.py -q` -> PASS，`12 passed`。
- Local DB: `900435 / PQC组长` permission = `mes:pro-process-pool-pqc-leader:query`，`900439 / PQC组长通用查询` permission = `mes:pro-process-pool-team-leader:query`。
- Local DB: active non-`pqc_leader_permission` bindings for `900435` = `0`；tenant `1` admin has active `pqc_leader_permission` binding count = `1`。
- Format: `git diff --check -- <本任务触碰文件>` -> PASS，仅 CRLF 提示。

## Blockers

- `node IntRuoyiFronted\tests\e2e\mes-process-pool-team-leader-static.spec.js` still fails on an unrelated existing PQC list selector assertion.
- `run-release-migration-policy-gate.py` fails on unrelated `20260805_erp_nas_table_auto_sync.sql` metadata: `type=schema,job`。
