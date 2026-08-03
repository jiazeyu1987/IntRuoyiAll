# Database Schema Evidence

## Data Change Goal And Affected Entities

- Goal: Seed independent DCC action roles for view, download, training, and distribute permissions.
- Entities: `system_role`, `system_menu`, `system_role_menu`, `system_user_role`, `dcc_file_category_permission_rule`.

## Database Engine And Migration Tool

- Engine: Local Docker MySQL 8.0, container `int-ruoyi-mysql`, database `ruoyi-vue-pro`.
- Migration tool: Task-owned idempotent SQL executed through Docker MySQL stdin for local verification.

## Schema, Seed, Fixture, Index, Or Constraint Changes

- No schema, index, or constraint changes.
- Seed changes only: independent roles, menu bindings, category action rules, and scoped user-role bindings.

## Data Safety Analysis

- Non-destructive insert/update only.
- Does not delete legacy mixed roles.
- Does not update DCC file status, training progress, acknowledgement time, publication status, or master current pointer.
- Scope is local tenant `1` and target category `906104 / 其他`.

## Rollback Or Recovery Plan

- New bindings are identified by role codes `dcc_action_view_independent`, `dcc_action_download_independent`, `dcc_action_training_independent`, and `dcc_action_distribute_independent`.
- Rollback can remove those role-menu, user-role, category permission rows and then the roles by code, without touching legacy roles or DCC business records.

## BDD Scenarios

- BDD: DCC view role is independent -> Given/When/Then in execution-log.md.
- BDD: DCC download role is independent -> Given/When/Then in execution-log.md.
- BDD: DCC training role is independent -> Given/When/Then in execution-log.md.
- BDD: DCC distribute role is independent -> Given/When/Then in execution-log.md.

## RED Command And Expected Failure

- RED: local DB baseline query -> FAIL because the four target independent role codes do not yet exist and target accounts still rely on legacy mixed roles.

## GREEN Command And Passing Result

- GREEN: `Get-Content -LiteralPath 'E:\IntRuoyi\doc\tasks\20260803-dcc-action-role-split\role-split.sql' -Encoding utf8 -Raw | docker exec -i int-ruoyi-mysql sh -lc 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -uroot --batch --raw ruoyi-vue-pro'` -> PASS.
- Result: `precheck_status=OK`; `roleIds.view=910432`; `roleIds.download=910433`; `roleIds.training=910434`; `roleIds.distribute=910435`; `targetCategoryId=906104`.
- GREEN: `node -e "...role-split.sql static contract..."` -> PASS, no forbidden DCC business-state update/delete patterns found.

## Migration Verification

- Read-only DB verification -> PASS:
  - Four enabled independent roles exist in tenant `1`.
  - View role menus: `6807:dcc:controlled-file:query:controlled-file/browser` and `6810:dcc:controlled-file:preview`.
  - Download role menu: `6811:dcc:controlled-file:download`.
  - Training role menu: `980121:dcc:controlled-file:training:mine:controlled-file/training-mine`.
  - Distribute role menu count: `0`.
  - Category `906104` rules: view role=`VIEW`, download role=`DOWNLOAD`, distribute role=`DISTRIBUTE`, all `active=1`.
  - `wangsiyu` bindings: `dcc_action_distribute_independent|dcc_action_view_independent`.
  - Training users with training role: `9`; training users with new download role: `0`.
- Redis cache refresh -> PASS:
  - Deleted precise role/user/menu/permission cache keys for this task; `redis-cli DEL` returned `7`.
  - Rebuilt `user_role_ids:910250` contains new role IDs `910432/910435`.
  - Rebuilt `menu_role_ids:1:6807` contains new view role ID `910432`.

## Blockers

- No blocker for additive split.
- Legacy mixed role removal is intentionally not part of this run because it can revoke existing business access and needs an approved migration/rollback plan.
- Final Git commit/push closeout is blocked by unrelated dirty workspace state and branch `ahead 2`; this task did not commit or push unrelated changes.
