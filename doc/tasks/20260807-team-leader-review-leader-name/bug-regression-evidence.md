# Bug Regression Evidence

## Bug Summary

- 生产组长报工历史需要展示审核通过人姓名，但后端时间线 mapper 原先只具备审核负责人 ID，当前静态合同要求补齐 `submissionReviewLeaderUserName`。

## Expected Behavior

- 生产组长报工列表员工列显示正式姓名。
- 生产组长报工历史的审核通过人显示 `system_users.nickname`，不能显示编号或空值。

## Reproduction

- `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> FAIL，缺少 `review_leader.nickname AS submissionReviewLeaderUserName`。

## Root Cause

- `MesProProcessPoolTimelineReadMapper.xml` 的时间线读模型未关联审核负责人用户姓名，响应链路缺少审核通过人姓名字段。

## Regression Test

- 更新后端 mapper 静态合同，锁定 `review_leader.nickname AS submissionReviewLeaderUserName` 和 `system_users review_leader` 租户/删除标记关联。
- 更新生产组长报工历史静态合同，锁定历史页展示 `submissionReviewLeaderUserName`。

## RED

- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> FAIL，预期失败原因：mapper 缺少审核通过人姓名字段。

## GREEN

- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> PASS。
- GREEN: `node IntRuoyiFronted\tests\e2e\team-leader-production-report-employee-name-static.spec.cjs` -> PASS。
- GREEN: `node IntRuoyiFronted\tests\e2e\team-leader-production-report-history-tab-static.spec.cjs` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Verification

- Verification: 后端 mapper 静态合同、前端生产组长员工姓名合同、前端生产组长报工历史合同、MES reactor 编译和前端类型检查均通过。

## Blockers

- Blockers: 无当前任务阻塞；并行 PQC 历史页签合同失败不属于本次生产组长姓名链路完成门禁。

## Risk And Scope

- Scope limited to process-pool timeline read model and production leader report/history static contracts.
- No fallback, mock data, default success, or frontend ID-to-name guessing was introduced.
