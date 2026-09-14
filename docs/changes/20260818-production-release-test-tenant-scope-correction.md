# 生产放行测试租户范围口径修正

## Request Summary

- change_id: `20260818-production-release-test-tenant-scope-correction`
- source: 用户于 2026-08-18 指出“从测试租户下登录，操作肯定都是在测试租户下”，并要求继续。
- requested_change: 移除“必须使用可见租户管理菜单的账号证明租户范围”的错误前置；当前验收以登录页选择或输入的测试租户作为租户边界。

## Current Baseline Reviewed

- 任务目录：`doc/tasks/20260814-production-release-flow-implementation`。
- 产品基线：`prd.md` 仍要求租户隔离、角色权限、真实页面、任务自有数据和无 fallback。
- 验收基线：P1-P10 已完成；P11 仍缺真实多账号业务主链、附件、追溯、清理和独立验收证据。
- 已完成前置：`zhulijiang`、`xujianhai` 已通过真实用户页面完成授权密码重置和权限登录核验；业务 fixture 写入仍为 0。
- 错误阻塞：Pass 50/51 将“当前管理员看不到租户管理菜单”写成继续前置，这把测试租户登录范围确认与租户管理权限错误绑定。

## Classification

- type: 验收前置口径修正。
- product_scope_change: 否。
- behavior_change: 否。
- data_scope_change: 否。
- test_scope_change: 是；当前 Agent 开发交付和用户手工验收不再要求租户管理菜单账号。

## Impact Analysis

| 范围 | 影响 |
| --- | --- |
| 产品 | AC-01 至 AC-34 不变；租户隔离仍是正式能力要求。 |
| 设计 | 无接口、数据模型或页面设计变更。 |
| 数据 | 不新增、不修改、不清理业务数据；本变更只调整前置判断。 |
| API | 不授权 API-only 或 SQL 替代页面路径。 |
| 测试 | 测试租户登录本身可作为当前业务操作租户边界；跨租户自动负向可作为后续独立补充，不阻塞当前开发交付。 |
| 发布 | 不构成上线通过；真实业务主链和用户手工验收证据仍未回填。 |
| 运维 | 不授权启动、停止或重启服务；本轮只更新任务文档。 |

## Decision

- decision: `ACCEPT_AND_SPLIT`
- accepted_scope: 当前任务文档、测试计划和验证报告删除“必须可见租户管理菜单”的 blocker；记录测试租户登录即限定本轮业务操作租户。
- split_scope: AC-30 的跨租户自动负向验证保留为后续独立验收或补充自动化项；如果用户只执行当前测试租户手工验证，不因缺租户管理菜单而阻塞开发交付。
- rationale: 租户管理菜单是环境管理权限，不是证明业务操作租户归属的必要条件；登录租户已经决定前端会话和后续业务请求的租户范围。

## Required Approval

- requester_approval: 已获得；用户明确纠正该前置并要求继续。
- additional_approval: 若后续需要创建第二租户、修改租户配置、跨租户写入或读取隔离数据，仍需另行明确授权。

## Downstream Actions

1. 更新 `task.md`，将当前状态改为“开发完成，等待测试租户手工验收证据”，不再列“租户管理菜单账号”为解除条件。
2. 更新 `test-plan.md`，把第二租户从当前固定前置改为 AC-30 自动负向的可选补充前置。
3. 更新 `verification-report.md` 和 `task-state.json`，保留 P11 未完成事实，但替换错误 blocker。
4. 在 `execution-log.md` 追加 Pass 52，记录本次口径修正、未启停服务、未写业务数据。

## Blockers And Next Action

- development_blocker: 无；T1-T10 产品开发交付已完成。
- validation_blocker: 用户手工测试租户真实业务主链、附件、追溯和清理证据尚未回填；当前 8082/48082 在本轮只读检查时未监听。
- next_action: 用户按更新后的测试租户手工验收执行单验证；收到证据后再更新 P11 结论。
