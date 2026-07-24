# 任务：修复 DCC 文件视图矩阵 SQL MESSAGE_TEXT 过长阻塞正式发布

- Task ID: `20260701-fix-dcc-fvm-message-text-prod-release`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current Status: `in_progress`

## Task Goal

修复 `sql/mysql/20260613_dcc_file_view_matrix_seed.sql` 在正式服执行时因 `SIGNAL ... MESSAGE_TEXT` 超出 MySQL 条件项长度限制而失败的问题，确保该 required SQL 继续 fail-fast 暴露真实前置缺口，但错误文本被限制在 MySQL 可接受长度内。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`：任务开始读取索引，只打开命中的经验文档。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`：SQL、Markdown、命令输出必须显式 UTF-8。
- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`：required SQL 失败优先修 SQL 契约，不手工改库绕过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；仍保留 required SQL 的 fail-fast，只截断错误消息长度。
- `是否从根因和长期维护角度解决`：是；修复所有动态 `MESSAGE_TEXT` 超长风险点。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: 正式服缺大量 DCC 视图矩阵前置对象时错误文本仍可抛出 -> Given 缺失部门/角色/分类列表较长 When required SQL 触发 SIGNAL Then MESSAGE_TEXT 必须被截断到 MySQL 条件项允许长度内且继续返回明确错误前缀。

BDD: SQL 不吞掉 DCC 前置校验失败 -> Given DCC 文件视图矩阵前置缺失 When 执行 seed SQL Then 不应静默跳过或默认成功。

## Milestones

1. 建立任务台账并记录门禁。`completed`
2. 补静态 RED 检查，证明当前动态 MESSAGE_TEXT 未限长。`completed`
3. 修复 SQL 并跑 GREEN。`completed`
4. 提交后端修复，重新进入发布链路。`completed`

## Current Status

COMPLETED：SQL 已修复并通过静态 MESSAGE_TEXT 限长检查、pytest、TDD 合规检查和 migration policy gate；后续重新构建发布包并继续发布链路。

## Final Verification Result

- python -m pytest script/tests/test_dcc_view_matrix_message_text_sql.py -q -> PASS`r
- python -X utf8 tool/verify_tdd_compliance.py --repo ... --task-dir doc/tasks/20260701-fix-dcc-fvm-message-text-prod-release --paths sql/mysql/20260613_dcc_file_view_matrix_seed.sql script/tests/test_dcc_view_matrix_message_text_sql.py -> PASS`r
- python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql -> PASS`r
