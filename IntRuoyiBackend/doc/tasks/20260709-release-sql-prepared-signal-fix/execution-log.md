# 20260709-release-sql-prepared-signal-fix 执行日志

- 2026-07-09T21:55:40
  - BDD: 测试服发布 SQL 不使用 prepared SIGNAL -> Given 发布 SQL 包含 fail-fast 数据冲突检查；When 发布脚本通过 mysql 客户端执行 required SQL；Then SQL 不得通过 PREPARE/EXECUTE 执行 SIGNAL SQLSTATE，避免 MySQL prepared-statement 协议 1295 错误。
  - RED: python -X utf8 -m pytest script/tests/test_release_sql_no_prepared_signal.py -q -> FAIL, expected reason: prepared SIGNAL detected in 20260613_mes_smart_scheduling_t1_schema.sql；证据见维护任务 evidence/red-release-sql-no-prepared-signal.log。
  - BLOCKER-RECORDED: backend-experience-index -> 后端仓缺少 docs/experience-index.md；影响=无法按后端仓本地索引摘取经验门禁；处理=记录流程问题，并按维护仓发布经验索引继续本次发布 SQL 修复。
  - GREEN: experience-preflight -> PASS，已按维护仓 docs/experience-index.md 命中发布/SQL/迁移相关经验并写入任务文档。
- 2026-07-09T21:58:00
  - GREEN: python -X utf8 -m pytest script/tests/test_release_sql_no_prepared_signal.py -q -> PASS，已移除 prepared SIGNAL；冲突门禁改为临时表主键冲突 fail-fast，避免 MySQL 1295。
- 2026-07-09T22:02:11
  - GREEN: python -X utf8 -m pytest script/tests/test_mes_smart_scheduling_t1_schema.py script/tests/test_mes_route_schedule_config_shared_sql.py script/tests/test_mes_route_schedule_config_item_capacity_sql.py script/tests/test_mes_schedule_order_key_process_sql.py script/tests/test_release_sql_no_prepared_signal.py -q -> PASS，11 passed；证据见维护任务 evidence/verify-mes-smart-scheduling-sql-contracts-after-test-update.log。
  - GREEN: python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql -> PASS；证据见维护任务 evidence/verify-release-migration-policy-gate-precommit.log。
- 2026-07-09T22:03:08
  - ISSUE: backend commit hook required TDD_TASK_DIR; 阶段=后端修复提交；现象=首次 git commit 被钩子拦截：TDD compliance failed: set TDD_TASK_DIR；影响=未产生提交，修复仍在暂存区；原因判断=提交钩子要求显式绑定任务目录；处理动作=记录问题并带 TDD_TASK_DIR 重试提交；结果=待重试；是否可前置检查=是；是否可自动化=是；下次避免=所有仓库提交前统一设置 TDD_TASK_DIR 为当前任务目录。
- 2026-07-09T22:04:17
  - GREEN: git commit -> PASS，commit=b0dedcdc904ec66800ecedd601cfc5ed7a9600a1，message=任务: 修复发布 SQL prepared SIGNAL。
  - GREEN: backend-fix-task-close -> PASS，任务文档标记完成，等待合入 int_main 后重建发布包。