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

- 本任务实现阶段曾通过定向后端 JUnit（11 tests，0 failures，0 errors）、前端静态契约和
  TypeScript 检查。
- 2026-07-30 21:06 +08:00 基于最新共享工作区复验时，后端在目标测试执行前被并行任务的
  `MesFrontlineWorkstationPostRouteBindingSourceTest.java` 阻塞：其引用的
  `MesFrontlineWorkstationPostRouteBindingSource` 生产类不存在，`yudao-module-mes:testCompile`
  失败。该失败不来自本任务排产数据包代码。
- 2026-07-30 21:19 +08:00 并行任务补齐生产类后复跑同一目标 Maven -> PASS，
  11 tests、0 failures、0 errors；临时编译 blocker 已解除。
- 最新前端静态契约、`pnpm ts:check` 和前后端 evidence validators 仍通过。
- `task-closeout-cleanup` preview/apply 均通过，正式任务记录和两份 evidence 保留，无删除项或阻塞项。
- 最终普通推送已通过，`origin/int_main=79040df4bc9e1ab9c4b437113a078602b17394df`，
  本地与远端 `ahead/behind=0/0`。
- 未执行真实跨租户浏览器导入，因为本任务未获得写入测试租户真实数据授权；当前证据为代码契约级优化和定向测试。

## Residual Risks

- 本任务实现被混入并行基线提交 `67282a86`，无法形成纯净独立实现提交；已保留提交边界和复验证据，
  不执行历史改写。
- 曾出现的 GitHub 连接重置和 `non-fast-forward` 推送 blocker 已解除；最终采用普通 push，
  未执行 force push、reset、rebase 或历史改写。
- 当前实现按原 ID upsert，不会删除目标租户包外多余数据；若需要“完全镜像恢复”，仍需另行定义清空/对账策略。
- 用户角色绑定仍要求目标环境存在相同用户名；该 fail-fast 行为保持不变。
