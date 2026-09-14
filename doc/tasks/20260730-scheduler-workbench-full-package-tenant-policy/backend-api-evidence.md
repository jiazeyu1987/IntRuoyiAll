# Backend API Evidence：排产员工作台全量包优化

## Scope

- Endpoint: `/admin-api/mes/pro/scheduler-workbench/full-config/export`
- Endpoint: `/admin-api/mes/pro/scheduler-workbench/full-config/import`
- Service: `MesProSchedulerWorkbenchFullConfigPackageServiceImpl`
- Service: `MesProSchedulerWorkbenchManualReplanDataPackageServiceImpl`

## API And Data Contract

- 全量包继续保留岗位配置包、角色配置包、路线配置包、手动重排数据包和用户角色绑定。
- 新增策略设置子包，承载 `mes.scheduler-workbench.policy-settings` 对应的正式排产策略设置。
- 手动重排数据导入时，租户型 DO 必须写入当前 `TenantContextHolder` 目标租户，而不是保留源租户编号。

## Auth And Error Behavior

- 权限沿用 Controller 现有 `mes:pro-scheduler-workbench:query` 和 `mes:pro-scheduler-workbench:update`。
- 缺少策略子包、策略格式非法、目标用户缺失、目标角色缺失和手动重排字段缺失必须 fail fast。
- 不引入 fallback、mock 成功、默认成功或吞异常。

## BDD

- `BDD: 全量包包含策略设置 -> Given 源租户保存了排产策略设置 / When 导出排产员工作台全部数据包 / Then 数据包包含正式策略设置子包，导入后目标租户返回相同策略设置。`
- `BDD: 跨租户导入重写租户上下文 -> Given 手动重排数据包内对象携带源租户 tenantId / When 在测试租户导入全部数据包 / Then 所有租户型对象写入测试租户 tenantId，不保留源租户 tenantId。`

## Validation

- RED/GREEN 使用目标 JUnit 覆盖导出合同、导入合同、缺包 fail-fast、用户角色 fail-fast 和租户重写。
- Contract verification 使用全量包 JSON 字段断言、导入响应计数字段断言和 mapper captor 断言。

## Verification

- `RED: mvn -pl yudao-module-mes -am "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchManualReplanDataPackageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL`，`getPolicySettingsCount()` 缺失。
- `GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchManualReplanDataPackageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS`，11 tests，0 failures，0 errors。
- 成功路径：导出包包含 `policySettings`；导入包调用 `savePolicySettings(...)` 并返回 `policySettingsCount=1`。
- 失败路径：缺少 `policySettings` 抛 `排产员工作台全量配置包缺少策略设置`；缺少手动重排包仍抛原有 fail-fast 错误。
- 租户边界：`MesProWorkOrderDO` 样例从源 `tenantId=100` 导入时被写成当前目标 `tenantId=200`。

## Blockers

- 无后端实现 blocker。
