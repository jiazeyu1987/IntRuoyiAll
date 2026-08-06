# Bug Regression Evidence

## Bug Summary

- 生产组长报工列表“员工”列显示 `964`，用户期望显示员工姓名。

## Expected Behavior

- 后端 `ProcessPoolTimelineEventRespVO.actualEmployeeUserName` 应返回正式姓名；前端优先显示该姓名。

## Reproduction

- `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs`
- `node IntRuoyiFronted\tests\e2e\team-leader-production-report-employee-name-static.spec.cjs`

## Root Cause

- `MesProProcessPoolTimelineReadMapper.xml` 将 `actualEmployeeUserName` 写成 `NULL`，导致后端列表/详情响应没有实际员工姓名。
- `TeamLeaderWorkbenchPage.vue` 在姓名缺失时退回显示 `actualEmployeeUserId`，所以用户看到 `964` 这类编号。

## Regression Test

- 更新 `process-pool-timeline-mapper-static.spec.cjs`，锁定 mapper 必须按租户和删除标记关联 `system_users actual_employee` 并返回 `actual_employee.nickname AS actualEmployeeUserName`。
- 新增 `team-leader-production-report-employee-name-static.spec.cjs`，锁定员工列和详情字段不得把 `actualEmployeeUserId` 当显示文案。

## RED

- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> FAIL，`时间轴 mapper 必须读取 F1 正式字段：actual_employee.nickname AS actualEmployeeUserName`。
- RED: `node IntRuoyiFronted\tests\e2e\team-leader-production-report-employee-name-static.spec.cjs` -> FAIL，员工列仍包含 `row.actualEmployeeUserName || row.actualEmployeeUserId || '--'`。
- `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> FAIL，`时间轴 mapper 必须读取 F1 正式字段：actual_employee.nickname AS actualEmployeeUserName`。
- `node IntRuoyiFronted\tests\e2e\team-leader-production-report-employee-name-static.spec.cjs` -> FAIL，员工列仍包含 `row.actualEmployeeUserName || row.actualEmployeeUserId || '--'`。

## GREEN

- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> PASS。
- GREEN: `node IntRuoyiFronted\tests\e2e\team-leader-production-report-employee-name-static.spec.cjs` -> PASS。
- `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> PASS。
- `node IntRuoyiFronted\tests\e2e\team-leader-production-report-employee-name-static.spec.cjs` -> PASS。

## Verification

- Verification: `node IntRuoyiFronted\tests\e2e\pqc-leader-sample-values-detail-only-static.spec.cjs` -> PASS。
- Verification: `node IntRuoyiFronted\tests\e2e\production-leader-report-row-modify-action-static.spec.cjs` -> PASS。
- Verification: `node IntRuoyiFronted\tests\e2e\team-leader-hide-review-copy-columns-static.spec.cjs` -> PASS。
- Verification: `node IntRuoyiFronted\tests\e2e\team-leader-production-report-payload-columns-static.spec.cjs` -> FAIL，生产工单列旧合同失败，非本任务姓名链路改动。

## Risk And Regression Scope

- 风险集中在工序池时间线列表/详情、生产组长与 PQC 组长报工管理页员工姓名展示。

## Blockers And Follow-Up

- 相邻旧合同 `team-leader-production-report-payload-columns-static.spec.cjs` 当前因“生产工单”列断言失败；该失败不属于本次员工姓名修复 diff，已在 `verification-report.md` 单独记录。
