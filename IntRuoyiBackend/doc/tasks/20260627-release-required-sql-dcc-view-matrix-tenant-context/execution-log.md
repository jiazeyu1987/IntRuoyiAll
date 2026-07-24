# 20260627 发布链路 DCC 查阅矩阵 seed 租户上下文缺失

BDD: 发布测试服执行 DCC 查阅矩阵 seed -> Given release package 包含 `20260624_dcc_view_matrix_independent_seed.sql` 且该 SQL 明确要求外部设置 `@dcc_view_matrix_seed_tenant_id` / When 发布脚本在测试环境执行 required SQL / Then 必须在同一 mysql 会话内先显式设置 `@dcc_view_matrix_seed_tenant_id := 122`，再执行该 SQL，避免真实页面 `deploy-release` 因缺少上下文失败。

- 根因摘要：测试服真实页面发布 operation `op-2026-06-27T040320387887200Z-cf7a52cd-2025-43d3-b485-f8b027c4f58f` 已推进到 `required-sql` 执行阶段，但 `publish-int-ruoyi.ps1` 当前仅把远端 SQL 文件直接管道进 mysql。`20260624_dcc_view_matrix_independent_seed.sql` 明确要求调用前先设置 `@dcc_view_matrix_seed_tenant_id`，因此真实发布在 line 736 失败并回写 `infra_release_migration=FAILED`。
- 预期行为：发布脚本对这类显式要求测试租户上下文的 migration，必须在同一 mysql 会话内先设置 `@dcc_view_matrix_seed_tenant_id := 122` 再执行 SQL；不得依赖人工登录服务器临时设置变量，也不得把 seed 默认落到 `tenant_id=1`。

- GREEN: experience-preflight -> PASS，本次仅在本机修改发布脚本、测试与任务文档，不直接执行服务器写入；真实页面测试服失败证据已记录在维护仓任务 `D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260625-head-full-release\execution-log.md`，可据此做本地 TDD 修复。
- RED: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL，新增“deploy-release 必须为 `20260624_dcc_view_matrix_independent_seed` 注入测试租户上下文”断言前，发布脚本不存在 `Get-RequiredDatabaseSqlSessionPreamble`，`Invoke-RequiredDatabaseSqlScripts` 也没有把 `SET @dcc_view_matrix_seed_tenant_id := 122;` 与目标 SQL 放进同一 mysql 会话。
- GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，`91 passed`
- GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_view_matrix_independent_seed_sql.py -q` -> PASS，`1 passed`
- 修复说明：发布脚本新增 `Get-RequiredDatabaseSqlSessionPreamble`，当前对 migration `20260624_dcc_view_matrix_independent_seed` 固定返回 `SET @dcc_view_matrix_seed_tenant_id := 122;`；`Invoke-RequiredDatabaseSqlScripts` 在存在 preamble 时通过同一远端 shell 管道先输出 preamble、再拼接远端 SQL 文件内容进入同一个 mysql 会话，从而满足 fail-fast seed 的调用契约而不依赖人工服务器操作。
- 问题追加：修复租户上下文后，维护仓重新轮询测试服真实发布 operation `op-2026-06-27T043350216700900Z-8388a3a5-8744-4d3c-9ab0-b2f34ac760f5`，日志显示 `20260624_dcc_view_matrix_independent_seed.sql` 已进入同一 mysql 会话执行，但在 line 737 触发 `VIEW_MATRIX_SEED_SUBJECT_PRECHECK_FAILED`，说明阻塞已从“缺少变量”前进到“测试租户主体映射与 seed 预期不一致”。
- GREEN: test-server-org-readonly-preflight -> PASS，使用只读 SSH + `docker exec intruoyi-mysql mysql ...` 查询测试服 `tenant_id=122` 当前真实组织树，确认：
  - `质量体系中心`、`研发创新中心`、`供应链中心`、`注册服务中心` 位于 `顶级部门` 下；
  - `市场营销中心`、`生产制造中心`、`检测中心` 位于 `瑛泰医疗` 下；
  - 不存在 `市场服务部`；
  - 不存在 `生产一车间`；
  - 不存在 `瑛泰医疗/注册服务中心` 这一层级。
- RED: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_view_matrix_independent_seed_sql.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_view_matrix_excel_seed_sql.py -q` -> FAIL，新增“DCC seed 必须匹配当前测试租户真实组织树、测试租户前置 SQL 不得继续创建旧部门层级/旧用户”断言后，复现：
  - `20260624_dcc_view_matrix_independent_seed.sql` 仍引用 `市场营销中心/市场服务部` 与 `瑛泰医疗/注册服务中心`
  - `script/dcc_view_matrix_test_tenant_prereq_20260624.sql` 仍计划创建 `市场服务部`、`生产一车间` 与 `dccmatrixworkshop`
- GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_view_matrix_independent_seed_sql.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_view_matrix_excel_seed_sql.py -q` -> PASS，`95 passed`
- GREEN: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\dcc_view_matrix_independent_seed_sql_test.py` -> PASS
- GREEN: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\dcc_view_matrix_test_tenant_prereq_sql_test.py` -> PASS
- 修复说明追加：
  - `20260624_dcc_view_matrix_independent_seed.sql` 已将“市场 / 业务+跟单及以上”主体从 `市场营销中心/市场服务部` 调整为测试服真实存在的 `瑛泰医疗/市场营销中心`。
  - `20260624_dcc_view_matrix_independent_seed.sql` 已将“市场 / 注册”主体从 `瑛泰医疗/注册服务中心` 调整为测试服真实存在的 `顶级部门/注册服务中心`。
  - `script/dcc_view_matrix_test_tenant_prereq_20260624.sql` 已同步移除过时的 `市场服务部`、`生产一车间`、`dccmatrixworkshop` 假设，并把 `dccmatrixmarket` 绑定到真实存在的 `市场营销中心`。
