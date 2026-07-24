# Bug Regression Evidence

## Bug Summary

批次执行任务生成链路需要确认是否完整使用工艺路线发布后的表单槽位冻结配置。风险点是任务只继承表单模板与共享字段，而遗漏路线槽位里的填写人/填写规则、必填策略或被当前草稿配置覆盖。

## Expected Behavior

创建批次执行任务时，系统应从冻结 `routeSnapshotJson` 的 `formBindings` 中解析表单槽位配置，并把填写人/填写规则、`instanceScope`、`sharedFormKey`、必填策略和相关上下文写入任务。

## Reproduction

- 目标测试构造发布冻结快照后，再把当前工序表单槽位改成 `CURRENT_DRAFT_*` 和 `CURRENT_SHARED_KEY`，随后调用 `openOrCreate` 断言任务仍应使用冻结快照中的 `FB_*` 和 `FROZEN_SHARED_KEY`。

## Root Cause

- `openOrCreate` 和 `reexecuteRejectedBatch` 原先在构造带 `routeVersionId/routeSnapshotJson` 的批次对象之前，就调用 `buildBatchTaskConfigs(route, routeProcesses)`。该调用只解析当前路线配置，绕过了已有的冻结快照解析方法，导致发布后草稿变更会影响新建批次任务。

## Regression Test

- 新增 `MesProEdhrBatchExecutionServiceTest#openOrCreate_usesFrozenRouteVersionFormBindingsInsteadOfCurrentDraft`。
- 新增 `src/test/js/edhr-route-form-slot-frozen-runtime-static.spec.cjs`。

## RED

- RED: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_usesFrozenRouteVersionFormBindingsInsteadOfCurrentDraft test` -> FAIL，实际任务 `formBindingKey=CURRENT_DRAFT_1`。

## GREEN

- GREEN: `mvn -pl yudao-module-mes -DskipTests compile` -> PASS。
- GREEN: `node src\test\js\edhr-route-form-slot-frozen-runtime-static.spec.cjs` -> PASS。
- GREEN: `mvn -Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_usesFrozenRouteVersionFormBindingsInsteadOfCurrentDraft surefire:test` -> PASS。

## Verification

- 目标 JUnit 回归测试验证发布快照和当前草稿冲突时，新建批次任务保留冻结表单槽位。
- 静态回归测试验证新建批次和质量拒收重执行批次均调用冻结快照感知构建方法。

## Risk And Regression Scope

- 影响批次执行任务生成、共享表单打开和填写任务分配。
- 不涉及数据库结构、远程服务、生产数据或发布操作。

## Blockers

- 标准 Maven `test` 生命周期存在无关旧测试源码编译阻塞；本任务目标验证已隔离完成。
