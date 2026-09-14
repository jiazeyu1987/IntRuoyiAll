# Backend API Evidence

## Scope

- Endpoint: `GET /form-center/template-pool`。
- Service: `FormCenterRuntimeServiceImpl#toTemplateResp`。
- Data object: `FormTemplateVersionDO`。

## API And Data Contract

- `FormCenterTemplateRespVO` 不暴露批记录绑定摘要。
- `FormTemplateVersionDO` 不映射批记录绑定字段。
- `toTemplateResp` 不读取或写入七个 `batchRecord*` 字段。
- 现有 FormCenter 模板字段和接口路径保持不变。

## Auth, Permissions, Validation, And Error Behavior

- 模板池既有租户过滤和权限边界不变。
- 后端不把缺少批记录绑定视为 FormCenter 模板错误。
- 不新增 BPM -> MES 查询、名称匹配、默认 `reportId` 或异常吞噬。

## Required Config, Services, Fixtures, And Migrations

- 不新增配置或外部服务。
- 不需要批记录数据夹具。
- 不需要新增迁移；错误新增迁移已从发布内容移除。

## BDD Scenarios

- `BDD: FormCenter 模板池不暴露批记录绑定契约 -> Given 查询当前租户模板池 / When 组装响应 / Then VO、DO 和 runtime 均不包含七个批记录绑定字段。`
- `BDD: 模板池不依赖 MES -> Given FormCenter 模板没有批记录数据 / When 查询模板池 / Then 正常返回当前模板，不查询或推断 reportId。`

## RED And GREEN

- `RED: mvn.cmd -pl yudao-module-bpm "-Dtest=FormCenterTemplateIndependenceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, VO、DO 和 runtime 仍包含七个错误字段。`
- `GREEN: mvn.cmd -pl yudao-module-bpm "-Dtest=FormCenterTemplateIndependenceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 2 tests。`

## Contract Or Integration Verification

- `FormCenterTemplateIndependenceContractTest#formCenterTemplateContractDoesNotExposeBatchRecordBinding` 反射检查 VO/DO 字段不存在。
- `FormCenterTemplateIndependenceContractTest#runtimeDoesNotMapBatchRecordBinding` 检查运行态源码不包含相应 getter/setter 映射。
- 真实页面模板池加载成功并完成三个按钮验证。

## Observability Touchpoints

- 本次未新增日志。
- 接口失败继续由现有 BPM 请求日志和前端错误链路暴露。

## Blockers And Downstream Skill Needs

- 当前后端/API 无 blocker。
- 冗余数据库列是否物理清理由独立数据库迁移审计决定。
