# Backend API Evidence

## Scope

- Endpoint：`GET /mes/pro/feedback/frontline/device-account/pqc/active-orders`。
- 本任务不改变 API shape、权限、持久化、异常或数据库 schema；只审计并锁定现有全局 ACTIVE 来源。

## API And Data Contract

- `MesFrontlinePqcContextServiceImpl.listActiveOrders()` 必须调用 `MesProcessPoolActiveOrderMapper.selectActiveList()`。
- `selectActiveList()` 只按 `activeStatus=ACTIVE` 查询，不按当前登录人或单个 `leaderUserId` 过滤。
- 服务按 `workOrderId + routeId` 去重并返回最新 `joinedAt`，缺正式订单、产品或路线数据时继续明确失败。

## Auth Validation And Error Behavior

- Permission unchanged：`mes:pro-feedback:query`。
- 正式 ACTIVE 集合为空时继续抛出 `PRO_FRONTLINE_PQC_ACTIVE_ORDER_EMPTY`。
- 不增加 fallback、空成功或其它订单来源。

## BDD And Verification

- BDD：多个生产组长的 ACTIVE 订单共享同一全局集合；接口失败或空集合不得降级。
- Existing GREEN evidence：`MesFrontlinePqcContextServiceTest.shouldListActiveOrdersFromUnifiedActiveOrderAuthority`。
- Contract validation：pending。

## Config Fixtures Migrations Observability

- Config：无变化。
- Fixtures：无新增。
- Migrations：无。
- Observability：沿用现有统一异常映射；无新增日志。

## Blockers And Downstream Needs

- 相邻在途路线版本任务拥有后端 PQC context 服务和测试文件；本任务不修改这些文件。

