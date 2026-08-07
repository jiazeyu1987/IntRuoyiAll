# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 生产组长活跃订单池显示正式路线名称与版本号，删除状态列和路线内部 ID 展示。
- Non-goals: 不修改活跃订单状态、加入/移出流程、其它工作台模块或业务数据。

## Requirements And Acceptance IDs

- AC-1: 表头为“路线名称”和“版本号”。
- AC-2: 行数据来自正式 `routeName` 与 `routeVersionNo` 字段。
- AC-3: 不显示“路线ID”“路线版本ID”“状态”列，也不显示对应内部 ID。
- AC-4: 加入时间、ERP 生产数量和移出操作保持不变。

## UI Entry Points, Routes, Components, And Owned Files

- Route: `/mes/pro/process-pool/production-leader`。
- Component: `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`。
- API contract: `IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts`。
- Backend contract: `MesTeamLeaderActiveOrderRespVO` 及其 controller/service 投影。
- Tests: 聚焦前端静态合同与后端 controller/service 测试。

## API Contracts And Data States

- Existing: `id/workOrderId/routeId/routeVersionId/erpFixedQuantitySnapshot/activeStatus/joinedAt`。
- Required: 新增正式只读显示字段 `routeName`、`routeVersionNo`；保留内部 ID 供业务逻辑使用但不在表格展示。
- Loading/empty/error: 复用现有活跃订单加载、空表和错误处理，不新增降级分支。

## BDD Scenarios

- BDD: Given 活跃订单关联正式路线与版本 When 生产组长查看活跃订单池 Then 页面显示路线名称和版本号且不显示状态与两个内部 ID 列。
- BDD: Given 正式路线显示字段无法解析 When 列表响应生成 Then 不由前端回退显示 ID。

## RED Command And Expected Failure

- RED: `node tests\e2e\production-leader-active-order-route-labels-static.spec.js` -> FAIL，前端 `TeamLeaderActiveOrderRespVO` 缺少 `routeName/routeVersionNo`。

## GREEN Command And Passing Result

- GREEN: pending。

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Responsive: 删除一列后表格在当前桌面宽度保持无横向遮挡；路线名称列使用现有溢出提示。
- Accessibility: 业务含义由明确表头表达，不以颜色或内部 ID 代替。
- Loading/empty/error: 保持现有 Element Plus loading、empty 与显式错误处理。
- Permission: 复用生产组长现有查询权限，不修改权限合同。

## E2E Or Component Verification Path

- 本机 `芋道源码/admin` 登录后进入生产组长活跃订单池，读取现有 5 条活跃订单做只读断言和截图。

## Blockers And Follow-Up Skills

- Blockers: 暂无。
- Follow-up skills: 若确认需要扩展后端响应，将使用 `backend-api-delivery` 完成正式接口字段与测试。
