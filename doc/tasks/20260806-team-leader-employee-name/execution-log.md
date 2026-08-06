# Execution Log

## User Intent

- 用户指出生产组长报工列表“员工”列显示 `964`，要求改成姓名。

## BDD

- BDD: 生产组长报工列表员工显示姓名 -> Given 工序池事件存在实际填写员工 ID 且系统用户/员工档案有姓名 When 生产组长打开报工管理列表或详情 Then “员工/实际员工”必须显示正式姓名，不能因后端姓名为空退回显示编号。

## Preflight

- 使用 `bug-regression-fix-loop` 技能，要求先复现、RED、最小修复、GREEN。
- 已读取 `docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/database-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- 初始 `git status --short --branch` 显示 `int_main...origin/int_main [ahead 1]` 且有多项并行脏改动；按项目规则先独立基线。

## Dirty Worktree Baselines

- Baseline 1: `8b083a6e0 chore: baseline dirty worktree before employee name fix`
  - 文件：`RouteFlowGraphDesigner.vue`、路线/PQC 静态测试、`20260806-qa-route-checkflag-load-error` 任务文档、`20260806-hide-review-copy-columns` 任务文档与测试等 12 个既有改动。
  - 备注：`git diff --cached --check` 命中两个既有任务文档 EOF 空行；未修改并行任务内容，作为基线门禁异常记录后隔离提交。
- Baseline 2: `b29b78104 chore: baseline concurrent production report edits`
  - 文件：`TeamLeaderWorkbenchPage.vue` 删除审核副本/复核判定列、`20260806-production-report-row-modify-action` 任务文档。
- Baseline 3: `34e2faceb chore: baseline concurrent route leader task updates`
  - 文件：`production-leader-report-row-modify-action-static.spec.cjs`、`20260806-route-start-production-leader-top-save` 任务文档与验证报告。
- Baseline 4: `30e2bd0cd chore: baseline concurrent submission edit action updates`
  - 文件：`TeamLeaderWorkbenchPage.vue` 修改/修正操作文案与复核日志详情、`pqc-leader-sample-values-detail-only-static.spec.cjs`。
- Baseline 5: `072320de6 chore: baseline concurrent route leader closeout updates`
  - 文件：`20260806-route-start-production-leader-top-save` 执行日志与验证报告收尾补写。

## Evidence

- Experience gate: 命中 `docs/backend-development.md#第三方报工直报正式链路门禁` 中 `team-leader/submission/page` / `MesProProcessPoolTimelineReadMapper` / `actual_employee_id` 读模型规则；任务约束已写入 `task.md`。
- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> FAIL，预期失败原因：mapper 当前缺少 `actual_employee.nickname AS actualEmployeeUserName` 且返回 `NULL AS actualEmployeeUserName`。
- RED: `node IntRuoyiFronted\tests\e2e\team-leader-production-report-employee-name-static.spec.cjs` -> FAIL，预期失败原因：员工列模板仍为 `row.actualEmployeeUserName || row.actualEmployeeUserId || '--'`。
- FIX: `MesProProcessPoolTimelineReadMapper.xml` -> `TimelineAuthorityJoins` 增加 `system_users actual_employee` 租户/删除标记关联，`TimelineColumns` 返回 `actual_employee.nickname AS actualEmployeeUserName`。
- FIX: `TeamLeaderWorkbenchPage.vue` -> 报工列表和详情员工字段只渲染 `actualEmployeeUserName || '--'`，不再把 `actualEmployeeUserId` 当显示文案。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> PASS。
- GREEN: `node IntRuoyiFronted\tests\e2e\team-leader-production-report-employee-name-static.spec.cjs` -> PASS。
- REGRESSION: `node IntRuoyiFronted\tests\e2e\pqc-leader-sample-values-detail-only-static.spec.cjs` -> PASS。
- REGRESSION: `node IntRuoyiFronted\tests\e2e\production-leader-report-row-modify-action-static.spec.cjs` -> PASS。
- REGRESSION: `node IntRuoyiFronted\tests\e2e\team-leader-hide-review-copy-columns-static.spec.cjs` -> PASS。
- REGRESSION-NONTASK: `node IntRuoyiFronted\tests\e2e\team-leader-production-report-payload-columns-static.spec.cjs` -> FAIL，失败点为既有 `productionSubmissionDefaultColumns` 包含 `label: '生产工单'`；本任务 diff 只涉及员工姓名渲染和 mapper 姓名字段，未改生产工单列。
