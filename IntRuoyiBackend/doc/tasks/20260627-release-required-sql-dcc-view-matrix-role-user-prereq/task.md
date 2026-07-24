# 任务：修复发布链路 DCC 查阅矩阵角色用户前置缺失

## 任务目标

- 修复测试服真实发布在 `20260624_dcc_view_matrix_independent_seed.sql` 阶段触发 `VIEW_MATRIX_SEED_ROLE_USER_PRECHECK_FAILED` 的问题。
- 将测试租户 `tenant_id=122` 所需的 DCC 查阅矩阵 prerequisite 数据纳入正式发布链路，而不是依赖人工预热或手工改库。
- 为新的 prerequisite 发布 SQL、发布排序约束与相关契约测试补齐 RED/GREEN 证据。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260627-release-required-sql-dcc-view-matrix-tenant-context\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已解决租户上下文缺失与旧组织树映射问题；本任务继续收口新暴露出的角色用户前置缺失问题。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\experience-index.md`
- 命中文档：无
- 适用强制门禁：
  - 本次仅修改本机 SQL、脚本契约测试与任务文档，不手工写服务器数据库作为正式修复方案。
  - 修复必须进入正式发布链路；不得通过人工先执行一次 prerequisite SQL、再让发布继续，作为长期方案替代。
  - 测试租户 prerequisite 只能作用于 `tenant_id=122`，不得把受保护租户或其他未知租户当作默认目标。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。根因是测试服真实发布链路只执行了 independent seed，却没有把其明确依赖的测试租户 prerequisite 数据纳入正式 required-sql 输入，导致关键部门负责人和 prerequisite 用户缺失。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 测试服发布先准备 DCC 测试租户 prerequisite -> Given tenant_id=122 的真实部门存在但关键 leader_user_id 和 dccmatrix* prerequisite 用户缺失 / When deploy-release(test) 执行 DCC 查阅矩阵 required SQL / Then 发布链路必须先执行正式的测试租户 prerequisite SQL，再执行 independent seed，避免角色用户预检查失败。`
- `BDD: prerequisite SQL 不得进入非测试环境写入 -> Given 同一发布包会继续用于 backup/prod / When preflight plan 针对 backup/prod 生成 required SQL 执行列表 / Then 测试租户 prerequisite SQL 必须只允许 test 环境执行，不得在 backup/prod 误写 tenant_id=122。`

## 里程碑

1. M1：记录真实测试服 role-user blocker 与只读数据证据。`COMPLETED`
2. M2：补充 prerequisite 发布契约 RED 测试。`COMPLETED`
3. M3：实现正式 prerequisite SQL 并跑通定向回归。`COMPLETED`
4. M4：更新任务证据并提交后端修复。`COMPLETED`

## 预期验证

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_view_matrix_independent_seed_sql.py -q`
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\dcc_view_matrix_test_tenant_prereq_sql_test.py`
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql`

## 最终验证结果

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_view_matrix_independent_seed_sql.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_view_matrix_excel_seed_sql.py -q` -> PASS (`98 passed`)
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql` -> PASS (`status=passed`, `migrationCount=218`)
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\dcc_view_matrix_independent_seed_sql_test.py` -> PASS
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\dcc_view_matrix_test_tenant_prereq_sql_test.py` -> PASS
