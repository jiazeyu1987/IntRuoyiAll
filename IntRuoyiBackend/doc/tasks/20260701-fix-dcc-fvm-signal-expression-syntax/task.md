# 任务：修复 DCC 视图矩阵 SQL SIGNAL 表达式语法

- Task ID: `20260701-fix-dcc-fvm-signal-expression-syntax`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current Status: `in_progress`

## Task Goal

修复 `sql/mysql/20260613_dcc_file_view_matrix_seed.sql` 中 `SIGNAL ... MESSAGE_TEXT = LEFT(...)` 在 MySQL 存储过程内不被接受的问题，保持错误消息 128 字符限长，并继续 fail-fast 暴露真实 DCC 视图矩阵前置缺口。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`：required SQL 失败优先修 SQL 契约，不手工改库绕过。
- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-agent-checklist.md`：发布失败先查 migration/manifest/脚本契约，再查环境。
- PowerShell/中文文件读写必须显式 UTF-8。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；仍保留 `SIGNAL SQLSTATE '45000'`。
- `是否从根因和长期维护角度解决`：是；改为先将截断后的消息写入变量，再把变量交给 `MESSAGE_TEXT`。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: DCC 视图矩阵缺失项较多时仍能抛出限长错误 -> Given DCC 视图矩阵 required SQL 聚合出较长缺失清单 When 存储过程执行到 `SIGNAL` Then 先将消息截断到 128 字符变量，再抛出该变量，SQL 语法必须可被 MySQL 接受。

BDD: SQL 不吞掉前置校验失败 -> Given 部门、角色或分类缺失 When seed SQL 执行 Then 仍使用 `SIGNAL SQLSTATE '45000'` 失败，不静默成功。

## Milestones

1. 建立任务台账并记录经验门禁。`completed`
2. 更新 RED 静态测试，证明 direct `LEFT(...)` 写入 `MESSAGE_TEXT` 不可接受。`completed`
3. 修复 SQL 并跑 GREEN。`completed`
4. 提交后端修复，重新构建发布包并继续发布链路。`in_progress`

## Expected Verification

- `python -m pytest script/tests/test_dcc_view_matrix_message_text_sql.py -q` -> PASS
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` -> PASS
- `python -X utf8 tool/verify_tdd_compliance.py --repo ... --task-dir doc/tasks/20260701-fix-dcc-fvm-signal-expression-syntax --paths ...` -> PASS

## Current Status

IN_PROGRESS：SQL 语法修复已完成，pytest、migration policy gate 与 TDD 合规检查均通过，正在提交后端修复。
## Verification Evidence

- python -m pytest script/tests/test_dcc_view_matrix_message_text_sql.py -q -> PASS，2 passed。
- python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql -> PASS，status=passed，migrationCount=235。
- python -X utf8 tool/verify_tdd_compliance.py --repo D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-dir doc/tasks/20260701-fix-dcc-fvm-signal-expression-syntax --paths sql/mysql/20260613_dcc_file_view_matrix_seed.sql script/tests/test_dcc_view_matrix_message_text_sql.py doc/tasks/20260701-fix-dcc-fvm-signal-expression-syntax/task.md doc/tasks/20260701-fix-dcc-fvm-signal-expression-syntax/execution-log.md -> PASS。
