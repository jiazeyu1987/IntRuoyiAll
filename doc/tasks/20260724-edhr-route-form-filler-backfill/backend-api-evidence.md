# Backend API Evidence

## Endpoint, Service, Job, or Handler Scope

- Endpoint: `GET /admin-api/mes/pro/edhr-batch-execution/get?id={id}`。
- Service: `MesProEdhrBatchExecutionServiceImpl`。

## API Contract and Data Contract

- Existing response field: `EdhrBatchExecutionTaskRespVO.fillableUsers`。
- Expected data contract: 动态表单任务应使用路线工序表单绑定的填写人配置回填 `fillableUsers`，不新增接口字段。

## Auth, Permissions, Validation, and Error Behavior

- 不改变认证、权限、校验或错误映射。
- 不吞异常、不返回默认成功、不引入 fallback。

## Required Config, Services, Fixtures, and Migrations

- Required fixture: 后端测试构造路线工序动态表单绑定及填写人配置。
- Migrations: 无。

## BDD Scenarios

- BDD: 动态表单任务显示工艺路线绑定填写人 -> Given 工艺路线工序绑定的损耗单配置了填写人, When 用户打开批次执行详情, Then 对应损耗单任务 `fillableUsers` 必须返回该配置人员，供右侧单据卡片显示。
- BDD: 主生产表任务填写人逻辑不被破坏 -> Given 主生产表或已有工作任务已有填写人来源, When 批次执行详情组装任务列表, Then 仍优先使用既有工作任务或任务分配规则解析填写人，不被动态表单绑定回填覆盖。

## RED Command and Expected Failure

- Pending.

## GREEN Command and Passing Result

- Pending.

## Contract or Integration Verification

- Pending.

## Observability Touchpoints

- No new logging required; this is response data mapping.

## Blockers and Downstream Skill Needs

- Existing backend files were already modified before this task; this task will preserve unrelated changes.
