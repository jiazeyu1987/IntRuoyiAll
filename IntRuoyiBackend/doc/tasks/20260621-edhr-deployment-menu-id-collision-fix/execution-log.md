# 执行日志: 20260621-edhr-deployment-menu-id-collision-fix

BDD: deployment 菜单切片不得复用已占用 eDHR 菜单号段 -> Given 真实测试库中 900296-900299 已被 eDHR统一变更占用且 900220 是 eDHR父菜单 / When 执行 20260619_mes_edhr_deployment_license_interface.sql / Then 迁移必须使用独立且未冲突的 deployment 菜单 ID 段，而不是覆盖或误判现有统一变更菜单。
BDD: 错误半执行后的 deployment 残留菜单也必须被正式迁移吸收 -> Given 某环境曾因旧脚本失败而留下错误的 deployment permission/path 菜单残留 / When 执行修复后的 20260619_mes_edhr_deployment_license_interface.sql / Then 迁移必须正式清理或收敛旧残留，并为测试租户合并正确的 deployment 菜单和权限。

- GREEN: task-created -> PASS，已创建任务目录并补齐 `task.md` 的目标、边界、经验门禁、设计约束检查、BDD 场景与当前状态，可进入只读核对与 RED。
- GREEN: maintenance-failure-read -> PASS，只读核对维护仓 `op-2026-06-21T145341175255100Z-0de12b04-2650-49e2-a088-47dfa4f2b7ed` 日志后确认：真实页面 deploy-test 失败于 `20260619_mes_edhr_deployment_license_interface.sql`，错误为 `Invalid eDHR deployment page menu definition; cannot merge tenant package menu_ids`。
- GREEN: test-db-menu-audit -> PASS，只读查询测试服真实库 `system_menu` 后确认：`900220` 是 eDHR父菜单 `eDHR批处理`，`900296-900299` 已被 `eDHR统一变更` 占用，旧失败脚本还留下了错误的 `900300 / mes:pro-edhr-deployment:precheck` 残留按钮。说明根因是 SQL 选择了冲突菜单号段，而不是测试库偶发脏数据。
- RED: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_edhr_deployment_schema_sql.py -q` -> FAIL，新增契约 `test_deployment_schema_uses_dedicated_menu_ids_and_cleans_legacy_rows` 后，旧 SQL 仍然写死 `900296-900300` 作为 deployment 菜单号段，且缺少对旧 `mes:pro-edhr-deployment:*` / `/mes/pro/feedback/edhr-deployment` 残留菜单的正式清理逻辑。
- GREEN: deployment-menu-id-fix -> PASS，已将 `20260619_mes_edhr_deployment_license_interface.sql` 的 deployment 菜单号段改为未占用的 `900315-900319`，并补入 `tmp_mes_edhr_deployment_legacy_menu_ids` 正式清理逻辑，用于吸收旧失败脚本留下的冲突 deployment 菜单残留而不影响新号段幂等重跑。
- GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_edhr_deployment_schema_sql.py -q` -> PASS，5 passed。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> PASS，返回 `status=passed`。
- 当前结论：业务仓已按严格 TDD 收口 `20260619_mes_edhr_deployment_license_interface.sql` 的菜单 ID 冲突根因。由于发布包输入已变化，维护仓必须重新构建新的 `releaseTag`，再从真实页面重走 `build-release -> deploy-test -> mark-tested -> prod -> backup`。
- RED: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_edhr_deployment_schema_sql.py -q` -> FAIL，新增契约 `test_deployment_schema_cleans_legacy_rows_before_inserting_new_menu_ids` 稳定命中旧顺序：`CREATE TEMPORARY TABLE \`tmp_mes_edhr_deployment_legacy_menu_ids\` AS` 仍位于 `SELECT 900315, 'eDHR部署授权接口'` 之后，说明当前 SQL 仍然是“先插入新菜单、后清理旧残留”，会把 `900319` 这种被旧权限行挡住的菜单插入挡掉后再删除旧行，最终留下不完整菜单集。
- GREEN: test-db-menu-audit-v2 -> PASS，只读查询测试服真实库 `system_menu` 后确认当前 deployment 菜单只剩 `900315-900318` 四条，新 `900319` 不存在，而旧残留 `900300` 也已被物理删除；该现场与真实页面 `deploy-test(v3)` 日志中的 `Missing eDHR deployment system_menu rows` 完全一致，证明根因就是旧 precheck 残留先挡住了 `900319` 插入，随后又被后置清理删掉。
- GREEN: deployment-menu-cleanup-order-fix -> PASS，已将 `tmp_mes_edhr_deployment_legacy_menu_ids` 的清理块前移到 `900315-900319` 插入之前，保证旧 `/mes/pro/feedback/edhr-deployment` 路径与 `mes:pro-edhr-deployment:*` 权限残留先清理、再插入新的 deployment 页面与按钮菜单，不再留下 4/5 菜单的半完成状态。
- GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_edhr_deployment_schema_sql.py -q` -> PASS，6 passed。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> PASS，返回 `status=passed`，`20260619_mes_edhr_deployment_license_interface.sql` 新哈希已纳入发布契约。
- GREEN: maintenance-backfill-v2 -> PASS，维护仓主任务 `20260621-frontend-release-full-flow-main-merge-rerun` 已记录新的 `deploy-test(v2)` 页面失败与本次 cleanup-order 根因；下一步必须用新的 `releaseTag` 从真实页面重新执行 `build-release -> deploy-test -> mark-tested -> prod -> backup`。
- 当前结论：业务仓现已完整收口 `20260619_mes_edhr_deployment_license_interface.sql` 的两阶段真实页面阻塞。由于 SQL 发布输入再次变化，维护仓必须使用新的 `releaseTag` 重新 build-release，并从真实页面重新走完整发布链路。
