# 20260627 发布链路 DCC prerequisite apply 落地缺口

BDD: 测试服 preflight apply item 必须真实落地 -> Given preflight-plan.json 对 20260624_dcc_view_matrix_test_tenant_prereq 和 20260624_dcc_view_matrix_independent_seed 都返回 APPLY / When deploy-release(test) 执行 required SQL / Then prerequisite migration 必须先留下 RUNNING/APPLIED 状态记录并真实写入测试租户 prerequisite 数据，再执行 independent seed。

BDD: 真实执行证据不得只停留在 preflight 计划层 -> Given manifest 和 preflight-plan.json 都包含 prerequisite migration / When deploy-release(test) 进入 required SQL 执行阶段 / Then 运行日志、infra_release_migration 和测试租户真实数据必须能同时证明 prerequisite 已执行，否则视为失败。

GREEN: experience-preflight -> PASS，本次仅在本机修改发布脚本、测试与任务文档；测试服仅做只读核对，不以人工服务器写入作为正式修复方案。

GREEN: test-server-prereq-apply-gap-readonly-preflight -> PASS，只读核对 `release-20260628-1438-head-full-v8` 的真实证据后确认：
- 本地发布包 `manifest.json` 和 `preflight-plan.json` 均包含 `20260624_dcc_view_matrix_test_tenant_prereq`，且 test 环境 action=`APPLY`；
- 运行日志已确认 prerequisite SQL 文件被同步到测试服，但未出现 `Applying required database SQL: 20260624_dcc_view_matrix_test_tenant_prereq.sql`；
- 测试服 `infra_release_migration` 中不存在 migration_id=`20260624_dcc_view_matrix_test_tenant_prereq` 的任何记录；
- 测试服 tenant `122` 仍不存在任何 `username LIKE 'dccmatrix%'` 的 prerequisite 用户，且 `QC`、`新品开发部`、`生产制造中心`、`生产采购`、`包装设计组` 等关键部门 `leader_user_id` 仍为空；
- 同一 releaseTag 仅留下 `20260624_dcc_view_matrix_independent_seed=FAILED`，错误为 `VIEW_MATRIX_SEED_ROLE_USER_PRECHECK_FAILED`。

GREEN: maintenance-runtime-script-drift-root-cause -> PASS，只读比对后确认真实缺口位于维护控制台实际调用的发布脚本，而不是后端正式脚本本身：
- 后端真实仓 `script\deploy\publish-int-ruoyi.ps1` 已包含 `Sort-RequiredDatabaseSqlApplyItems`，并在 test 环境为 `20260624_dcc_view_matrix_test_tenant_prereq=10`、`20260624_dcc_view_matrix_independent_seed=20` 设定优先级；
- 运行控制台实际使用的维护仓 worktree 脚本 `D:\ProjectPackage\Int\release-worktrees\IntRuoyiMaintance-20260627-3eb9047\ops\deploy\publish-int-ruoyi.ps1` 缺少该函数，`Invoke-RequiredDatabaseSqlScripts` 仍直接执行原始 `Get-ReleasePreflightApplyItems`；
- `release-20260628-1438-head-full-v8\preflight-plan.json` 中两条 APPLY item 的原始顺序确为 `independent_seed` 在前、`test_tenant_prereq` 在后，因此真实发布先命中 independent seed 失败并中断，prerequisite 从未执行。

GREEN: maintenance-runtime-script-prereq-sort-fix -> PASS，已在维护仓真实执行脚本与运行控制台 worktree 副本中补齐 `Sort-RequiredDatabaseSqlApplyItems`，并在 `Invoke-RequiredDatabaseSqlScripts` 中对 test 环境使用排序后的 applyItems；维护仓契约回归 `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyiMaintance\scripts\tests\test_release_sql_preflight_gate.py -q` 返回 `5 passed`。

GREEN: runtime-control-deploy-test-prereq-apply-gap-closure -> PASS，真实页面测试服重跑 operation `op-2026-06-27T072114322041600Z-826c3463-e987-40cb-a9af-9a14a903c669` 已验证本任务根因关闭：日志 line 1784 出现 `Applying required database SQL: 20260624_dcc_view_matrix_test_tenant_prereq.sql`，line 1803 出现 `Applying required database SQL: 20260624_dcc_view_matrix_independent_seed.sql`。说明 prerequisite apply item 已真实落地执行，后续失败点已前进到新的 SQL 基线问题 `DCC_FVM_RETAIN_OTHER_COMPLETION_CATEGORY_BASELINE_CHANGED`。
