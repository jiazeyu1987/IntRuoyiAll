# 生产放行在芋道源码环境验证

## Request Summary

- change_id: `20260817-production-release-yudao-source-validation`
- source: 用户于 2026-08-17 明确要求“在芋道源码里进行验证”。
- requested_change: 终止此前“仅由用户手工验收”的责任安排，由 Agent 在本机 `int_main` 运行态先执行 `芋道源码` 环境验证。

## Current Baseline Reviewed

- 任务目录：`doc/tasks/20260814-production-release-flow-implementation`。
- 产品与验收范围：`prd.md`、`development-plan.md`、`test-plan.md`；P1-P10 已完成，P11 仍要求真实多账号、三组任务自有订单、四附件、签核、追溯和清理。
- 运行态：`8081/48081` 均可访问；后端运行 Jar `backend-runtime-control-20260817-082151.jar` 生成于生产放行融合之后，内嵌 MES Jar 含 73 个生产放行相关条目，抽查六个核心类全部存在。
- 登录身份：本机默认身份标签为 `芋道源码/admin`；未提供七个独立业务账号、第二测试租户、三组订单 fixture、四附件、签核证据或清理计划。
- 强制边界：`docs/e2e-rules.md#官方登录前置与-admin-only-全量验证门禁` 禁止仅用 `芋道源码/admin` 执行写入型、多用户、签名、放行、发布或需数据清理的 E2E。

## Classification

- type: 验收责任与目标环境变更。
- product_scope_change: 否。
- behavior_change: 否。
- test_scope_change: 是；Agent 恢复验证执行责任，但只能先执行 `芋道源码/admin` 允许的只读路径。

## Impact Analysis

| 范围 | 影响 |
| --- | --- |
| 产品 | AC-01 至 AC-34 不变，不新增业务功能。 |
| 设计 | 无架构、接口或交互设计变更。 |
| 数据 | 当前只读验证不写业务数据；完整 P11 仍需独立测试租户和任务自有可清理数据。 |
| API | 只允许页面触发的只读请求和健康检查；不得用 API-only 替代页面验收。 |
| 测试 | 先验证登录、生产组长、工作任务和表单追溯入口；多账号写入主链继续阻塞。 |
| 发布 | 不构成上线或远程环境授权，P11 未通过前不得宣称全任务完成。 |
| 运维 | 不启停服务；只核对当前本机进程、运行包来源和健康状态。 |

## Decision

- decision: `SPLIT`
- accepted_scope: 在当前本机 `int_main` 运行态使用 `芋道源码/admin` 执行官方登录和三个生产放行相关入口的只读页面验证。
- blocked_scope: 组长提交、PQC 通过/拒绝、四附件上传、管理者签核放行、跨角色/跨租户写入验证和数据清理。
- rationale: 用户指定了目标环境，但当前只有 admin 基线身份；完整 P11 写入范围缺少合规测试租户、账号、fixture 和清理前置，不能因环境指定而绕过数据安全门禁。

## Required Approval

- requester_approval: 已获得 `芋道源码` 只读验证授权。
- additional_approval: 完整 P11 需要提供并确认非 admin 基线的本机测试租户、七个独立业务账号、第二租户隔离账号、三组任务自有订单、四附件、签核证据和页面清理计划。
- prohibited_approval_path: 不接受把 `芋道源码/admin` 基线数据改造成临时写入 fixture 的授权替代；应提供正式测试租户和任务自有数据。

## Downstream Actions

1. 使用官方 `scripts/preflight/login-preflight.mjs` 在真实前端页面执行只读登录和入口验证。
2. 更新 `execution-log.md`、`verification-report.md`、`task.md` 和 `task-state.json`，记录运行态 blocker 已解除、只读结果和剩余写入 blocker。
3. P11、P11-AC1、P11-AC2 和 AC-01 至 AC-34 在真实多账号写入证据齐备前保持 `BLOCKED`。
4. 前置齐备后，由独立 tester 复验真实页面、只读最终状态和清理证据。

## Blockers And Next Action

- runtime_blocker: 已解除；当前 48081 运行包包含生产放行核心实现，8081/48081 均返回 HTTP 200。
- validation_blocker: 当前只确认 `芋道源码/admin`，缺少完整 P11 所需的合规测试租户、多账号、业务 fixture、附件、签核和清理前置。
- next_action: 保留已通过的只读页面证据；等待正式测试租户与完整前置后执行 TC-13，不对 admin 基线产生业务写入。
