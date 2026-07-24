GREEN: experience-preflight -> PASS，已读取发布失败修复门禁；本轮只修 required SQL `SIGNAL ... MESSAGE_TEXT` 语法契约，不手工修改测试服、正式服或备份服数据库绕过发布。

BDD: DCC 视图矩阵缺失项较多时仍能抛出限长错误 -> Given DCC 视图矩阵 required SQL 聚合出较长缺失清单 When 存储过程执行到 `SIGNAL` Then 先将消息截断到 128 字符变量，再抛出该变量，SQL 语法必须可被 MySQL 接受。

BDD: SQL 不吞掉前置校验失败 -> Given 部门、角色或分类缺失 When seed SQL 执行 Then 仍使用 `SIGNAL SQLSTATE '45000'` 失败，不静默成功。

RED: pytest-message-text-contract -> FAIL，`python -m pytest script/tests/test_dcc_view_matrix_message_text_sql.py -q` 失败；当前 SQL 仍包含 `SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = LEFT(...)`，测试要求改为先 `SET @..._signal_message = LEFT(..., 128)` 再 `SIGNAL ... MESSAGE_TEXT = @..._signal_message`。
CHANGE: sql/mysql/20260613_dcc_file_view_matrix_seed.sql，将四个动态 SIGNAL 改为先 SET @..._signal_message = LEFT(..., 128)，再 SIGNAL MESSAGE_TEXT = @..._signal_message，避免 MySQL 存储过程拒绝 MESSAGE_TEXT 表达式。

GREEN: pytest-message-text-contract -> PASS，python -m pytest script/tests/test_dcc_view_matrix_message_text_sql.py -q -> 2 passed。

GREEN: migration-policy-gate -> PASS，python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql -> status=passed，migrationCount=235。

GREEN: tdd-compliance -> PASS，python -X utf8 tool/verify_tdd_compliance.py --repo D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-dir doc/tasks/20260701-fix-dcc-fvm-signal-expression-syntax --paths sql/mysql/20260613_dcc_file_view_matrix_seed.sql script/tests/test_dcc_view_matrix_message_text_sql.py doc/tasks/20260701-fix-dcc-fvm-signal-expression-syntax/task.md doc/tasks/20260701-fix-dcc-fvm-signal-expression-syntax/execution-log.md -> PASS。
