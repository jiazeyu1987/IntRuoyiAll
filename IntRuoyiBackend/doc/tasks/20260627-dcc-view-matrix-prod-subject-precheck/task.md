# 20260627 DCC 矩阵正式服主体预检修复

## Task Goal

修复正式服发布 `release-20260628-2015-head-full-v13` 在执行 `20260624_dcc_view_matrix_independent_seed.sql` 时触发的 `VIEW_MATRIX_SEED_SUBJECT_PRECHECK_FAILED`，确保 DCC 审核矩阵 seed 的组织主体映射同时匹配测试租户 prerequisite 和正式服真实组织树。

## Scope

- 后端发布 worktree：`D:\ProjectPackage\Int\release-worktrees\IntRuoyi-backend-20260627-8632c42`
- 影响 SQL：`sql/mysql/20260624_dcc_view_matrix_independent_seed.sql`
- 影响测试：`script/tests/test_dcc_view_matrix_independent_seed_sql.py`
- 维护仓脚本修复另由主发布任务记录：DCC seed tenant context 已按环境区分。

## 经验门禁

- 发布/数据库变更不得用 mock、默认成功或静默跳过掩盖失败。
- 编写或修复 SQL 前，以当前真实库只读查询为准核对表名字段和真实组织数据。
- 正式服写入默认禁止；本任务只做源码修复和只读核对，不直接改正式库业务数据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；修正 seed 对真实业务组织层级的映射，不补虚构正式服部门。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: DCC 矩阵注册主体映射匹配真实组织树 -> Given 正式服 `tenant_id=1` 中 `注册服务中心` 位于 `瑛泰医疗` 下 When 发布执行 `20260624_dcc_view_matrix_independent_seed.sql` Then `市场 / 注册` 主体应解析到 `瑛泰医疗/注册服务中心`，不得要求不存在的 `顶级部门/注册服务中心`。

## Milestones

1. 记录正式服失败证据和真实组织层级。`COMPLETED`
2. 先用测试复现旧映射错误。`COMPLETED`
3. 修正 seed 与测试租户 prerequisite 映射并通过回归。`COMPLETED`
4. 将修复提交到后端发布输入，返回维护控制台重新出包。`COMPLETED`

## Expected Verification

- `python -X utf8 -m pytest script\tests\test_dcc_view_matrix_independent_seed_sql.py -q`
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql`
- 正式服只读临时表预检显示 DCC view matrix subject missing_or_duplicate 为 0。

## Verification Evidence

- `python -X utf8 -m pytest script\tests\test_dcc_view_matrix_independent_seed_sql.py -q`：`4 passed`。
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql`：`status=passed`，`migrationCount=218`。
- `prod-readonly-fixed-subject-preflight`：正式服只读临时表预检 `missing_or_duplicate=0`，`checked_dept_subjects=13`。

## Current Status

COMPLETED：源码 SQL 与测试已修复，本地门禁和正式服只读预检通过；后端修复已提交为 `c17ef45d2c`，可作为下一次维护控制台构建发布输入。
