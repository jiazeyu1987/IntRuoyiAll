# Execution Log

BDD: 测试租户已有部分 DCC 历史部门 -> Given 测试租户仅已有 `质量体系中心` 及 `QA/QC/QMS` When 执行 DCC view matrix prerequisite Then SQL 应补齐 `瑛泰医疗/注册服务中心/注册部` 等缺失父链，并按父级 key 唯一解析所有计划部门。

GREEN: experience-preflight -> PASS，已在主发布任务读取 `release-backup-restore.md`、`server-access.md` 与数据库变更门禁；本任务仅修改发布 SQL 与测试，不手工改测试服数据。

RED: runtime-control-deploy-test-20260627-v14 -> FAIL，真实运行控制台测试服部署 operation `op-2026-06-27T140529058593900Z-da047d1c-947c-4985-846f-f9aad9396d1a` 在 `20260624_dcc_view_matrix_test_tenant_prereq.sql` 第 297 行失败：`VIEW_MATRIX_TEST_PREREQ_DEPT_RESOLUTION_FAILED`。

GREEN: test-server-readonly-current-dept-shape -> PASS，只读查询测试服 `tenant_id=122` 当前相关组织数据：根 `顶级部门(id=111)` 下已有 `质量体系中心(id=910303)`，其下已有 `QA/QC/QMS`；未查到 prerequisite 当前需要补齐的 `瑛泰医疗`、`注册服务中心`、`注册部` 等完整父链。

RED: regression-contract-before-fix -> FAIL，预期原因：旧 SQL 只通过父部门名称解析计划节点，无法在测试租户存在部分历史组织树或同名父级风险时按计划父级唯一定位，真实发布已复现 `VIEW_MATRIX_TEST_PREREQ_DEPT_RESOLUTION_FAILED`。

GREEN: sql-parent-key-resolution-fix -> PASS，`20260624_dcc_view_matrix_test_tenant_prereq.sql` 与辅助脚本改为复制 `tmp_dcc_view_matrix_test_parent_plan` / `tmp_dcc_view_matrix_test_grand_plan`，按 `parent_key` 和具体 `parent_id` 解析父链；`regsvc` 与正式组织拓扑保持在 `ytyl/瑛泰医疗` 下。

GREEN: targeted-regression-tests -> PASS，`python -X utf8 -m pytest script\tests\test_dcc_view_matrix_independent_seed_sql.py script\tests\test_dcc_view_matrix_excel_seed_sql.py -q` -> `7 passed`。

GREEN: release-migration-policy-gate -> PASS，`python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> `status=passed`、`migrationCount=218`。

GREEN: auxiliary-sql-contract -> PASS，`python -X utf8 script\dcc_view_matrix_test_tenant_prereq_sql_test.py` -> `dcc view matrix test tenant prerequisite SQL contract PASS`。

GREEN: test-server-utf8-rollback-dryrun -> PASS，使用 UTF-8 LF 临时 SQL 经 `scp` 上传测试服 `/tmp/runtime-dcc-prereq-dryrun-rollback.sql`，通过 `docker exec -i -e MYSQL_PWD=<redacted> intruoyi-mysql mysql --default-character-set=utf8mb4 -uroot ruoyi-vue-pro` 执行；事务内输出 `tenant_id=122`、`dryrun_department_count=17`、`dryrun_user_count=16`、`dryrun_leader_count=8`，随后 `ROLLBACK`，未再触发 `VIEW_MATRIX_TEST_PREREQ_*` 或 `Can't reopen table`。
