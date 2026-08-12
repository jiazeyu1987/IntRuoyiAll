# Backend API Evidence

## Scope

- Endpoint: `GET /mes/pro/feedback/frontline/device-account/active-orders`。
- Service: 正式报工提交授权新增选中活跃订单、路线、路线工序和工序一致性校验。
- Behavior: 订单数量 100、完成数量 200 仍允许提交，不限制、不截断、不自动拆单。

## API And Data Contract

- 候选接口返回 `workOrderId/workOrderCode/workOrderName/productId/productCode/productName/routeId/routeCode/routeName/quantity/joinedAt`。
- 客户端显式提交同一 `workOrderId` 到 feedback 与 process-pool 上下文；两份不一致即失败。
- 授权在同一事务中锁定当前生产组长的活跃订单，并核对正式路线工序快照。

## Auth Permissions Validation And Errors

- 身份使用当前登录设备账号，通过设备上下文解析唯一生产组长，不信任客户端组长 ID。
- 候选只返回该生产组长正式活跃订单；失效订单、其他组长订单、路线或工序不匹配均明确失败。
- 重复幂等提交先返回原成功回执，不因订单后续移出活跃池破坏历史幂等恢复。
- 不吞异常、不默认选择第一张订单、不回退到运行配置隐式订单。

## Required Config Services Fixtures Migrations

- 依赖现有设备账号、生产组长负责范围、活跃订单、工艺路线工序快照和正式提交事务。
- 无新增配置、数据库迁移或兼容字段。
- 单元测试使用既有 Mockito fixture；写入型真实 E2E 需要任务专用业务夹具。

## BDD Scenarios

- BDD: 只查询负责组长订单 -> Given 当前设备账号有负责组长和活跃订单 When 查询候选 Then 只返回该组长订单。
- BDD: 200 件归属选中订单 -> Given 选中订单 A 和其正式工序 P When 提交 200 件 Then 报工和工序池事件归属 A 并成功。
- BDD: 无效订单工序失败 -> Given 订单失效或工序不属于订单路线 When 提交 Then 失败且不创建记录。
- BDD: 重复提交恢复 -> Given 相同幂等键已成功 When 订单后来移出活跃池并重试 Then 返回原回执。

## RED And GREEN

- RED: 控制器测试首次因生产 active-orders endpoint 不存在而失败。
- RED: 提交服务测试首次因缺少 active-order 授权方法而失败。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesFrontlineActiveOrderControllerTest,MesProFrontlineFeedbackSubmitServiceTest,MesFrontlineSubmitAuthorizationTest,MesFrontlineSubmitIdentityTraceTest" test` -> PASS，23 tests。
- GREEN: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS，30/30 modules。

## Contract Integration Verification

- 前端静态合同锁定请求路径和提交字段来源。
- 后端测试锁定 200 件不封顶、失效订单拒绝、订单工序一致性和幂等恢复。
- 本机后端运行于工作树端口 48100，actuator 为 UP；只读真实页面通过代理调用接口且无页面错误。

## Observability

- 沿用正式提交事务、业务异常和请求日志；新增校验失败直接返回现有业务错误，不产生默认成功记录。
- 组长页面使用现有提交事件和订单分配快照观察待调整数量，无新增隐式后台任务。

## Blockers And Downstream Skills

- Blocker: 写入型真实 E2E 缺少 `TLW_*` 业务夹具，未执行浏览器写入。
- 无 schema、发布或外部平台后续工作；前置补齐后只需运行既有真实闭环。
