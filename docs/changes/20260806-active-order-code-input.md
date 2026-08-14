# 20260806 活跃订单按订单号加入

## Request Summary And Source

- Source: 用户要求“添加活跃订单只需要输入订单号就可以，订单号对应的是生产工单的工单编号列，输入订单号的控件是输入下拉控件”。
- Scope: 继续任务 `doc/tasks/20260805-production-leader-active-order-pool-tab/`，改造新增活跃订单弹窗、前后端接口、合同测试和真实流程脚本。

## Current Baseline Reviewed

- 前端新增活跃订单弹窗当前要求 `生产订单ID`、`路线ID`、`路线版本ID`、`调拨单ID列表`。
- 后端 `POST /mes/pro/process-pool/team-leader/active-order/add` 当前接收 `workOrderId`、`routeId`、`routeVersionId`、`transferIds`，并用客户端路线值与有效排产对比。
- 调拨追溯已有只读端点 `/active-order/transfer-trace` 和页面表格，历史关联数据需要保留。

## Classification

- Requirement change: 用户可见表单字段收缩和 API 请求契约调整。
- API change: 新增候选搜索端点，新增活跃订单请求只保留 `workOrderId`。
- Test change: 前端静态合同、后端目标 JUnit、真实 E2E 脚本语法和前置数据门禁需要同步。

## Impact

- Product: 新增活跃订单只输入订单号，通过远程下拉选择真实候选，不允许自由文本提交。
- Data: 不新增迁移；后端从唯一有效排产工单解析正式路线和路线版本。
- API: 新增 `GET /mes/pro/process-pool/team-leader/active-order/candidates?keyword=...`，维护权限；`POST /active-order/add` 仅接收 `workOrderId`。
- UX: 候选接口增加 `eligible` / `ineligibleReason`，下拉中可加入候选排在最前并以绿色“符合要求”标识；暂不可加入候选保留原因，避免用户继续选择明显缺 QA/排产前置的数据。
- Tests: 失败分支覆盖未选候选、工单未确认、无有效排产、多条有效排产、排产路线缺失；调拨追溯从新增动作拆出，只读验证已有正式关联数据。
- Release risk: 旧前端或脚本若继续提交路线/调拨字段会被静态合同阻断；后端不再信任客户端路线参数。

## Decision

Accept.

## Required Approvals

- 用户已在本轮明确给出实施计划，且计划要求接受本次需求变更。

## Downstream Skill Reruns

- `backend-api-delivery`: 新候选端点、收缩新增请求、服务端路线解析和错误分支。
- `frontend-feature-delivery`: 单字段远程下拉、加载/错误/空选择状态、请求体契约。

## Blockers And Next Action

- Git 非空锁已恢复：本轮接手时 `.git/index.lock` 为 0 字节且超过 60 秒无活动 Git/Git-LFS 进程，已按项目门禁删除陈旧锁。
- Implementation: 后端候选接口、workOrderId-only 新增接口、服务端唯一有效排产路线解析、前端订单号远程下拉、旧路线/调拨输入删除、RRM 脚本候选选择与未选候选阻塞路径已完成。
- Verification: 后端目标 JUnit（29 tests）、前端静态合同、两个真实脚本语法检查、`pnpm ts:check`、evidence validators 和 `git diff --check` 已通过；相邻 RRM 静态合同已同步删除旧 route/version/transfer 新增依赖，候选 eligibility 排序/绿色展示合同已补齐。
- Blocker: 写入型真实 Playwright E2E 缺少测试租户、测试生产组长账号和任务自有 `TLW_*` 工单/工序/设备/签名夹具；当前任务保持 blocked，不执行 cleanup apply、提交或推送。
