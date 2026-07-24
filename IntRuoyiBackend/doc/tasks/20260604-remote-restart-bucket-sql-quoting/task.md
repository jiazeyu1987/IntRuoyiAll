# 任务：修复测试服后端重启 bucket 检查命令解析失败

## 任务目标

修复运行控制台重启测试服务器后端时，远端展厅媒体 bucket 一致性检查在 SSH Bash 中解析失败的问题。脚本必须继续 fail fast 校验 `infra_file_config.master` bucket、MinIO 容器状态和样例对象存在性，不得跳过检查、吞掉错误或改写测试服数据。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260604-runtime-control-rollback-target-backend/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务只修改远端重启脚本、对应脚本契约测试和本任务文档。

## BDD 场景

- BDD: 远端后端重启前应正确解析 bucket SQL -> Given 运行控制台请求重启测试服 backend 且需要读取 `infra_file_config.master` bucket / When PowerShell 通过 SSH 发送远端 Bash 检查脚本 / Then 远端 shell 必须把 SQL 当作 mysql `-e` 参数执行，不能因 SQL 函数括号被 Bash 解析而失败。
- BDD: 展厅媒体缺失仍需 fail fast -> Given SQL 可正常执行但 master bucket、MinIO 容器或样例对象缺失 / When 重启 backend / Then 脚本必须在 `docker compose restart backend` 前失败并输出明确原因。

## Milestones

- [x] M1：建立任务文档并确认上一后端任务完成。
- [x] M2：复现现有远端 SQL 引号契约缺陷并新增 RED 测试。
- [x] M3：修复远端重启脚本 SQL 引号方式，避免 Bash 解析 SQL 函数括号。
- [x] M4：运行 targeted 测试和回归测试，记录 GREEN 证据。
- [x] M5：运行 task-closeout-cleanup 预览并按验证结果提交本次改动。

## Expected Verification

- RED/GREEN：`python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py::test_remote_restart_quotes_mysql_bucket_sql_for_ssh_shell -q`
- GREEN：`python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py::test_remote_restart_blocks_showroom_media_bucket_inconsistency_before_backend_restart -q`
- GREEN：`python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py -q`
- GREEN：bug regression evidence validator
- GREEN：task-closeout-cleanup 预览

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。仍在 backend/full 重启前强制检查 bucket、MinIO 容器和样例对象。
- `是否从根因和长期维护角度解决`：是。修复 SSH 远端 shell 命令的 SQL 引号结构，避免 Windows PowerShell/SSH 传参导致 SQL 双引号丢失后被 Bash 解析。
- `是否存在临时补丁或绕过`：否。不跳过检查、不改测试服数据、不调整受保护文件配置。

## 当前状态

completed

## 验证结果

- VERIFY：上一后端任务 `doc/tasks/20260604-runtime-control-rollback-target-backend/task.md` 状态为 `completed`。
- RED：`python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py::test_remote_restart_quotes_mysql_bucket_sql_for_ssh_shell -q` -> FAIL，预期原因：远端重启脚本仍使用 `mysql ... -e "SELECT ..."`。
- GREEN：`python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py::test_remote_restart_quotes_mysql_bucket_sql_for_ssh_shell -q` -> PASS。
- GREEN：`python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py::test_remote_restart_blocks_showroom_media_bucket_inconsistency_before_backend_restart -q` -> PASS。
- GREEN：`python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py -q` -> PASS，13 passed。
- CHECK：`python -X utf8 -` 调用 `D:\Programs\Git\bin\bash.exe -n -c <bucket检查片段>` -> PASS，修复后的远端 Bash 片段可解析。
- GREEN：bug regression evidence validator -> PASS。
- GREEN：`git diff --check -- script/deploy/restart-int-ruoyi-remote.ps1 script/tests/test_runtime_control_scripts.py doc/tasks/20260604-remote-restart-bucket-sql-quoting` -> PASS，仅有 Windows 行尾规范化提示。
- CLOSEOUT PREVIEW：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-remote-restart-bucket-sql-quoting --mode preview` -> READY，delete `<none>`，blocked `<none>`，warnings `<none>`。

## Blockers

- 暂无。

## Cleanup Keep

- `doc/tasks/20260604-remote-restart-bucket-sql-quoting/bug-regression-evidence.md`
