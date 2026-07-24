GREEN: experience-preflight -> PASS，已读取 IntRuoyi 经验索引、PowerShell 编码门禁与发布失败修复门禁；本轮只修 required SQL MESSAGE_TEXT 过长契约，不手工修改正式库绕过发布。

BDD: 正式服缺大量 DCC 视图矩阵前置对象时错误文本仍可抛出 -> Given 缺失部门/角色/分类列表较长 When required SQL 触发 SIGNAL Then MESSAGE_TEXT 必须被截断到 MySQL 条件项允许长度内且继续返回明确错误前缀。

BDD: SQL 不吞掉 DCC 前置校验失败 -> Given DCC 文件视图矩阵前置缺失 When 执行 seed SQL Then 不应静默跳过或默认成功。

RED: static-message-text-contract -> FAIL，旧版 20260613_dcc_file_view_matrix_seed.sql 的动态 SIGNAL MESSAGE_TEXT 未使用 LEFT(..., 128) 限长，正式服大量缺失项时触发 MySQL ERROR 1648。

CHANGE: sql/mysql/20260613_dcc_file_view_matrix_seed.sql，将 dept/role/ambiguous_category/missing_category 四个动态 SIGNAL MESSAGE_TEXT 改为 LEFT(..., 128)，保留 fail-fast。

GREEN: static-message-text-contract -> PASS，已确认四个动态 MESSAGE_TEXT 均使用 LEFT(..., 128) 限长。

GREEN: tdd-compliance -> PASS，python -X utf8 tool\verify_tdd_compliance.py --repo D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-dir doc/tasks/20260701-fix-dcc-fvm-message-text-prod-release --paths sql/mysql/20260613_dcc_file_view_matrix_seed.sql script/tests/test_release_migration_policy.py -> PASS。

GREEN: migration-policy-gate -> PASS，python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql -> status=passed，migrationCount=235。

CHANGE: script/tests/test_dcc_view_matrix_message_text_sql.py，新增 DCC FVM 动态 SIGNAL MESSAGE_TEXT 必须 LEFT(..., 128) 限长且禁止回退为未限长变量的静态回归。

GREEN: pytest-message-text-contract -> PASS，python -m pytest script/tests/test_dcc_view_matrix_message_text_sql.py -q -> PASS。

GREEN: final-verify-before-commit -> PASS，pytest、TDD 合规检查与 migration policy gate 均通过，任务状态更新为 completed。
