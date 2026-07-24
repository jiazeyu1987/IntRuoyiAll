# Execution Log

BDD: 测试租户 prerequisite 补齐组织树后 independent seed 主体可解析 -> Given 测试服 `tenant_id=122` 已成功执行 `20260624_dcc_view_matrix_test_tenant_prereq.sql` 并输出 17 个部门、16 个用户、8 条负责人关系 When 执行 `20260624_dcc_view_matrix_independent_seed.sql` Then 所有 DEPT/ROLE 主体应唯一解析，不触发 `VIEW_MATRIX_SEED_SUBJECT_PRECHECK_FAILED`。

GREEN: experience-preflight -> PASS，已读取维护仓 `docs/experience-index.md` 命中的 `release-build-preflight-lessons.md`、`release-backup-restore.md`、`server-access.md` 与 `agent-memory/project-error-prevention.md`；本任务只读定位测试服 SQL blocker，不执行 mark-tested/prod/backup。

RED: runtime-control-deploy-test-20260627-v15 -> FAIL，operation `op-2026-06-27T144834229945300Z-0fe294cc-10f6-4e8e-8b3a-ba0b1d68e750` 发布 `release-20260627-2230-head-full-v15` 到测试服失败；前置 SQL `20260624_dcc_view_matrix_test_tenant_prereq.sql` 已成功输出 `prepared_department_count=17`、`prepared_user_count=16`、`prepared_leader_count=8`，下一条 `20260624_dcc_view_matrix_independent_seed.sql` 在测试租户执行时报 `ERROR 1644 (45000) at line 737: VIEW_MATRIX_SEED_SUBJECT_PRECHECK_FAILED`。

GREEN: test-server-readonly-subject-diagnosis -> PASS，使用 ASCII + `UNHEX(... USING utf8mb4)` 诊断 SQL 避免中文传输污染，只读定位唯一失败主体为 `注册 / ● / ALL_MEMBERS / 注册服务中心/注册部`，`resolved_count=2`；候选分别为旧历史链 `顶级部门/注册服务中心(id=910304)/注册部(id=910313)` 与 prerequisite 新补齐链 `顶级部门/瑛泰医疗(id=910305)/注册服务中心(id=910331)/注册部(id=910332)`。根因：independent seed 只按父部门名称 `注册服务中心` 解析，未使用完整父链消除同名父级二义性。

RED: python -X utf8 -m pytest script\tests\test_dcc_view_matrix_independent_seed_sql.py -q -> FAIL，预期原因：新增测试要求 `注册` 主体使用完整路径 `瑛泰医疗/注册服务中心/注册部`，并要求 SQL 通过完整部门路径解析父级；当前 SQL 仍使用两段路径且只按 `parent_dept.name = subject.parent_dept_name`。

GREEN: seed-full-path-resolution-fix -> PASS，`20260624_dcc_view_matrix_independent_seed.sql` 为 `tmp_dcc_view_matrix_seed_subject` 增加 `parent_path_name` 与 `grand_parent_path_name`，从 `subject_lookup_name` 自动拆解三段路径；两段路径保持原父级解析，三段路径增加 `parent_dept.parent_id = grand_dept.id` 约束，`注册` 主体改为 `瑛泰医疗/注册服务中心/注册部`。

GREEN: targeted-regression-tests -> PASS，`python -X utf8 -m pytest script\tests\test_dcc_view_matrix_independent_seed_sql.py script\tests\test_dcc_view_matrix_excel_seed_sql.py -q` -> `8 passed`。

GREEN: release-migration-policy-gate -> PASS，`python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> `status=passed`、`migrationCount=218`，修复后的 `20260624_dcc_view_matrix_independent_seed.sql` sha256=`cf1e9cbb31f82f0ea73665d44e22efded499a3ea0830b452637df44d96f68244`。

GREEN: test-server-independent-seed-rollback-dryrun -> PASS，使用 UTF-8 LF 临时 SQL 上传测试服 `/tmp/runtime-dcc-independent-seed-dryrun-rollback.sql`，显式设置 `@dcc_view_matrix_seed_tenant_id := 122` 后执行；事务内输出 `dryrun_category_count=59`、`dryrun_view_matrix_rule_count=243`、`dryrun_generated_role_count=5`、`dryrun_generated_role_user_count=8`，随后 `ROLLBACK`，未触发 `VIEW_MATRIX_SEED_SUBJECT_PRECHECK_FAILED`。
