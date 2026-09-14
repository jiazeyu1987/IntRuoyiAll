# 审批中心操作入口收敛

## Task Goal

将审批中心每行操作入口统一为“查看、审核、流程”三个按钮，保留现有审批、业务详情和流程留痕能力，避免修改后端接口和业务路由契约。

## Milestones

1. 记录 BDD 场景并建立操作列静态回归合同。
2. 将操作列收敛为查看、审核、流程，并保持按任务能力控制可用性。
3. 运行定向静态合同、前端类型检查和差异校验，记录剩余环境阻塞。
4. 完成任务收尾记录。

## Expected Verification

- `node tests/e2e/approval-center-actions-consolidation-static.spec.js`
- `node scripts/approval-center-page-contract.test.mjs`
- `node tests/e2e/approval-center-review-action-static.spec.js`
- `node tests/e2e/approval-center-bpm-detail-clickable-static.spec.js`
- `pnpm ts:check`
- `git diff --check`

## Current Status

ready_for_closeout

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；不新增任何 fallback、降级或异常吞没。
- 是否从根因和长期维护角度解决：是；统一操作入口由正式任务能力和既有路由决定。
- 是否存在临时补丁或绕过：否。

## Cleanup Keep

- doc/tasks/20260826-approval-center-action-consolidation/task.md
- doc/tasks/20260826-approval-center-action-consolidation/execution-log.md
- doc/tasks/20260826-approval-center-action-consolidation/verification-report.md
