# 20260725-route-form-submit-fda-audit

## Task Goal

补齐表单中心路线表单提交导致 eDHR 工作任务完成、批次任务提交/批准、后续填写任务创建时的 FDA 操作审计，确保 who/what/when/why、前后状态、权限结果和批次追溯可见性完整。

## Milestones

- [x] M1: 创建任务记录并隔离既有并发脏区。
- [ ] M2: 用 RED 测试复现路线表单提交缺少操作审计的问题。
- [ ] M3: 在正式服务路径中补齐操作审计，不引入 fallback、吞异常或默认成功。
- [ ] M4: 运行目标 Maven/JUnit、静态合同和必要回归验证。
- [ ] M5: 更新验证报告、完成收尾、提交并推送。

## Expected Verification

- 目标单测先证明 `completeRouteFormFillAndCreateNextFill` 完成路线表单填写时必须记录操作审计。
- 操作审计包含 `batchExecutionId`、`workTaskId`、`objectType/objectId`、`operationType`、操作者、操作时间、前后状态、原因、权限判定、结果状态、幂等键、请求来源。
- 路线表单提交路径仍会完成当前填写任务、更新批次任务提交/批准状态，并创建下一填写任务。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，计划把路线表单提交纳入统一操作审计链路。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 命中 `docs/backend-development.md#2026-07-25 Maven Reactor 兄弟模块验证门禁`：涉及 yudao-module-mes 后端代码和测试时，验证使用 `mvn -pl yudao-module-mes -am ...`。
- 命中 `docs/e2e-rules.md#edhr-本地状态样本操作审计追溯门禁` 的通用原则：不得只验证审计落库，后续真实路径需关注批次追溯可见性。
