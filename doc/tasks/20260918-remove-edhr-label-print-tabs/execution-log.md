# 执行日志

## BDD

BDD: eDHR 标签打印入口下线 -> Given 用户打开生产模块菜单或访问 eDHR 标签打印路由，When 系统加载前端路由和菜单，Then 不再提供标签模板、标签实例、打印任务、打印策略四个页签，也不再提供对应页面入口或 900320-900331、900338-900346 菜单入口。

BDD: eDHR 标签打印历史能力保留 -> Given 后端历史表、控制器和接口已经存在，When 本次 UI 菜单退休迁移执行，Then 不删除业务表、业务数据或后端接口，只软删除相关菜单并从角色和租户套餐菜单集合移除。

## TDD Evidence

RED: `node tests/e2e/edhr-label-print-queue-static.spec.js` -> FAIL, 旧 eDHR 标签打印 API 文件仍存在。

RED: `python -X utf8 -m pytest script/tests/test_edhr_label_print_menu_removal_sql.py` -> FAIL, 菜单退休迁移文件尚未创建。

GREEN: `node tests/e2e/edhr-label-print-queue-static.spec.js` -> PASS, 页面/API/路由/定向 TypeScript 引用均已移除。

GREEN: `node tests/e2e/edhr-print-policy-reissue-static.spec.js` -> PASS, 打印策略及打印相关 UI 入口已退休。

GREEN: `node tests/e2e/edhr-redundant-route-alias-static.spec.js` -> PASS, 相关路由别名和已删除组件入口不再存在。

GREEN: `python -X utf8 -m pytest script/tests/test_edhr_label_print_menu_removal_sql.py script/tests/test_edhr_label_print_queue_schema_sql.py script/tests/test_edhr_print_policy_reissue_schema_sql.py` -> PASS, 13 tests passed。

GREEN: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file sql/mysql/20260608_edhr_batch_execution_schema.sql --sql-file sql/mysql/20260618_mes_edhr_traveler_instance_binding.sql --sql-file sql/mysql/20260618_mes_edhr_label_print_queue.sql --sql-file sql/mysql/20260618_mes_edhr_print_policy_reissue_void.sql --sql-file sql/mysql/20260918_mes_edhr_label_print_menu_removal.sql` -> PASS, 5 migrations dependency closure passed。

GREEN: `pnpm exec vue-tsc --noEmit -p tsconfig.schedule-relaxed.json` -> PASS。

GREEN: `pnpm exec eslint src/router/modules/remaining.ts scripts/edhr-release-e2e-coverage-gate.mjs tests/e2e/edhr-label-print-queue-static.spec.js tests/e2e/edhr-print-policy-reissue-static.spec.js tests/e2e/edhr-redundant-route-alias-static.spec.js tests/e2e/edhr-system-time-format-hardening-static.spec.js tests/e2e/edhr-deep-dedup-list-detail-real-flow.e2e.js --max-warnings=0` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260918-remove-edhr-label-print-tabs/frontend-feature-evidence.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260918-remove-edhr-label-print-tabs/database-schema-evidence.md` -> PASS。

GREEN: frontend/database evidence validator self-tests -> PASS。

BASELINE BLOCKER: `node tests/e2e/edhr-system-time-format-hardening-static.spec.js` -> FAIL, 既有 `ExecutionPage.vue` 缺少该测试原有的时间格式化断言；与本次标签/打印页面删除无关。

BASELINE BLOCKER: `node scripts/edhr-release-e2e-coverage-contract.test.mjs` -> FAIL, 既有覆盖矩阵未登记 `src/api/mes/pro/edhr/nonconformanceReview.ts` 和 `src/views/mes/pro/edhr/signatureSelection.ts`；与本次标签/打印页面删除无关。

BASELINE NOTE: 工作区另有 `docs/release-backup-restore.md` 和 `intrruoyi-runtime/` 并行改动，本任务未修改或清理。

## Milestone Status

- [x] 任务初始化
- [x] RED 删除回归测试失败
- [x] 前端入口移除
- [x] 菜单退休迁移
- [x] GREEN 验证通过
- [x] 收尾清理 preview/apply
- [x] 收尾完成

## Closeout

GREEN: `task-closeout.py --task-id 20260918-remove-edhr-label-print-tabs --mode preview` -> PASS, keep 清单仅保留 task.md、execution-log.md、verification-report.md，临时 evidence 文件进入 delete 清单。

GREEN: `task-closeout.py --task-id 20260918-remove-edhr-label-print-tabs --mode apply` -> PASS, 仅删除两份 task-local evidence 文件。

GREEN: `git commit -m "Remove eDHR label print tabs"` -> PASS, implementation commit `1d2f593bf`.

GREEN: final closeout status updated to `completed` after task-owned implementation commit and cleanup evidence were recorded.

NOTE: `docs/release-backup-restore.md`、`intrruoyi-runtime/` 以及 `IntRuoyiBackend/yudao-module-infra/src/test/java/cn/iocoder/yudao/module/infra/service/runtimecontrol/RuntimeRestoreCandidateServiceImplTest.java` 是并行/无关工作区改动，本任务未纳入、未回滚、未清理。
