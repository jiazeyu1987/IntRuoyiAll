# 生产组长报工列表员工姓名修复

## Task Goal

- 将生产组长报工管理列表与详情中的“员工/实际员工”从用户编号退回显示改为正式员工姓名显示。
- 修复根因：后端时间线读模型需要返回 `actualEmployeeUserName`，前端不应只能收到空姓名后显示 `actualEmployeeUserId`。

## Milestones

- [x] 创建任务文档并记录并行脏工作区基线。
- [x] RED：用回归测试证明后端 mapper 当前把 `actualEmployeeUserName` 置空。
- [x] GREEN：修复 mapper 正式姓名来源并更新必要合同。
- [x] REGRESSION：运行目标静态/后端验证，确认列表姓名链路不再退回编号。
- [ ] Closeout：记录验证报告、经验沉淀、提交并推送。

## Expected Verification

- `node IntRuoyiBackend/yudao-module-mes/src/test/js/process-pool-timeline-mapper-static.spec.cjs`
- `node IntRuoyiFronted/tests/e2e/team-leader-production-report-employee-name-static.spec.cjs`
- 必要时运行目标 Maven/前端静态相邻测试。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标为后端正式读模型补齐姓名字段。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- `docs/backend-development.md#第三方报工直报正式链路门禁`
  - Trigger: `team-leader/submission/page`、`MesTeamLeaderWorkbenchService.getSubmissionPage`、`MesProProcessPoolTimelineReadMapper`、`actual_employee_id`。
  - Preflight check: 页面读模型按 `mes_pro_process_pool_event.server_submit_time`、`actual_employee_id` 和生产组长责任员工集合筛选；本次只修读模型姓名字段，不用前端假行或空列表刷新替代正式时间线。
  - Blocker: 后端不能追溯正式员工姓名来源，或只能靠前端猜测/编号转文案时停止。
  - Verification: 静态合同锁定 mapper 不再返回 `NULL AS actualEmployeeUserName`，前端合同锁定员工列优先姓名。
  - Forbidden action: 禁止用前端硬编码、默认显示值、API-only 非组长账号或空姓名 fallback 冒充姓名修复。
