# 20260627 发布链路 DCC FVM retain-other completion 基线失配

BDD: retain-other completion 必须作用于已被 independent seed 准备好的同一租户 -> Given deploy-release(test) 已为目标租户执行 DCC FVM independent seed / When retain-other completion 继续执行 / Then 它必须针对同一真实目标租户验证并补全 DCC FVM 分类与审批路线，而不是硬编码落在另一套旧基线租户上。

BDD: 真实分类基线漂移必须显式暴露并用正式方案修复 -> Given 测试服 tenant_id=1 当前真实数据为 active_total=81、dcc_fvm_count=32、other_count=1、intauth_count=48 / When retain-other completion 进入首道基线校验 / Then 契约测试必须先复现这组旧假设失效，再用正式修复统一 SQL 设计。

GREEN: experience-preflight -> PASS，本次仅在本机修改 SQL、测试与任务文档；测试服只做只读核对，不以人工补库作为正式方案。

GREEN: test-server-fvm-retain-other-readonly-preflight -> PASS，只读核对测试服真实发布失败链路与数据库基线后确认：
- `op-2026-06-27T072114322041600Z-826c3463-e987-40cb-a9af-9a14a903c669` 已真实执行 `20260624_dcc_view_matrix_test_tenant_prereq.sql` 与 `20260624_dcc_view_matrix_independent_seed.sql`；
- 随后在 `20260625_dcc_fvm_matrix_retain_other_completion.sql` 失败，错误为 `DCC_FVM_RETAIN_OTHER_COMPLETION_CATEGORY_BASELINE_CHANGED`；
- `20260625` SQL 硬编码 `SET @dcc_fvm_completion_tenant_id := 1`，并首道校验要求 `active_total=60`、`dcc_fvm_count=59`、`other_count=1`；
- 测试服 `tenant_id=1` 真实只读结果为 `active_total=81`、`dcc_fvm_count=32`、`other_count=1`、`intauth_count=48`；
- `20260624_dcc_view_matrix_independent_seed.sql` 的分类插入、角色、查阅规则与审批路由都基于 `@dcc_view_matrix_seed_tenant_id` 执行，真实发布时目标租户并非硬编码 `1`。

GREEN: root-cause-hypothesis-20260627 -> PASS，当前根因已收敛为 `20260624` 与 `20260625` 两条 required SQL 在目标租户/前置基线上的设计失配，而不是维护仓 prerequisite 排序问题，也不是测试服 prerequisite 未执行。

RED: python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_fvm_matrix_retain_other_completion_sql.py -q -> FAIL，新增“`20260625_dcc_fvm_matrix_retain_other_completion.sql` 不允许在 test 环境执行”契约后，断言复现 migration 头仍为 `allowedEnvironments=test,backup,prod`。

GREEN: python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_fvm_matrix_retain_other_completion_sql.py -q -> PASS，`5 passed`；已将 `20260625_dcc_fvm_matrix_retain_other_completion.sql` 的 migration 元数据收敛为 `allowedEnvironments=backup,prod`，并增加契约锁定其 `tenant_id=1` 本地范围不得进入 test 发布链路。

GREEN: python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql -> PASS，返回 `status=passed`、`migrationCount=218`；说明本次最小修复未破坏整体 migration 元数据契约。

GREEN: prod-fvm-retain-other-readonly-preflight-20260628 -> PASS，v16 正式服重试已越过 `20260624_dcc_view_matrix_independent_seed.sql`，但在 `20260625_dcc_fvm_matrix_retain_other_completion.sql` 失败；正式服 tenant_id=1 只读分类分布为 `active_total=207`、`dcc_fvm_count=59`、`other_count=1`、`intauth_count=48`、`non_fvm_non_other_count=147`。结论：目标 DCC_FVM 集合与 OTHER 模板满足契约，失败仅由 SQL 硬编码全租户 active 分类总数 `60` 导致。

RED: python -X utf8 -m pytest script\tests\test_dcc_fvm_matrix_retain_other_completion_sql.py -q -> FAIL，新增 `test_completion_sql_does_not_pin_total_active_category_count` 后首次运行返回 `1 failed, 5 passed`，复现 SQL 仍包含 `v_active_total <> 60`。

GREEN: dcc-fvm-retain-other-total-active-baseline-fix -> PASS，已将 `20260625_dcc_fvm_matrix_retain_other_completion.sql` 首道基线校验从 `v_active_total <> 60 OR v_dcc_fvm_count <> 59` 收敛为 `v_dcc_fvm_count <> 59`，保留 OTHER 模板唯一性、view matrix 59/243、review route 59 与节点完整性门禁；不引入 fallback，不静默跳过。

GREEN: python -X utf8 -m pytest script\tests\test_dcc_fvm_matrix_retain_other_completion_sql.py -q -> PASS，`6 passed`。

GREEN: python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql -> PASS，返回 `status=passed`、`migrationCount=218`。
