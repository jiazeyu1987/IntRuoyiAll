# Verification Report

## Result

- Completed: created tenant `1` PQC leader role `910439 / PQC组长权限角色 / pqc_leader_permission`.
- Completed: bound role menus `5100,900220,900435`.
- Completed: assigned the role to exactly 2 random eligible users with pre-bind role count less than 2.

## Assigned Users

- `617 / jiangdan / 蒋丹`: post-bind effective role count = `2`, has PQC leader role = `1`.
- `1467 / majing / 马静`: post-bind effective role count = `2`, has PQC leader role = `1`.

## Verification Evidence

- Role exists in tenant `1` and is active/undeleted.
- Role menu count is `3`; role menu IDs are `5100,900220,900435`.
- Task-owned `system_user_role` assignment count for role `910439` is `2`.
- Invalid selected users count is `0`.
- Duplicate active bindings count is `0`.
- Database schema evidence validator passed: `Database schema evidence is valid.`

## Rollback

- Remove task-owned `system_user_role` rows where `tenant_id = 1`, `role_id = 910439`, and `creator = 'codex-20260806-pqc-leader-role'`.
- For full rollback, remove `system_role_menu` rows for `role_id = 910439`, then remove `system_role.id = 910439` only after confirming no later non-task-owned bindings exist.

## Final Status

- Database mutation and post-write verification passed.
- Task cleanup applied; temporary schema evidence was removed after its validator PASS was copied into this report.
- Final task document status is `completed`.
