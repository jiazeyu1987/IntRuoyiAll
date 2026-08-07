# Backend API Evidence

## Endpoint, Service, Job, Or Handler Scope

- Endpoint: `GET /admin-api/mes/pro/process-pool/team-leader/active-order/list`。
- Service: `MesTeamLeaderActiveOrderService.listActiveOrders` 的正式只读投影。
- Scope: 为每条活跃订单补充路线名称和版本号；不修改写入、移出、状态或权限行为。

## API Contract And Data Contract

- Existing identity: `routeId`、`routeVersionId` 继续保留为内部业务身份。
- Added display fields: `routeName: string`、`routeVersionNo: string`。
- Formal sources: `mes_pro_route.name` 与 `mes_pro_route_version.version_no`，按活跃订单冻结的精确 ID 批量读取。
- No fallback: 路线、版本缺失或版本不属于该路线时明确失败，不用 ID、空串或当前 active 版本代替。

## Auth, Permissions, Validation, And Error Behavior

- Permission remains `mes:pro-process-pool-team-leader:query`。
- Leader identity remains `SecurityFrameworkUtils.getLoginUserId()`，客户端不能指定。
- Missing route: 使用正式 `PRO_ROUTE_NOT_EXISTS` 错误。
- Missing/mismatched route version: 使用正式 `PRO_ROUTE_VERSION_NOT_EXISTS` 错误。

## Required Config, Services, Fixtures, And Migrations

- Config/migration: 无。
- Services: 现有 active-order mapper、route mapper、route-version mapper。
- Fixture: JUnit mock；真实页面复用本机现有 5 条活跃订单做只读验证。

## BDD Scenarios

- BDD: Given 活跃订单关联正式路线和版本 When 查询列表 Then 响应包含准确 `routeName` 和 `routeVersionNo`。
- BDD: Given 路线或精确版本缺失/错配 When 查询列表 Then 接口明确失败且不返回 ID 替代文案。
- BDD: Given 当前用户无查询权限 When 调用端点 Then 保持现有权限拦截。

## RED Command And Expected Failure

- RED: 前端跨层静态合同 -> FAIL，后端 Response VO 尚无 `routeName/routeVersionNo`。
- RED: 聚焦 Maven 测试 -> FAIL，`MesTeamLeaderActiveOrderRow` 尚不存在；证明服务层正式读模型未实现。

## GREEN Command And Passing Result

- GREEN: 编译并运行 `MesTeamLeaderActiveOrderRouteLabelsFocusedHarness` -> PASS；验证正式路线名称/版本号返回、正式路线缺失失败、版本与路线错配失败。
- GREEN: 前端跨层静态合同 -> PASS；验证 Response VO 字段、Controller 投影及页面消费字段一致。

## Contract Or Integration Verification

- 后端聚焦行为验证、前端静态合同和类型检查已通过。
- 标准 MES Maven 回归被非本任务的既有 Lombok/生成类编译缺口阻断，未获得虚假通过结论。
- 真实页面只读验证仍等待共享 `48081` 加载本任务补丁 Jar。

## Observability Touchpoints

- 复用现有接口异常响应和前端列表加载错误提示；不新增吞异常或默认成功。

## Blockers And Downstream Skill Needs

- Blockers: 共享 `48081` 被并行任务恢复为旧 Jar；全量 MES 编译存在非本任务既有缺口。
- Downstream: `frontend-feature-delivery` 消费新增字段并完成页面展示。
