# 执行日志: 20260622-edhr-print-policy-menu-id-collision-fix

- BDD: 打印策略 SQL 不能复用已占用 menu id -> Given 真实测试库中 900285 已被其他 eDHR 菜单占用 / When 执行 20260618_mes_edhr_print_policy_reissue_void.sql / Then SQL 必须使用独立未冲突的 menu id，并完整落下打印策略页面与按钮菜单。
- BDD: 打印策略修复后真实页面 deploy-test 不得再因 900285 冲突中断 -> Given 维护仓真实页面 deploy-test(v9) 已证明当前失败点是 print policy SQL / When 修复后的 SQL 重新进入 clean 发布包并参与真实页面发布 / Then 测试服发布链路不得再在该 SQL 阶段因 system_menu 主键冲突失败。

- GREEN: previous-task-check -> PASS，上一后端任务 `20260622-edhr-oq-pq-legacy-cleanup-order-fix` 已 `COMPLETED`，且维护仓真实页面 `deploy-test(v9)` 已证明其修复成功跨过 OQ/PQ 阶段；当前新的阻塞已切换到后续 print policy SQL 菜单主键冲突。
- GREEN: maintenance-failure-read -> PASS，只读核对维护仓 operation `op-2026-06-22T011513436036700Z-42784c13-1bca-48e9-b0a9-fa51f2309f0a` 日志后确认：真实页面 `deploy-test(v9)` 失败于 `20260618_mes_edhr_print_policy_reissue_void.sql` line 103，错误为 `Duplicate entry '900285' for key 'system_menu.PRIMARY'`。
- GREEN: experience-preflight -> PASS，已读取 `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md` 命中的 `release-backup-restore.md` 与 `server-access.md`，并完成维护仓失败证据、业务仓已有回归入口与任务文档门禁预检；允许进入本任务的只读根因定位与 RED 契约编写。
- GREEN: root-cause-readonly -> PASS，只读核对测试服 `172.30.30.58` 当前真实 `system_menu` 确认：`900283-900285` 已被 `eDHR交付驾驶舱 / 交付查询 / 交付项目创建` 占用，`900286-900289` 已被 `eDHR验证包矩阵 / 验证包查询 / 验证包创建 / 追溯门禁评估` 占用；源码搜索同时确认 `20260618_mes_edhr_flow_intervention_log.sql` 也复用了 `900286-900292`。这证明 `20260618_mes_edhr_print_policy_reissue_void.sql` 当前复用的 `900284-900292` 全段都不再适合作为 print policy 正式菜单号。
- RED: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_edhr_print_policy_reissue_schema_sql.py -q` -> FAIL，新增契约要求 print policy SQL 必须改用独立未冲突的 `900338-900346`，当前 SQL 仍停留在冲突号段 `900284-900292`，断言 `900338` 缺失。
- GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_edhr_print_policy_reissue_schema_sql.py -q` -> PASS，print policy SQL 已切换到独立号段 `900338-900346`，5 项契约全部通过。
- GREEN: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql` -> PASS，迁移策略门禁继续通过。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260622-edhr-print-policy-menu-id-collision-fix\bug-regression-evidence.md` -> PASS，缺陷回归证据校验通过。
