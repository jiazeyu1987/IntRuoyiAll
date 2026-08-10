# Execution Log

## User Intent

用户要求实现此前分析出的 PQC 偏差：检验项 Tab 显示名称、巡检数量按百分比抽样率计算、选择工序后按首检/巡检展示正式检验项和数量、无待检任务工序不再阻断全工序展示、PQC 组长管理列表避免混入非 PQC 记录。

## BDD Scenarios

- BDD: PQC tab displays inspection item names -> Given 一线 PQC 已选择订单和工序 When 检验项 Tab 渲染 Then Tab 标题显示 QA 检验项 `itemName`，不显示检验方法或内部编号。
- BDD: Patrol quantity uses QA percentage ratio -> Given 产品生产数量为 10000 且巡检抽样率为 0.4 When 选择该工序巡检 Then 检验数量显示 `10000 * 0.4 / 100 = 40`。
- BDD: Inspection type selection follows available tasks -> Given 同一订单工序存在首检和巡检待检任务 When 一线 PQC 在工序上选择首检或巡检 Then 页面切换到对应任务、检验项和数量；不存在任务的类型不可提交。
- BDD: PQC management lists only PQC submissions -> Given PQC 组长负责员工同时存在生产提交和 PQC 提交 When 打开 PQC 管理列表 Then 只返回 `PQC_INSPECTION` 事件。

## Milestone Updates

- completed: 已创建任务记录并读取前后端、PowerShell、任务收尾和经验门禁规则。
- completed: 一线 PQC 检验项 Tab 已改为显示 QA 检验项 `itemName`，检验方法继续显示在项目详情/方法区域。
- completed: 前端按工序保留全路线工序卡片，并通过 `pqcTaskOptions` 在首检/巡检间切换正式待检任务快照；无待检任务的工序不可提交但不阻断展示。
- completed: 后端巡检数量按百分比口径计算，`10000 * 0.4 / 100 = 40`；PQC 组长管理列表增加 `PQC_INSPECTION` 事件类型过滤。
- completed: 已运行前后端目标回归、类型检查和 task-owned diff 检查。
- completed: cleanup preview/apply 已通过，保留 task.md、execution-log.md、verification-report.md，无删除项、无阻塞、无警告。

## Verification Evidence

- RED: `node tests\e2e\pqc-requirement-alignment-static.spec.cjs` -> FAIL, expected reason: `formatPqcInspectionItemTabLabel` 仍返回 `item.label`，未显式使用 `item.itemName || '未配置检验项目名称'`。
- RED: `node tests\e2e\pqc-tab-item-name-display-static.spec.cjs` -> FAIL, expected reason: PQC 可见 Tab 标题仍来自 `item.label`。
- RED: `node tests\e2e\pqc-tab-method-display-static.spec.cjs` -> FAIL, expected reason: 红框 Tab helper 未按正式检验项名称显示。
- RED: `node tests\e2e\pqc-active-title-method-display-static.spec.cjs` -> FAIL, expected reason: Tab helper 未区分 Tab 名称和 active 方法标题。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderServiceTest#shouldAddWorkOrderWithoutScheduleFromActiveRouteSnapshot,MesTeamLeaderActiveOrderServiceTest#shouldKeepScheduledPqcBusinessDateFromProcessPlanDateWhenErpPlannedStartMissing,MesTeamLeaderActiveOrderServiceTest#shouldGenerateFormalPqcTasksFromPublishedRegulationWhenAddingActiveOrder,MesTeamLeaderActiveOrderServiceTest#shouldResolvePatrolInspectionQuantityFromQaPercentageRatio,MesTeamLeaderActiveOrderErpPlannedStartTest#shouldUseJoinedDateForUnscheduledPqcTasksWhenErpPlannedStartMissing,MesFrontlinePqcContextServiceTest#shouldExposeFirstAndPatrolTaskOptionsForSameProcess,MesTeamLeaderWorkbenchServiceImplTest#shouldRestrictPqcLeaderManagementPageToPqcInspectionEvents" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 7 tests run, 0 failures, 0 errors.
- GREEN: `node tests\e2e\pqc-requirement-alignment-static.spec.cjs` -> PASS.
- GREEN: `node tests\e2e\pqc-tab-item-name-display-static.spec.cjs` -> PASS.
- GREEN: `node tests\e2e\pqc-tab-method-display-static.spec.cjs` -> PASS.
- GREEN: `node tests\e2e\pqc-active-title-method-display-static.spec.cjs` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `git diff --check -- <task-owned paths>` -> PASS with Git CRLF normalization warnings only.
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-frontline-pqc-requirement-alignment --mode preview` -> PASS, keep core task records, delete none, blocked none, warnings none.
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-frontline-pqc-requirement-alignment --mode apply` -> PASS, deleted none, blocked none, warnings none.
- Environment note: 第一轮后端目标 Maven 在 testCompile 前因 `yudao-module-mes\target\classes` 大量 class 缺失失败；源文件仍存在。等待并发 Maven 结束后运行 `mvn -pl yudao-module-mes -DskipTests compile` 重建主类输出，再复跑目标 Maven 到达 Surefire 并 PASS。

## Blockers

- 当前工作区已有大量非本任务未提交改动；本任务只修改 PQC 需求对齐相关文件，不回滚或暂存 unrelated 改动。
- 无当前任务阻塞项；任务已完成。
