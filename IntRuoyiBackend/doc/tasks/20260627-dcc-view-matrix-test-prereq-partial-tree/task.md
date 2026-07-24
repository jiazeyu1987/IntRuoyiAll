# 20260627 DCC 矩阵测试租户前置组织树修复

## Task Goal

修复 `release-20260627-2147-head-full-v14` 发布测试服时，`20260624_dcc_view_matrix_test_tenant_prereq.sql` 在测试租户已有部分历史部门数据时触发 `VIEW_MATRIX_TEST_PREREQ_DEPT_RESOLUTION_FAILED` 的问题，确保 prerequisite 能幂等补齐完整测试组织树。

## Scope

- 后端发布 worktree：`D:\ProjectPackage\Int\release-worktrees\IntRuoyi-backend-20260627-8632c42`
- 影响 SQL：`sql/mysql/20260624_dcc_view_matrix_test_tenant_prereq.sql`
- 同步辅助 SQL：`script/dcc_view_matrix_test_tenant_prereq_20260624.sql`
- 影响测试：`script/tests/test_dcc_view_matrix_independent_seed_sql.py`、`script/dcc_view_matrix_test_tenant_prereq_sql_test.py`

## 经验门禁

- 发布 SQL 修复前必须以真实库只读证据核对当前数据形态。
- 不手工改测试租户数据绕过发布；必须通过幂等 SQL 修复根因。
- 缺少测试或门禁失败时不得重新构建发布包。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；修正组织树解析和测试契约，使 prerequisite 对部分历史残留数据幂等。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: 测试租户已有部分 DCC 历史部门 -> Given 测试租户仅已有 `质量体系中心` 及 `QA/QC/QMS` When 执行 DCC view matrix prerequisite Then SQL 应补齐 `瑛泰医疗/注册服务中心/注册部` 等缺失父链，并按父级 key 唯一解析所有计划部门。

## Milestones

1. 记录测试服真实失败与数据形态。`COMPLETED`
2. 增加失败回归测试。`COMPLETED`
3. 修复 SQL 幂等解析并同步辅助脚本。`COMPLETED`
4. 运行定向测试和发布迁移门禁。`COMPLETED`

## Expected Verification

- `python -X utf8 -m pytest script\tests\test_dcc_view_matrix_independent_seed_sql.py script\tests\test_dcc_view_matrix_excel_seed_sql.py -q`
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql`

## Verification Evidence

- `python -X utf8 -m pytest script\tests\test_dcc_view_matrix_independent_seed_sql.py script\tests\test_dcc_view_matrix_excel_seed_sql.py -q` -> PASS，`7 passed`。
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> PASS，`status=passed`、`migrationCount=218`。
- `python -X utf8 script\dcc_view_matrix_test_tenant_prereq_sql_test.py` -> PASS。
- 测试服 UTF-8 临时文件 dry-run：事务内解析 `tenant_id=122`、`dryrun_department_count=17`、`dryrun_user_count=16`、`dryrun_leader_count=8`，随后 `ROLLBACK`，未持久化测试数据。

## Current Status

COMPLETED
