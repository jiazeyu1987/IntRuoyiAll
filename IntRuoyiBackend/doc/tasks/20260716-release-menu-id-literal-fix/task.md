# 20260716 release menu id literal fix

## Task Goal

Fix the release preflight blocker in `sql/mysql/20260714_signature_my_signature_admin_menu.sql` so menu ID lists parsed by the release preflight planner use static integer literals and the next code-only release can build a manifest.

## Scope

- Backend repository only: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`.
- SQL migration: `sql/mysql/20260714_signature_my_signature_admin_menu.sql`.
- Regression tests: `script/tests/test_signature_my_signature_admin_menu_sql.py`.
- No manual database updates, no server writes, no fallback, and no reuse of failed releaseTag `release-20260716-intmain-codeonly-three-env-r260716a-r1`.

## Milestones

- [x] Create backend task records.
- [x] Add failing SQL regression for release preflight static menu ID literals.
- [x] Update SQL to keep runtime behavior while making preflight-parsed menu ID lists statically parseable.
- [x] Run focused SQL tests and release migration policy gate.
- [ ] Commit only backend task-owned files.

## Expected Verification

- `python -X utf8 -m pytest script/tests/test_signature_my_signature_admin_menu_sql.py -q`
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --output <task-dir>/migration-policy-gate.json`
- Backend `git diff --check`.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；让 SQL 满足发布 preflight planner 的静态契约并补测试。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed - focused SQL regression passed, and the same patch passed full release migration policy gate in a clean backend release worktree with `migrationCount=303`; ready for isolated backend commit.
