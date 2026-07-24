# 执行日志: 20260622-edhr-dhr-template-menu-id-collision-fix

- BDD: DHR 模板 SQL 不能复用已占用 unified change menu id -> Given 真实测试库中 900293-900299 已被 eDHR统一变更占用 / When 执行 20260618_mes_edhr_dhr_template_lifecycle.sql / Then SQL 必须把 DHR 模板按钮菜单切到独立未冲突号段，并完整落下完整性检查/审核/签核/生效/停用/作废菜单。
- BDD: DHR 模板修复后真实页面 deploy-test 不得再因 900293 冲突中断 -> Given 维护仓真实页面 deploy-test(v10) 已证明当前失败点是 dhr_template_lifecycle SQL / When 修复后的 SQL 重新进入 clean 发布包并参与真实页面发布 / Then 测试服发布链路不得再在该 SQL 阶段因 system_menu 主键冲突失败。

- GREEN: previous-task-check -> PASS，上一后端任务 `20260622-showroom-publish-audio-integrity-gate` 已 `completed`，最近直接相关的菜单冲突任务 `20260622-edhr-print-policy-menu-id-collision-fix` 也已 `COMPLETED_WAITING_MAIN`，且维护仓真实页面 `deploy-test(v10)` 已证明其修复成功跨过 print policy 阶段；当前新的阻塞已切换到后续 DHR 模板 SQL 菜单主键冲突。
- GREEN: maintenance-failure-read -> PASS，只读核对维护仓 operation `op-2026-06-22T031709289783300Z-b87c3859-b5bf-4f2c-b03f-008cf8d5e7b3` 日志后确认：真实页面 `deploy-test(v10)` 失败于 `20260618_mes_edhr_dhr_template_lifecycle.sql` line `120`，错误为 `Duplicate entry '900293' for key 'system_menu.PRIMARY'`。
- GREEN: experience-preflight -> PASS，已读取 `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md` 命中的 `release-backup-restore.md` 与 `server-access.md`，并完成维护仓失败证据、业务仓回归入口与任务文档门禁预检；允许进入本任务的只读根因定位与 RED 契约编写。
- GREEN: root-cause-readonly -> PASS，只读核对测试服 `172.30.30.58` 当前真实 `system_menu` 确认：`900290-900292` 已被 DHR 模板页面/查询/创建占用，而 `900293-900299` 已被 `eDHR统一变更 / 创建 / 提交 / 审批 / 生效申请 / 影响范围 / 事件` 占用；源码搜索同步确认 `20260618_mes_edhr_dhr_template_lifecycle.sql` 当前仍把 DHR 模板按钮复用为 `900293-900298`。这证明当前正式 SQL 在按钮段上直接撞上统一变更菜单主键，而 `900347-900352` 在源码与测试服现场均未被占用，可作为独立安全号段。
- RED: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_edhr_dhr_template_schema_sql.py -q` -> FAIL，新增契约要求 DHR 模板按钮必须改用独立未冲突的 `900347-900352`，当前 SQL 仍停留在冲突号段 `900293-900298`，断言 `900347` 缺失。
- GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_edhr_dhr_template_schema_sql.py -q` -> PASS，DHR 模板 SQL 已切换到独立号段 `900347-900352`，4 项契约全部通过。
- GREEN: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql` -> PASS，迁移策略门禁继续通过。
