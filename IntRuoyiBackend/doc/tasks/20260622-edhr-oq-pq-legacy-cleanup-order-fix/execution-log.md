# 执行日志: 20260622-edhr-oq-pq-legacy-cleanup-order-fix

- BDD: legacy OQ/PQ 残留存在时也必须补齐新菜单 -> Given 测试服真实库已残留旧 OQ/PQ 页面/查询/创建菜单并且新 SQL 需要迁移到 900332-900337 / When 执行 20260618_mes_edhr_oq_pq_execution_deviation.sql / Then SQL 必须先吸收 legacy 残留再补齐新菜单，最终 900332-900337 六条都存在。
- BDD: 真实页面 deploy-test 在 legacy cleanup 后不得把 OQ/PQ 页面菜单删空 -> Given 维护仓真实页面 deploy-test(v8) 已证明测试库当前只剩 900335-900337 / When 修复后的 SQL 再次参与发布 / Then 不得再因 Missing eDHR OQ/PQ system_menu rows 中断测试服发布链路。

- GREEN: previous-task-check -> PASS，上一后端任务 `20260622-edhr-oq-pq-menu-definition-fix` 已 `COMPLETED`，且维护仓 `build-release(v8)` 已证明上一轮 OQ/PQ 号段修复进入 clean 发布包；当前新的阻塞已切换为 legacy cleanup 顺序缺陷。
- GREEN: maintenance-failure-read -> PASS，只读核对维护仓 operation `op-2026-06-22T001514869158500Z-1d251735-38f1-43d5-afe3-c40e4b603a8e` 日志后确认：真实页面 `deploy-test(v8)` 失败于 `20260618_mes_edhr_oq_pq_execution_deviation.sql` line 399，错误为 `Missing eDHR OQ/PQ system_menu rows; cannot merge tenant package menu_ids`。
- GREEN: experience-preflight -> PASS，已读取 `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md` 命中的 `release-backup-restore.md` 与 `server-access.md`，并完成对维护仓真实页面失败日志、业务仓源码和测试服只读 `system_menu` 的门禁预检；允许进入本任务的只读根因定位与 RED 契约编写。
- GREEN: root-cause-readonly -> PASS，只读核对测试服 `172.30.30.58` 当前真实 `system_menu` 确认：`900332-900337` 仅存在 `900335-900337`，`900332-900334` 缺失，旧 OQ/PQ `900290-900292` 已不存在，`900293-900295` 仍为 `eDHR统一变更`。这证明当前 SQL 会先因 legacy `path/permission` 残留跳过 `900332-900334` 插入，再删除 legacy `900290-900292`，最终把页面/查询/创建三条删空。
- RED: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_edhr_oq_pq_schema_sql.py -q` -> FAIL，新增契约要求 `DELETE legacy_menu` 必须先于 `900332/900333/900334` 插入，但当前 SQL 仍把 cleanup 放在插入之后，正好复现了真实测试服的删空根因。
- GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_edhr_oq_pq_schema_sql.py -q` -> PASS，cleanup 顺序已调整到新菜单插入之前，6 项契约全部通过。
- GREEN: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql` -> PASS，迁移策略门禁继续通过。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260622-edhr-oq-pq-legacy-cleanup-order-fix\bug-regression-evidence.md` -> PASS，回归证据校验通过。
- GREEN: maintenance-integration-v9 -> PASS，维护仓真实页面 `deploy-test(v9)` 的远端日志已确认 `20260618_mes_edhr_oq_pq_execution_deviation.sql` 成功 `APPLIED`，说明本任务修复已进入 clean 发布包并跨过原始测试服阻塞；维护仓新的失败点已切换到后续 `20260618_mes_edhr_print_policy_reissue_void.sql`。
