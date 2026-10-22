# 移除 eDHR 标签打印页签

## Task Goal

删除 eDHR 标签管理页面中的“标签模板”“标签实例”“打印任务”“打印策略”四个页签，并移除对应的前端页面入口和运行时菜单入口。运行时菜单范围包括标签/任务菜单 900320-900331，以及打印策略和打印相关子菜单 900338-900346。

后端标签、实例、打印任务和打印策略相关表、控制器及 API 保留，避免本次 UI 下线影响历史数据和既有接口能力。数据库只做菜单软下线，不删除业务表或业务数据。

## 设计约束检查

- 仅移除当前实际运行的 legacy 页面入口；`docs/edhr/*-contract.json` 中路径不同的商业化规划页签不属于当前运行页面，本任务不改动。
- 不修改历史迁移文件，不执行真实数据库写入。
- 不增加 fallback、mock、静默成功或吞错逻辑。
- 只处理本任务涉及的源文件和测试，不影响并行任务或无关改动。

## Milestones

1. 建立任务文档、BDD 场景和删除回归测试。
2. RED：确认页面、API、路由和运行时菜单仍存在。
3. 移除前端页面、API 文件、静态路由及相关 TypeScript 排除项。
4. 增加菜单退休迁移，软删除 900320-900331、900338-900346，并从角色和租户套餐菜单集合中移除。
5. GREEN：运行前端删除契约、后端 SQL 契约、相关静态回归和差异检查。
6. 完成验证记录和任务收尾状态。

## Expected Verification

- `node tests/e2e/edhr-label-print-queue-static.spec.js`
- `python -X utf8 -m pytest script/tests/test_edhr_label_print_menu_removal_sql.py script/tests/test_edhr_label_print_queue_schema_sql.py`
- `python -X utf8 -m pytest script/tests/test_edhr_print_policy_reissue_schema_sql.py`
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file sql/mysql/20260608_edhr_batch_execution_schema.sql --sql-file sql/mysql/20260618_mes_edhr_traveler_instance_binding.sql --sql-file sql/mysql/20260618_mes_edhr_label_print_queue.sql --sql-file sql/mysql/20260618_mes_edhr_print_policy_reissue_void.sql --sql-file sql/mysql/20260918_mes_edhr_label_print_menu_removal.sql`
- `git diff --check`
- `pnpm exec vue-tsc --noEmit -p tsconfig.schedule-relaxed.json`
- 前端定向静态检查；不执行真实 E2E，因为本轮未明确要求 E2E。

## Cleanup Keep

- doc/tasks/20260918-remove-edhr-label-print-tabs/task.md
- doc/tasks/20260918-remove-edhr-label-print-tabs/execution-log.md
- doc/tasks/20260918-remove-edhr-label-print-tabs/verification-report.md

## Current Status

completed

## Closeout Result

- Implementation commit: `1d2f593bf` (`Remove eDHR label print tabs`).
- Task closeout cleanup preview/apply passed before final status update.
- Final task records and reusable project experience were prepared for a separate closeout commit.
