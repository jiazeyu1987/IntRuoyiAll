# Bug

`admin` 登录后看不到新增的 `生产组长` / `PQC组长` 主导航入口。

## Expected

`admin` 在本地库中应能看到 eDHR 下的 `QA`、`生产组长`、`PQC组长` 三个新增入口；普通角色不应因为 admin 同时拥有该角色而被误扩散。

## Reproduction

- Reproduction: local MySQL readback before applying the final migration showed only menu `900434 QA` existed and was bound; `900436 生产组长` / `900435 PQC组长` were absent from `system_menu` and admin role bindings.
- Reproduction: static SQL contract was expanded to cover admin visibility expectations and initially failed before the SQL contract was corrected.

## Root Cause

The code migration had been updated in the workspace but had not yet been applied to the local MySQL runtime, so the running local `system_menu` still lacked the two leader menu rows. A first attempted local correction was too broad because it bound admin's non-admin roles; that was immediately soft-deleted and the source migration kept to formal admin/existing eDHR role scopes.

## Regression Test

- Updated `IntRuoyiBackend/script/tests/test_mes_edhr_qa_menu_sql.py` to keep the QA/production/PQC menu ids, package inclusion, and admin role binding contract covered.

## RED

- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_qa_menu_sql.py` -> FAIL, expected reason: admin visibility regression contract did not find the required SQL evidence before the fix.

## GREEN

- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_qa_menu_sql.py` -> PASS, 3 passed.
- GREEN: local MySQL migration apply -> PASS; tenant 1 and tenant 122 `admin` each have effective bindings for `900434`, `900435`, and `900436`.

## Verification

- `node tests\e2e\mes-edhr-qa-menu-static.spec.js` -> PASS.
- `node tests\e2e\edhr-batch-record-leader-tabs-static.spec.js` -> PASS.
- `node tests\e2e\edhr-batch-page-graph-tab-static.spec.js` -> PASS.
- `node tests\e2e\mes-process-pool-team-leader-static.spec.js` -> PASS.
- `pnpm ts:check` -> PASS.
- Mis-scoped local role bindings were soft-deleted; readback showed no non-admin role without existing eDHR anchors retained the new menu bindings.

## Risk

Browser-side cached permission trees may require refresh or re-login after the local DB migration.

## Blockers

- Real browser login E2E was not run because this turn did not establish login/test-tenant credentials.
- Final commit/push remains blocked by unrelated dirty workspace state and branch ahead status.
