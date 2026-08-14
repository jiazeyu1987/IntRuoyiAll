# 20260808 活跃订单加入仅保留 QA 与重复检测

## Request Summary

- Source: 用户明确要求“只要求有qa,重复检测，其他的限制都去掉”。
- Scope: 生产组长新增活跃订单候选搜索与新增接口。

## Current Baseline Reviewed

- 前端弹窗只提交 `workOrderId`，候选接口返回 `eligible/ineligibleReason`。
- 后端旧实现同时校验已确认工单、ERP 数量、有效排产唯一、产品正式路线绑定、ACTIVE 路线版本快照、启用工序、计划日期、PQC 数量规则等。

## Classification

- Requirement change / product behavior change.

## Impact

- Product: 订单加入门禁从“排产/路线/数量/PQC 完整前置”收敛为“有 QA + 重复检测”。
- API: `/active-order/candidates` 和 `/active-order/add` 路径保持不变，请求仍只提交 `workOrderId`。
- Data: 活跃订单仍需要写入正式 `routeId/routeVersionId/routeProcessId/processId`，来源改为产品已发布 QA 规程上下文。
- Tests: 后端服务测试需覆盖放宽准入、缺 QA 拒绝和重复检测。
- Release: 无数据库迁移。

## Decision

Accepted. 用户为当前任务直接提出产品口径变更，按该口径执行后端准入规则调整。

## Required Approvals

- No additional approval required for this task scope.

## Downstream Skill Reruns

- `backend-api-delivery`

## Blockers And Next Action

- No blocker.
- Completed: 已按 accepted 变更完成后端实现与定向测试验证。
