# Verification Report：排产员工作台全量数据包跨租户恢复优化

## Summary

- 后端全量包已补策略设置子包，导入时调用正式策略保存链路。
- 手动重排数据包导入时会把 `TenantBaseDO` 行重写为当前目标租户，避免保留源租户 `tenantId`。
- 前端导入成功摘要已展示策略设置导入数量。

## Commands

- `mvn -pl yudao-module-mes -am "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchManualReplanDataPackageServiceTest" test` -> FAIL，上游 reactor 模块无匹配测试；已按项目门禁改用 `surefire.failIfNoSpecifiedTests=false`。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchManualReplanDataPackageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> RED FAIL，生产 VO 缺 `getPolicySettingsCount()`。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchManualReplanDataPackageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> GREEN PASS，11 tests，0 failures，0 errors。
- `node tests/e2e/mes-scheduler-workbench-import-timeout-static.spec.js` -> RED FAIL，缺 `policySettingsCount: number`。
- `node tests/e2e/mes-scheduler-workbench-import-timeout-static.spec.js` -> GREEN PASS。
- `pnpm ts:check` -> PASS。

## Result

- 目标行为通过定向后端 JUnit、前端静态契约和 TypeScript 检查。
- 未执行真实跨租户浏览器导入，因为本任务未获得写入测试租户真实数据授权；当前证据为代码契约级优化和定向测试。

## Residual Risks

- 当前实现按原 ID upsert，不会删除目标租户包外多余数据；若需要“完全镜像恢复”，仍需另行定义清空/对账策略。
- 用户角色绑定仍要求目标环境存在相同用户名；该 fail-fast 行为保持不变。
