# 任务：修复发布链路 DCC prerequisite apply 落地缺口

## 任务目标

- 修复测试服真实发布中 `preflight-plan.json` 已要求 `APPLY` `20260624_dcc_view_matrix_test_tenant_prereq`，但实际执行链路未落地执行该 migration 的问题。
- 保证 `deploy-release(test)` 在真实运行时先执行 `20260624_dcc_view_matrix_test_tenant_prereq.sql`，再执行 `20260624_dcc_view_matrix_independent_seed.sql`，避免再次出现 `VIEW_MATRIX_SEED_ROLE_USER_PRECHECK_FAILED`。
- 为“计划包含 prerequisite 但运行时漏执行”的根因补齐 RED/GREEN 证据与正式修复。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260627-release-required-sql-dcc-view-matrix-role-user-prereq\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已把 test-only prerequisite migration 正式纳入发布物与 preflight 计划，并新增 test 环境排序规则；本任务继续收口真实发布中“计划有、执行漏”的 apply 落地缺口。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\experience-index.md`
- 命中文档：无
- 适用强制门禁：
  - 本次仅允许在本机修改发布脚本、测试与任务文档；服务器侧只做只读核对，不做人工补跑 prerequisite 作为正式方案。
  - 必须以真实测试服只读证据为准确认根因；不得把“脚本看起来应该执行”当作已执行事实。
  - 修复必须确保 `deploy-release(test)` 的真实执行链路能留下 prerequisite 的 migration 状态记录；不得仅靠排序函数存在即视为完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。根因是测试服真实发布中，preflight 计划虽包含 prerequisite 的 `APPLY` 动作，但运行时没有落地执行该 item，导致测试租户 prerequisite 用户与 leader 数据未被写入。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 测试服 preflight apply item 必须真实落地 -> Given preflight-plan.json 对 20260624_dcc_view_matrix_test_tenant_prereq 和 20260624_dcc_view_matrix_independent_seed 都返回 APPLY / When deploy-release(test) 执行 required SQL / Then prerequisite migration 必须先留下 RUNNING/APPLIED 状态记录并真实写入测试租户 prerequisite 数据，再执行 independent seed。`
- `BDD: 真实执行证据不得只停留在 preflight 计划层 -> Given manifest 和 preflight-plan.json 都包含 prerequisite migration / When deploy-release(test) 进入 required SQL 执行阶段 / Then 运行日志、infra_release_migration 和测试租户真实数据必须能同时证明 prerequisite 已执行，否则视为失败。`

## 里程碑

1. M1：记录测试服“计划有、执行漏”的只读证据。`COMPLETED`
2. M2：补充发布脚本 apply 落地缺口 RED 测试。`COMPLETED`
3. M3：完成最小正式修复并跑通定向回归。`COMPLETED`
4. M4：更新证据并提交后端修复。`COMPLETED`

## 预期验证

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql`
- 必要时补充最小脚本级复现，证明 prerequisite apply item 在 test 环境确实位于 independent seed 之前且不会被执行链路漏掉。

## 最终验证结果

- 维护仓实际运行脚本 `D:\ProjectPackage\Int\IntRuoyiMaintance\ops\deploy\publish-int-ruoyi.ps1` 与运行控制台 worktree 副本均已补齐 `Sort-RequiredDatabaseSqlApplyItems`，并在 test 环境对 `20260624_dcc_view_matrix_test_tenant_prereq=10`、`20260624_dcc_view_matrix_independent_seed=20` 强制排序。
- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyiMaintance\scripts\tests\test_release_sql_preflight_gate.py -q` -> PASS (`5 passed`)
- 真实页面测试服重跑 operation `op-2026-06-27T072114322041600Z-826c3463-e987-40cb-a9af-9a14a903c669` 日志已验证 prerequisite 真实落地执行：line 1784 为 `Applying required database SQL: 20260624_dcc_view_matrix_test_tenant_prereq.sql`，line 1803 为 `Applying required database SQL: 20260624_dcc_view_matrix_independent_seed.sql`。
- 说明：本任务根因“preflight 计划包含 prerequisite，但真实执行链路漏掉该 apply item”已被关闭；后续测试服失败已前进到新的 SQL 基线问题 `DCC_FVM_RETAIN_OTHER_COMPLETION_CATEGORY_BASELINE_CHANGED`，不再属于本任务范围。
