# 20260627 DCC 矩阵测试服主体预检修复

## Task Goal

修复 `release-20260627-2230-head-full-v15` 发布测试服时，`20260624_dcc_view_matrix_independent_seed.sql` 在测试租户 `tenant_id=122` 执行后触发 `VIEW_MATRIX_SEED_SUBJECT_PRECHECK_FAILED` 的问题，确保测试租户 prerequisite 成功补齐组织树后，independent seed 能按同一真实父链解析主体。

## Scope

- 后端发布 worktree：`D:\ProjectPackage\Int\release-worktrees\IntRuoyi-backend-20260627-8632c42`
- 影响 SQL：`sql/mysql/20260624_dcc_view_matrix_independent_seed.sql`
- 影响测试：`script/tests/test_dcc_view_matrix_independent_seed_sql.py`

## 经验门禁

- 发布失败必须记录 operation、releaseTag、失败 SQL 和真实日志，不得拼接多轮发布结果。
- 修复前先用真实测试服只读 SQL 查清楚缺失主体，不手工改测试租户数据绕过。
- `code-only` 仍会执行 required SQL；测试服发布成功前不得 mark-tested、promote-prod 或 promote-backup。
- 修复后必须重新构建新 releaseTag，失败候选包只能作为排障证据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；seed 改为从完整 subject path 解析父级与祖父级，避免测试租户历史同名父部门导致二义性。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: 测试租户 prerequisite 补齐组织树后 independent seed 主体可解析 -> Given 测试服 `tenant_id=122` 已成功执行 `20260624_dcc_view_matrix_test_tenant_prereq.sql` 并输出 17 个部门、16 个用户、8 条负责人关系 When 执行 `20260624_dcc_view_matrix_independent_seed.sql` Then 所有 DEPT/ROLE 主体应唯一解析，不触发 `VIEW_MATRIX_SEED_SUBJECT_PRECHECK_FAILED`。

## Milestones

1. 记录 v15 测试服失败证据。`COMPLETED`
2. 只读定位 missing subject 明细。`COMPLETED`
3. 增加失败回归测试。`COMPLETED`
4. 修复 SQL 解析并通过本地门禁。`COMPLETED`
5. 提交修复并重新构建新候选。`PENDING`

## Expected Verification

- `python -X utf8 -m pytest script\tests\test_dcc_view_matrix_independent_seed_sql.py script\tests\test_dcc_view_matrix_excel_seed_sql.py -q`
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql`
- 测试服 UTF-8 dry-run 或真实发布 required SQL 不再触发 `VIEW_MATRIX_SEED_SUBJECT_PRECHECK_FAILED`。

## Verification Evidence

- `python -X utf8 -m pytest script\tests\test_dcc_view_matrix_independent_seed_sql.py script\tests\test_dcc_view_matrix_excel_seed_sql.py -q` -> PASS，`8 passed`。
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> PASS，`status=passed`、`migrationCount=218`。
- 测试服 UTF-8 rollback dry-run -> PASS，输出 `dryrun_category_count=59`、`dryrun_view_matrix_rule_count=243`、`dryrun_generated_role_count=5`、`dryrun_generated_role_user_count=8`。

## Current Status

IN_PROGRESS：源码与测试修复已完成，待提交并重新构建新候选包。
