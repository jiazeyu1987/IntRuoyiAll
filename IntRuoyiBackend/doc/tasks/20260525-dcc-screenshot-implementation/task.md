# DCC 截图需求实现任务记录

## Task Goal

在 `IntRuoyi` 下复用现有 DCC、BPM、系统用户、前端 DCC 页面和权限能力，实现用户截图中的 DCC 首版可用需求，不在 `IntDCC` 开发，不另起一套独立 DCC 系统。

## Milestones

- T1 后端受控文件元数据与下载基础：completed。
- T2 前端上传、列表和下载入口：completed。
- T3 后端 DCC 流程动作与第四节点门禁：completed。
- T4 前端流程动作与第四节点页面：completed。
- T5 发放、打印、外来文件和密码策略：completed。
- T6 测试租户真实路径验证与收口：completed。

## Expected Verification

- BDD 场景必须覆盖截图可见的用户、业务和接口行为。
- TDD 必须记录 RED、GREEN 和回归验证证据。
- 后端 DCC/System 目标单测、SQL 迁移测试和模块回归通过。
- 前端 DCC 静态契约测试、相关回归测试和 `pnpm ts:check` 通过。
- 测试租户必须使用真实前端路径验证上传、提交、BPM 待办、审批详情、电子签名和状态推进。

## Current Status

Completed on 2026-05-25. T1 到 T6 均已通过，测试租户真实路径已经生成文件 `COD-225424`，第一层文控审核已通过并推进到 `PENDING_MATRIX_REVIEW`。

## Final Verification

- Backend targeted and module regression: PASS。
- Frontend contract and type checks: PASS。
- Playwright real path route check: PASS。
- Playwright upload submit: PASS，file id `2054545668044044040`。
- Database evidence: PASS，四层路线快照和 Flowable 待办已生成。
- Playwright first-node approval: PASS，`approve-task` 返回 `data=true`。
- Signature evidence: PASS，`dcc_controlled_file_signature` 写入第一层 `APPROVE` 电子签名。

## Cleanup Keep

- `doc/tasks/20260525-dcc-screenshot-implementation/dev-plan.md`
- `doc/tasks/20260525-dcc-screenshot-implementation/prd.md`
- `doc/tasks/20260525-dcc-screenshot-implementation/request-analysis.md`
- `doc/tasks/20260525-dcc-screenshot-implementation/test-plan.md`
- `doc/tasks/20260525-dcc-screenshot-implementation/test-report.md`
- `doc/tasks/20260525-dcc-screenshot-implementation/task-state.json`
