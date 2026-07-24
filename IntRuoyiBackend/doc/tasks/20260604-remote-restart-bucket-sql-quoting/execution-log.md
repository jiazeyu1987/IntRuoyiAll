# 执行日志：修复测试服后端重启 bucket 检查命令解析失败

- BDD: 远端后端重启前应正确解析 bucket SQL -> Given 运行控制台请求重启测试服 backend 且需要读取 `infra_file_config.master` bucket / When PowerShell 通过 SSH 发送远端 Bash 检查脚本 / Then 远端 shell 必须把 SQL 当作 mysql `-e` 参数执行，不能因 SQL 函数括号被 Bash 解析而失败。
- BDD: 展厅媒体缺失仍需 fail fast -> Given SQL 可正常执行但 master bucket、MinIO 容器或样例对象缺失 / When 重启 backend / Then 脚本必须在 `docker compose restart backend` 前失败并输出明确原因。
- VERIFY：上一后端任务 `doc/tasks/20260604-runtime-control-rollback-target-backend/task.md` 状态为 `completed`。
- RED: `python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py::test_remote_restart_quotes_mysql_bucket_sql_for_ssh_shell -q` -> FAIL，预期原因：`restart-int-ruoyi-remote.ps1` 仍使用 `mysql ... -e "SELECT ... JSON_EXTRACT(config, '$.bucket') ..."`，没有使用远端 shell 安全的单引号 SQL。
- FIX：`restart-int-ruoyi-remote.ps1` 将 bucket 查询改为 `mysql ... -e 'SELECT ...'`，SQL 内部使用 `CAST(0x242e6275636b6574 AS CHAR)` 表达 `$.bucket`，使用 `SUBSTRING(CAST(0x20 AS CHAR), 1, 0)` 表达空字符串，避免 SQL 内部嵌套 shell 引号。
- GREEN: `python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py::test_remote_restart_quotes_mysql_bucket_sql_for_ssh_shell -q` -> PASS。
- GREEN: `python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py::test_remote_restart_blocks_showroom_media_bucket_inconsistency_before_backend_restart -q` -> PASS。
- GREEN: `python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py -q` -> PASS，13 passed。
- CHECK: `python -X utf8 -` 调用 `D:\Programs\Git\bin\bash.exe -n -c <bucket检查片段>` -> PASS，修复后的远端 Bash 片段可解析。
- GREEN: bug regression evidence validator -> PASS。
- GREEN: `git diff --check -- script/deploy/restart-int-ruoyi-remote.ps1 script/tests/test_runtime_control_scripts.py doc/tasks/20260604-remote-restart-bucket-sql-quoting` -> PASS，仅有 Windows 行尾规范化提示。
- CLOSEOUT PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-remote-restart-bucket-sql-quoting --mode preview` -> READY，delete `<none>`，blocked `<none>`，warnings `<none>`。
