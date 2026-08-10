# 验证报告

## Summary

- 问题：`activeOrderId=45` 下存在 PQC 任务正式工序身份缺失，原逻辑静默过滤后误报“当前工序缺少待执行 PQC 检验任务”。
- 修复：运行态不再隐藏非取消任务的空 `routeProcessId/processId`，改为 fail fast 报出任务身份；迁移脚本按正式 PQC 规程与活跃订单工序快照回填并收紧非空约束。
- 风险：未连接真实数据库执行迁移，生产/测试数据落地需由授权发布流程执行迁移前置检查。

## Verification

- PASS: `python -X utf8 -m pytest script\tests\test_mes_pqc_task_identity_closure_sql.py -q`，结果 `3 passed in 4.42s`。
- PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldFailFastWhenPendingPqcTaskMissingFormalProcessIdentity" "-Dsurefire.failIfNoSpecifiedTests=false" test`，结果 `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，结果 `Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`。
- PASS: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output ..\doc\tasks\20260807-pqc-missing-task-active-order-45\migration-policy-gate.json`，结果 `status=passed`。
- PASS: 本地运行态 Jar 复核，新 Jar 内 `MesFrontlinePqcContextServiceImpl` 包含 `CANCELLED/pqcTaskIdentityText/selectActiveOrderIdsByTaskStatus`，旧 Jar 不包含。
- PASS: 本地后端重启到 PID `6360`，`http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- PASS: 登录态只读 PQC 待检列表返回 `PQC_ACTIVE_ORDER_COUNT=6` 且 `CONTAINS_ACTIVE_ORDER_45=False`。

## Closeout Notes

- 本任务未执行 Git stage/commit/push，符合项目 Git Policy：未被用户显式要求时不执行 Git 操作。
- 本任务未操作远程服务、生产库、测试库或本地运行态数据。
- Cleanup PASS: `task-closeout-cleanup` preview/apply 已保留 `task.md/execution-log.md/verification-report.md`，删除临时 `bug-regression-evidence.md` 与 `migration-policy-gate.json`。
- Experience PASS: 已更新 `docs/backend-development.md` 与 `docs/experience-index.md` 中的 PQC 长期经验门禁。
- Reopen cleanup PASS: 二次运行态核验产生的 `runtime-jar-check*` 与 `runtime-jar-stage` 临时文件已由 cleanup 删除；任务目录最终仅保留三份正式记录。
