# 执行日志：修复 MES 排产员角色缺失导致正式发布阻塞

- BDD: MES 排产员角色基线缺失发布阻塞 -> Given 正式租户 1 缺少启用且未删除的 `排产员/mes_scheduler` 角色；When 执行 `20260629_mes_smart_scheduling_role_scope.sql`；Then SQL 应恢复或创建排产员角色并继续完成角色菜单范围收口。
- GREEN: experience-preflight -> PASS，已按索引命中并遵守 release-build-preflight、release-agent-checklist、release-backup-restore、server-access、worktree-memory、powershell-memory。
- GREEN: prod-readonly-diagnosis -> PASS，正式库 `system_role` 租户 1 样本与排产员精确/模糊查询均未返回 `排产员/mes_scheduler`，`infra_release_migration` 记录 `20260629_mes_smart_scheduling_role_scope` 在 `prod` 失败，错误为 `Missing enabled MES scheduler role in tenant 1`。
- RED: `python -X utf8 -m pytest script/tests/test_mes_smart_scheduling_role_scope_sql.py -q` -> FAIL，新增合同测试确认当前 SQL 缺少 `v_scheduler_role_id`、排产员创建/恢复逻辑和租户 1 排产员目标解析。
- CHANGE: `sql/mysql/20260629_mes_smart_scheduling_role_scope.sql` -> 增加租户 1 `排产员/mes_scheduler` 幂等恢复、缺失创建和单一目标 ID 解析；非管理员租户仍只收集已有启用排产员角色。
- GREEN: `python -X utf8 -m pytest script/tests/test_mes_smart_scheduling_role_scope_sql.py script/tests/test_mes_smart_scheduling_role_assignment_sql.py -q` -> PASS，26 passed。
- GREEN: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` -> PASS，migrationCount=236，`20260629_mes_smart_scheduling_role_scope` sha=`63df8be74ec6782602378eac5ff838658e240d971ce2e440be1f7e10afd7a44c`。
- GREEN: `python -X utf8 tool/verify_tdd_compliance.py --repo . --task-dir doc/tasks/20260701-fix-mes-scheduler-role-scope-prod-missing` -> PASS。
- GREEN: backend-task-complete -> PASS，任务文档已标记完成；后续发布必须使用包含本修复的新提交和新 releaseTag。
