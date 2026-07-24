# 任务：修复远端后端重启 bucket SQL collation 错误

## 任务目标

修复测试服后端重启前置 bucket 检查中的 MySQL collation 错误。脚本已经应用上一轮 SSH-safe SQL 改动，但 `COALESCE(JSON_UNQUOTE(...), SUBSTRING(...))` 在测试服 MySQL 上触发 `Illegal mix of collations`。本任务改为不在 SQL 内混合默认字符串表达式，由 shell 显式把 SQL `NULL` 视为缺失 bucket 并继续 fail fast。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260604-remote-restart-bucket-sql-quoting/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务只修改远端重启脚本、对应脚本契约测试和本任务文档。

## BDD 场景

- BDD: 远端 bucket 查询不得触发 collation 混用 -> Given 测试服 MySQL JSON 字段返回 `utf8mb4_bin` 字符串 / When 重启脚本查询 master bucket / Then SQL 不得使用会混合 `utf8mb4_bin` 与默认 `latin1_swedish_ci` 的 `COALESCE` 字符串表达式。
- BDD: master bucket 缺失仍需 fail fast -> Given SQL 查询结果为 `NULL`、空字符串或无记录 / When 重启 backend / Then shell 必须把 bucket 判定为缺失并在重启前失败。

## Milestones

- [x] M1：建立任务文档并确认上一后端任务完成。
- [x] M2：新增 RED 测试复现 collation-sensitive SQL 契约缺陷。
- [x] M3：修复远端 bucket SQL，移除 `COALESCE` 字符串混合。
- [x] M4：运行 targeted 测试、全量脚本测试与证据校验。
- [x] M5：运行 task-closeout-cleanup 预览并提交本次改动。

## Expected Verification

- RED/GREEN：`python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py::test_remote_restart_bucket_sql_avoids_collation_sensitive_coalesce -q`
- GREEN：`python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py::test_remote_restart_quotes_mysql_bucket_sql_for_ssh_shell -q`
- GREEN：`python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py -q`
- GREEN：bug regression evidence validator
- GREEN：task-closeout-cleanup 预览

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。SQL 执行失败仍由 `set -e` 中断；只把 SQL 正常返回的 `NULL` 明确判定为缺失 bucket。
- `是否从根因和长期维护角度解决`：是。移除 collation-sensitive 的 SQL 默认字符串表达式，避免依赖 MySQL 默认字符集。
- `是否存在临时补丁或绕过`：否。不跳过展厅媒体一致性检查、不改测试服数据、不切换受保护文件配置。

## 当前状态

completed

## 验证结果

- VERIFY：上一后端任务 `doc/tasks/20260604-remote-restart-bucket-sql-quoting/task.md` 状态为 `completed`。
- RED：`python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py::test_remote_restart_bucket_sql_avoids_collation_sensitive_coalesce -q` -> FAIL，预期原因：脚本仍包含 `COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config...`。
- GREEN：`python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py::test_remote_restart_bucket_sql_avoids_collation_sensitive_coalesce -q` -> PASS。
- GREEN：`python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py::test_remote_restart_quotes_mysql_bucket_sql_for_ssh_shell -q` -> PASS。
- GREEN：`python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py::test_remote_restart_blocks_showroom_media_bucket_inconsistency_before_backend_restart -q` -> PASS。
- GREEN：`python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py -q` -> PASS，14 passed。
- CHECK：`python -X utf8 -` 调用 `D:\Programs\Git\bin\bash.exe -n -c <bucket检查片段>` -> PASS，修复后的远端 Bash 片段可解析。
- GREEN：bug regression evidence validator -> PASS。
- GREEN：`git diff --check -- script/deploy/restart-int-ruoyi-remote.ps1 script/tests/test_runtime_control_scripts.py doc/tasks/20260604-remote-restart-bucket-sql-collation` -> PASS，仅有 Windows 行尾规范化提示。
- CLOSEOUT PREVIEW：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-remote-restart-bucket-sql-collation --mode preview` -> READY，delete `<none>`，blocked `<none>`，warnings `<none>`。

## Blockers

- 暂无。

## Cleanup Keep

- `doc/tasks/20260604-remote-restart-bucket-sql-collation/bug-regression-evidence.md`
