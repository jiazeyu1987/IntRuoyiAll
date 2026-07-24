# 20260715 release metadata personal-file fix

## Task Goal

Fix the `build-release` blocker where `sql/mysql/20260714_dcc_personal_file_decommission.sql` is missing required release migration metadata.

## Milestones

- [x] Record task scope, BDD, and preflight gates.
- [x] Add failing regression coverage for the missing release metadata.
- [x] Add the correct `-- release-migration:` metadata to the SQL file.
- [x] Run focused SQL metadata tests and the release migration policy gate.
- [x] Commit only task-owned backend changes.

## Expected Verification

- Focused pytest for `test_dcc_personal_file_decommission_migration_has_release_metadata` fails before the SQL change and passes after.
- Full `script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --output <evidence>` passes.
- Commit contains only the SQL metadata fix, focused test, and this task record.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；补齐发布脚本要求的正式 migration metadata，并用测试锁定。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- SQL/release gate: any `sql/mysql/*.sql` file entering a release must start with `-- release-migration:` and pass `script/release/run-release-migration-policy-gate.py`.
- Commit isolation gate: backend main workspace contains unrelated staged/dirty work; use path-limited commit only for this task's files and do not disturb unrelated index/worktree entries.

## Current Status

completed - SQL metadata and dependency-format fixes are implemented and verified; path-limited backend commit is being created without disturbing unrelated staged/dirty work.
