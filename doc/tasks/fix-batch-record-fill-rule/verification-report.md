# 验证报告：修复批记录模板填写规则误报

## Summary

- 已修复保存边界：`source=AUTO && reviewed=true` 的已确认规则归一化为 `source=MANUAL && reviewed=true`，执行层继续 fail fast 拦截真正未确认规则。
- 已修复用户授权纳入范围的两个旧阻塞：`resolveRecordbookEnabled(Boolean,String)` 编译前置可通过，损耗报告 Word 解析回归已恢复 `□报废`。
- 已修复真实 E2E 新暴露的 `/task/open` 上下文误判：传统批记录任务按 `executionId + batchRecordReportId` 校验，Form Center 任务仍要求完整动态表单上下文，`BATCH_SHARED` 缺少冻结执行仍报错。
- 已重启本地后端并完成真实前端 E2E：测试租户/aoteman 打开既有批次 `JILUBEN-E2E-1784859323164`，`/task/open` 返回成功并进入 eDHR 执行页。
- 历史异常 JSON dry run/apply 仍按原设计拆为后续受控任务；本次未静默修复历史模板数据。

## Commands

- `mvn -pl yudao-module-mes '-Dtest=MesProEdhrBatchExecutionServiceTest' test`
- `mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordCellRuleSupportTest,MesProBatchRecordReportServiceImplDbTest' test`
- `mvn -pl yudao-module-mes '-Dtest=MesProRouteVersionPublishProjectionServiceTest,MesProRouteVersionPublishProjectionServiceImplTest,MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldSerializeCurrentBatchRecordBindingsFromProcessSettings' test`
- `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root doc\tasks\fix-batch-record-fill-rule`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\fix-batch-record-fill-rule\bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\fix-batch-record-fill-rule\backend-api-evidence.md`
- `git diff --check`

- `mvn -pl yudao-module-mes '-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_opensLegacyBatchRecordTaskWithFrozenExecutionWithoutFormCenterContext' test`
- `mvn -pl yudao-module-mes '-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_opensLegacyBatchRecordTaskWithFrozenExecutionWithoutFormCenterContext+openTask_requiresFrozenExecutionForBatchSharedTask' test`
- `mvn -pl yudao-module-mes '-Dtest=MesProRouteFlowConfigServiceImplTest#getRouteFlowProcessConfigList_shouldReturnInternalRecordMetadata' test`
- `mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordCellRuleSupportTest,MesProBatchRecordReportServiceImplDbTest' test`
- `mvn -pl yudao-server -am -DskipTests package`
- `node --check tests\e2e\edhr-batch-execution-real-flow.e2e.js`
- `node tests\e2e\edhr-batch-execution-real-flow.e2e.js`

## Result

- PASS：`MesProEdhrBatchExecutionServiceTest` 全类 134/134 通过，实时路线配置、冻结快照历史恢复、共享批记录执行冻结和质量拒收签名校验顺序均已覆盖。
- PASS：路线发布/快照相关 13/13 通过，当前路线快照继续携带批记录绑定元数据。
- PASS：三份任务证据结构校验通过，`git diff --check` 退出码 0。
- BLOCKED：当前 shell 复跑真实 E2E 缺少任务专用环境变量，脚本 fail fast 未进入浏览器；此前 `doc/tasks/fix-batch-record-fill-rule/real-e2e-evidence.md` 的 PASS 证据未被覆盖。

- PASS：传统批记录任务 openTask 回归通过，修复前 RED 为 `1040750412 eDHR 批次缺少唯一批记录路线`，修复后返回真实 executionId。
- PASS：共享任务冻结执行门禁仍保留，`BATCH_SHARED` 缺少 `executionId` 时仍返回 `PRO_EDHR_BATCH_EXECUTION_TASK_CONTEXT_REQUIRED`。
- PASS：编译前置目标测试 1/1 通过。
- PASS：规则支持层与报表服务回归集 129/129 通过，损耗报告 Word 解析不再失败。
- PASS：本地后端 48081 新 jar 健康检查 `UP`，前端 8081 返回 200。
- PASS：真实前端 E2E 通过，证据见 `doc/tasks/fix-batch-record-fill-rule/real-e2e-evidence.md`。

## Remaining Blockers

- 无当前授权范围内验证阻塞。
- 保留历史事实：最早的 `source/reviewed` 严格 RED 因当时范围外编译错误未能在修复前取得；当前新增的传统批记录 openTask 阻塞已取得 RED/GREEN。
- 后续可选：历史模板 JSON dry run/apply 需另行授权，不能作为本次最小修复自动执行。
