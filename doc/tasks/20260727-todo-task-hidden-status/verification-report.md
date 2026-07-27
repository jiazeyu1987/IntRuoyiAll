# Verification Report

## Summary

- Root cause: local Docker MySQL `ruoyi-vue-pro` missed `system_profile_workbench_task_visibility`.
- Fix applied: ran the existing idempotent migration `IntRuoyiBackend\sql\mysql\20260727_system_profile_workbench_task_visibility.sql` against local Docker MySQL.
- Regression guard: added `IntRuoyiBackend\script\tests\test_system_profile_workbench_task_visibility_sql.py`.

## Commands

- `python -X utf8 -m pytest script\tests\test_system_profile_workbench_task_visibility_sql.py -q` -> PASS, 3 tests.
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --sql-file sql\mysql\20260708_system_user_table_column_config.sql --sql-file sql\mysql\20260727_system_profile_workbench_task_visibility.sql --output ..\doc\tasks\20260727-todo-task-hidden-status\profile-workbench-visibility-migration-policy-gate.json` -> PASS.
- Local DB schema verification -> PASS, table count `1` and expected columns present.
- `node tests\e2e\profile-unified-todo-list-real.e2e.js` from `E:\IntRuoyi\IntRuoyiFronted` -> PASS.
- `mvn -pl yudao-module-system -am "-Dtest=ProfileWorkbenchTaskVisibilityServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- `node tests\e2e\profile-workbench-task-hide-restore-static.spec.js` -> PASS.
- `node tests\e2e\profile-unified-todo-list-static.spec.js` -> PASS.
- `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS.
- `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py -q` -> PASS, 11 tests.

## Result

个人工作台待办列表真实页面验证通过，未再出现“待办任务加载失败 / 隐藏任务状态：系统异常”。

## Closeout

- Implementation commit `6325516cb7516a21ff127aaeaf2485dafe745f5d` was pushed to `origin/codex/20260727-todo-task-hidden-status`.
- Cleanup apply / worktree removal remains blocked by local `int_main` ahead/dirty state outside this task; the task remains `ready_for_closeout`.
