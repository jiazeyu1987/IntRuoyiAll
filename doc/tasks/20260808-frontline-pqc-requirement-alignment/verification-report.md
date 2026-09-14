# Verification Report

## Scope

- 一线 PQC 工序选择、首检/巡检任务快照、检验项 Tab 名称、检验方法展示、巡检数量计算、提交到 PQC 组长管理列表过滤。

## Results

- PASS: `node tests\e2e\pqc-requirement-alignment-static.spec.cjs`
- PASS: `node tests\e2e\pqc-tab-item-name-display-static.spec.cjs`
- PASS: `node tests\e2e\pqc-tab-method-display-static.spec.cjs`
- PASS: `node tests\e2e\pqc-active-title-method-display-static.spec.cjs`
- PASS: `pnpm ts:check`
- PASS: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderServiceTest#shouldAddWorkOrderWithoutScheduleFromActiveRouteSnapshot,MesTeamLeaderActiveOrderServiceTest#shouldKeepScheduledPqcBusinessDateFromProcessPlanDateWhenErpPlannedStartMissing,MesTeamLeaderActiveOrderServiceTest#shouldGenerateFormalPqcTasksFromPublishedRegulationWhenAddingActiveOrder,MesTeamLeaderActiveOrderServiceTest#shouldResolvePatrolInspectionQuantityFromQaPercentageRatio,MesTeamLeaderActiveOrderErpPlannedStartTest#shouldUseJoinedDateForUnscheduledPqcTasksWhenErpPlannedStartMissing,MesFrontlinePqcContextServiceTest#shouldExposeFirstAndPatrolTaskOptionsForSameProcess,MesTeamLeaderWorkbenchServiceImplTest#shouldRestrictPqcLeaderManagementPageToPqcInspectionEvents" "-Dsurefire.failIfNoSpecifiedTests=false" test`，7 tests run, 0 failures, 0 errors.
- PASS: `git diff --check -- <task-owned paths>`，仅 Git CRLF normalization warnings。
- PASS: cleanup preview/apply，保留核心任务记录，无删除项、无阻塞、无警告。

## Notes

- 第一轮后端 Maven 未进入 Surefire，失败原因为 `target\classes` 构建产物缺失，不是 PQC 业务断言失败；已通过单模块主类重建后复跑目标 JUnit 获得 PASS。
- 未运行真实写入型 E2E；本轮完成静态契约、类型检查和后端目标单元/服务测试验证。
