# 执行日志：修复远端后端重启 bucket SQL collation 错误

- BDD: 远端 bucket 查询不得触发 collation 混用 -> Given 测试服 MySQL JSON 字段返回 `utf8mb4_bin` 字符串 / When 重启脚本查询 master bucket / Then SQL 不得使用会混合 `utf8mb4_bin` 与默认 `latin1_swedish_ci` 的 `COALESCE` 字符串表达式。
- BDD: master bucket 缺失仍需 fail fast -> Given SQL 查询结果为 `NULL`、空字符串或无记录 / When 重启 backend / Then shell 必须把 bucket 判定为缺失并在重启前失败。
- VERIFY：上一后端任务 `doc/tasks/20260604-remote-restart-bucket-sql-quoting/task.md` 状态为 `completed`。
- RED: `python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py::test_remote_restart_bucket_sql_avoids_collation_sensitive_coalesce -q` -> FAIL，预期原因：脚本仍包含 `COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config...`。
- FIX：`restart-int-ruoyi-remote.ps1` 将 bucket SQL 改为 `SELECT JSON_UNQUOTE(JSON_EXTRACT(config, CAST(0x242e6275636b6574 AS CHAR CHARACTER SET utf8mb4))) ...`，不再使用 `COALESCE` 混合字符串排序规则；shell 在 SQL 成功后把返回值 `NULL` 显式归为空 bucket，再沿用原 fail-fast 检查。
- GREEN: `python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py::test_remote_restart_bucket_sql_avoids_collation_sensitive_coalesce -q` -> PASS。
- GREEN: `python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py::test_remote_restart_quotes_mysql_bucket_sql_for_ssh_shell -q` -> PASS。
- GREEN: `python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py::test_remote_restart_blocks_showroom_media_bucket_inconsistency_before_backend_restart -q` -> PASS。
- GREEN: `python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py -q` -> PASS，14 passed。
- CHECK: `python -X utf8 -` 调用 `D:\Programs\Git\bin\bash.exe -n -c <bucket检查片段>` -> PASS，修复后的远端 Bash 片段可解析。
- GREEN: bug regression evidence validator -> PASS。
- GREEN: `git diff --check -- script/deploy/restart-int-ruoyi-remote.ps1 script/tests/test_runtime_control_scripts.py doc/tasks/20260604-remote-restart-bucket-sql-collation` -> PASS，仅有 Windows 行尾规范化提示。
- CLOSEOUT PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-remote-restart-bucket-sql-collation --mode preview` -> READY，delete `<none>`，blocked `<none>`，warnings `<none>`。
