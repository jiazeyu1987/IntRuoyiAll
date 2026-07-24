# Execution Log - 20260716 release menu id literal fix

## BDD Scenarios

BDD: release preflight parses signature menu ids -> Given the code-only release planner scans `20260714_signature_my_signature_admin_menu.sql`, When it extracts menu IDs from preflight-parsed menu ID lists, Then those IDs are static integer literals and not unresolved SQL variables.

## TDD / Verification Evidence

- RED: `python -X utf8 -m pytest script/tests/test_signature_my_signature_admin_menu_sql.py -q` -> FAIL, expected reason: new regression `test_release_preflight_menu_id_lists_use_static_integer_literals` found `(@unified_signature_menu_id)` instead of static literal `(900218)` in `tmp_signature_regular_menu_ids`.
- GREEN: `python -X utf8 -m pytest script/tests/test_signature_my_signature_admin_menu_sql.py -q` -> PASS, `5 passed`.
- GREEN: clean-worktree migration policy gate -> PASS, in `D:\ProjectPackage\Int\IntRuoyiWorktrees\r260716a\b` after applying the same patch, `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --output D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260716-current-head-codeonly-three-env\backend-fix-validation-migration-policy-gate.json` returned `status=passed`, `migrationCount=303`.

## Commands And Evidence

- 2026-07-16: Created backend task directory `doc/tasks/20260716-release-menu-id-literal-fix/` with `task.md` and `execution-log.md`.
- 2026-07-16: Main backend worktree full migration gate was not used as release evidence because unrelated uncommitted `20260715_mes_schedule_capacity_mode_unification.sql` failed metadata parsing; release verification was repeated in the clean `r260716a\b` worktree containing only this task's patch.
- 2026-07-16: Updated `20260714_signature_my_signature_admin_menu.sql` so preflight-parsed menu ID lists use literal `900218`, `900411`, `900418`, and `900413`, while retaining SQL variables for ordinary runtime checks and updates.
