# Verification Report

## Result

- Status: PASS for the approved scope “只同步两边都存在的用户角色绑定”。
- Target: test server `172.30.30.58`，database `ruoyi-vue-pro`，tenant `1/芋道源码`。
- Change result: `system_user_role` soft-deleted `12` target-only active bindings, reactivated `2` existing soft-deleted bindings, inserted `140` new bindings。
- Source parity: common-user source pairs `2282`，post-sync missing `0`，post-sync extra `0`。
- Boundary: other tenant `system_user_role` hash stayed `b460456a55b8f40b578bfb1512d4006e11d45de29beb56cc76f77cb82e76e117`。

## Key Evidence

- Pre-change RED: `verify-before-change.json` showed `VERIFY_MISSING=142` and `VERIFY_EXTRA=12`。
- Apply GREEN: official `apply-test-db-sql.ps1` returned `COMMITTED` and `APPLY_TEST_DB_SQL_OK`。
- Post-change GREEN: `verify-after-cache.json` showed `VERIFY_MISSING=0` and `VERIFY_EXTRA=0`。
- Backup: `pre-change-test-user-role-backup.sql` SHA-256 `8c409b3de44a3e9846cbc3b6a754f400189bba74834adc9538b3094049230501`。
- Rollback: `rollback.sql` restores the common-user target role binding set to the pre-change test-server state。
- Cache: affected users `111`; Redis scan found `0` matching `user_role_ids` keys, so delete request count was `0` and no full Redis clear was performed。
- Evidence validator: `validate_database_schema.py --evidence doc/tasks/20260808-test-user-role-bindings-sync-local/database-schema-evidence.md` passed。
- Cleanup: preview/apply kept all audit evidence and deleted only the task-owned `__pycache__` file。

## Wangsiyu / DCC

- `wangsiyu` post-sync effective roles: `approval_center_entry,dcc_action_distribute_independent,dcc_action_view_independent,dcc_distribute_e2e,doc_control,wenkong,wenkong_download`。
- Removed from `wangsiyu`: test-only `wenkong_no_download` because it was not present in local source for the same user。
- DCC risk explicitly accepted by the requested parity sync: `wangsiyu` now has `dcc:controlled-file:category:manage`、`dcc:controlled-file:directory:manage` and `dcc:controlled-file:download` through `wenkong` / `wenkong_download`。
- `zhaohaichen` also now has `wenkong` dangerous DCC permissions; details are recorded in `verify-dcc-dangerous-detail.json`。

## Exclusions

- No missing roles in test for the approved common-user scope.
- Not synced because users do not exist on test: `edhrmatrixapprover`、`pqce2efullscreen`、`smokeappr1`、`smokeerp1`、`smokeread1`。
- Excluded source bindings count: `8`。
- UI re-login was not run in this step; already logged-in sessions may need logout/login to pick up the changed role set if frontend-side permission state is cached.
