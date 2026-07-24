# Backend API Evidence

## Scope

- 服务范围：批次执行任务生成链路，重点为 `MesProEdhrBatchExecutionServiceImpl` 从工艺路线冻结快照解析 `formBindings` 并生成批次执行任务。

## API And Data Contract

- 批次执行任务必须继承路线发布快照中的表单槽位配置。
- `instanceScope=PROCESS` 表示按工序生成独立表单实例。
- `instanceScope=BATCH_SHARED` 表示共享表单实例，必须携带稳定 `sharedFormKey`。
- 填写人/填写规则、必填策略和可填写范围必须来源于发布快照，不得被当前草稿配置或默认逻辑覆盖。

## Auth, Validation, Error Behavior

- 本任务不新增接口、权限或数据库结构。
- 缺少必要表单模板、共享标识或非法槽位配置时应显式失败，不引入默认成功、吞异常或降级。

## Required Config, Services, Fixtures, Migrations

- 不需要新迁移。
- 目标验证优先使用后端单元/契约测试，不依赖远程服务或生产数据。

## BDD Scenarios

- BDD: 冻结表单槽位生成批次执行任务 -> Given 已发布路线快照包含工序独立和批次共享表单槽位 / When 创建批次执行任务 / Then 任务保留填写人/填写规则和共享配置
- BDD: 发布后修改当前草稿不影响批次执行 -> Given 已发布路线快照存在 / When 当前草稿配置变化 / Then 创建批次执行仍使用冻结快照

## RED

- RED: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_usesFrozenRouteVersionFormBindingsInsteadOfCurrentDraft test` -> FAIL，`openOrCreate` 生成的任务使用 `CURRENT_DRAFT_1`，未使用发布快照中的 `FB_<routeProcessId>_1`。

## GREEN

- GREEN: `mvn -pl yudao-module-mes -DskipTests compile` -> PASS。
- GREEN: `node src\test\js\edhr-route-form-slot-frozen-runtime-static.spec.cjs` -> PASS。
- GREEN: `mvn -Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_usesFrozenRouteVersionFormBindingsInsteadOfCurrentDraft surefire:test` -> PASS，目标测试类隔离编译后目标方法通过。

## Contract Verification

- 新建批次：任务构建改为在批次对象写入 `routeVersionId` 和 `routeSnapshotJson` 后调用冻结快照感知构建方法。
- 质量拒收重执行：同样使用新 active route version 快照构建任务。
- 冻结快照：继续读取 `configSnapshots.batchUseConfigs[].formBindings`，不新增 fallback 到当前草稿配置。

## Observability

- 不新增日志；如现有任务生成失败路径缺少显式异常，将保持 fail-fast 行为。

## Blockers

- 标准 Maven `test` 生命周期被无关旧测试源码的编译错误阻塞；目标 JUnit 通过手动隔离编译和直接 surefire 执行完成验证。
