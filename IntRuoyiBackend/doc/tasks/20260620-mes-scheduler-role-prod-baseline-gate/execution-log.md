# 执行日志: 20260620-mes-scheduler-role-prod-baseline-gate

- BDD: 正式库无排产角色时迁移必须 no-op -> Given system_menu 与租户包菜单基线已存在但正式库没有启用中的排产/计划角色 / When 执行 20260617_mes_scheduler_role_smart_scheduling_tab 迁移 / Then 迁移应直接结束而不是 SIGNAL 失败。
- BDD: 存在目标角色时仍必须只授权智能排产菜单树 -> Given 某租户存在启用中的排产/计划角色且租户包包含 900120 菜单树 / When 执行 20260617 迁移 / Then 仍只同步智能排产菜单树并保持幂等。

- GREEN: experience-preflight -> PASS，已读取 `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md` 命中的 `release-backup-restore.md` 与 `server-access.md`，本任务允许继续做正式库只读探查与发布阻塞根因修复。
- GREEN: previous-task-check -> PASS，上一后端任务 `20260620-dcc-file-view-matrix-migration-dependency` 已 `COMPLETED`。
- GREEN: prod-baseline-readonly-audit -> PASS，已通过 SSH 只读查询正式服 `172.30.30.57` 上的 `intruoyi-mysql`：`system_menu` 已存在 `900120/5590/5580/5262/5540`；`system_tenant_package` 表结构正常；正式租户 `芋道源码(id=1)` 与 `瑛泰医疗(id=162)` 的 `package_id=0`；库内不存在任何启用中的 `排产员/计划员/生产计划员/排产员/计划员` 或 `planner/scheduler/mes_*` 角色。
- RED: python -X utf8 -m pytest script\tests\test_mes_scheduler_role_smart_scheduling_tab_sql.py -q -> FAIL，`1 failed, 4 passed`；新增契约 `test_scheduler_role_smart_scheduling_sql_noops_when_no_target_roles_exist` 证明当前 SQL 仍包含 `Missing enabled MES scheduler/planner role with smart scheduling tenant package` 阻塞语义。
- GREEN: sql-noop-fix -> PASS，已将 `20260617_mes_scheduler_role_smart_scheduling_tab.sql` 改为：当 `tmp_mes_scheduler_role_targets` 为空时，显式 `DROP TEMPORARY TABLE` 并 `LEAVE ensure_mes_scheduler_role_smart_scheduling_tab`，不再 SIGNAL 失败。
- GREEN: python -X utf8 -m pytest script\tests\test_mes_scheduler_role_smart_scheduling_tab_sql.py -q -> PASS，返回 `5 passed`。
- GREEN: python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql -> PASS，返回 `status=passed, migrationCount=166`。
