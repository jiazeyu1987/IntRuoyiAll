# Bug Regression Evidence

## Bug Summary

用户反馈个人工作台显示“待办任务加载失败”，错误明细为“隐藏任务状态：系统异常”。前端错误来自隐藏任务状态接口 `/system/profile-workbench-task-visibility/hidden-keys` 返回系统异常，导致待办列表无法完成加载。

## Expected Behavior

本机运行库应包含 `system_profile_workbench_task_visibility` 表；隐藏任务状态接口应正常返回当前用户隐藏任务 Key 列表，个人工作台不应显示“待办任务加载失败”。

## Reproduction

- `docker exec int-ruoyi-mysql ... information_schema.tables ... system_profile_workbench_task_visibility` -> `0`，本机运行库缺表。
- `node tests\e2e\profile-unified-todo-list-real.e2e.js` 在缺表时会进入页面错误分支并显示“待办任务加载失败 / 隐藏任务状态：系统异常”。

## Root Cause

代码与正式迁移文件已经存在，但本机 Docker MySQL `ruoyi-vue-pro` 未应用 `20260727_system_profile_workbench_task_visibility.sql`，导致隐藏状态接口查询不存在的 `system_profile_workbench_task_visibility` 表并返回系统异常。

## Regression Test

新增 `IntRuoyiBackend\script\tests\test_system_profile_workbench_task_visibility_sql.py`，覆盖迁移元数据、建表字段、唯一键和非破坏性约束，防止该表迁移缺少正式契约测试。

## RED

- RED: `python -X utf8 -m pytest script\tests\test_system_profile_workbench_task_visibility_sql.py -q` -> FAIL，原因：测试文件不存在。
- RED: 本机 schema preflight -> FAIL，目标表数量为 `0`。

## GREEN

- GREEN: `python -X utf8 -m pytest script\tests\test_system_profile_workbench_task_visibility_sql.py -q` -> PASS，3 tests。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --sql-file sql\mysql\20260708_system_user_table_column_config.sql --sql-file sql\mysql\20260727_system_profile_workbench_task_visibility.sql --output ..\doc\tasks\20260727-todo-task-hidden-status\profile-workbench-visibility-migration-policy-gate.json` -> PASS。
- GREEN: 本机应用 `20260727_system_profile_workbench_task_visibility.sql` 后，目标表数量为 `1`。
- GREEN: `node tests\e2e\profile-unified-todo-list-real.e2e.js` -> PASS。

## Verification

迁移契约测试、release migration policy gate、本机 schema 复查和个人工作台真实 E2E 均已通过。

## Risk And Regression Scope

本次未引入 fallback、吞异常、默认成功或前端隐藏错误；修复范围限定为本机运行库迁移补齐和迁移契约测试。后续发布仍需由 release migration policy gate 纳入该 SQL 并在目标环境执行。

## Blockers And Follow-Up

- 新 worktree 缺少前端 `node_modules/playwright`，因此真实 E2E 使用已运行且依赖完整的主工作区前端执行；该验证不修改业务数据。
