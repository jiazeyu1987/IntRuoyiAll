# 生产组长报工历史审核通过人姓名补齐

## Task Goal

- 补齐生产组长报工历史读模型中的审核通过人姓名字段，避免历史页只能显示审核人编号或空值。
- 修复当前后端静态合同阻塞：`MesProProcessPoolTimelineReadMapper` 必须返回 `submissionReviewLeaderUserName`。

## Milestones

- [x] 创建任务文档并复现当前静态合同 RED。
- [x] GREEN：后端时间线读模型正式返回审核通过人姓名，并透传到响应 VO。
- [x] REGRESSION：运行后端 mapper 合同、员工姓名合同和报工历史合同。
- [ ] Closeout：记录验证证据并完成提交/推送。

## Expected Verification

- `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs`
- `node IntRuoyiFronted\tests\e2e\team-leader-production-report-employee-name-static.spec.cjs`
- `node IntRuoyiFronted\tests\e2e\team-leader-production-report-history-tab-static.spec.cjs`
- `mvn -pl yudao-module-mes -am "-DskipTests" compile`
- `pnpm ts:check`

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标为后端正式时间线读模型补齐姓名字段。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- `docs/backend-development.md#第三方报工直报正式链路门禁`
  - Trigger: `team-leader/submission/page`、`MesProProcessPoolTimelineReadMapper`、生产组长报工读模型姓名字段。
  - Preflight check: 页面读模型中的人员姓名必须来自正式 `system_users.nickname` 关联，不能靠前端编号兜底或硬编码文案。
  - Blocker: mapper 只能返回审核人 ID、无法追溯正式用户姓名、或前端必须用 ID 推断姓名时停止。
  - Verification: 静态合同锁定 mapper 返回 `review_leader.nickname AS submissionReviewLeaderUserName`，前端历史页显示姓名字段。
  - Forbidden action: 禁止用前端硬编码、空值成功、编号 fallback 或 API-only 说明冒充姓名修复。

## Cleanup Candidates

- doc/tasks/20260807-team-leader-review-leader-name/bug-regression-evidence.md
