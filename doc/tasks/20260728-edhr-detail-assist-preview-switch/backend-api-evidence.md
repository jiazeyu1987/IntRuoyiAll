# Backend API Evidence: eDHR 未打开主生产表预览快照

## Scope

增强现有 `GET /mes/pro/edhr-batch-execution/{batchExecutionId}/task/{taskId}/preview` 的服务行为，不新增接口、不改变响应结构。

## API / Data Contract

- 未打开主生产表：`formViewModel.executionSnapshotJson` 使用正式批记录 runtime snapshot，包含 `fields` 与 `assistRows`。
- 无辅助配置：`assistRows` 返回空数组，不返回默认成功辅助数据。
- 动态表单：继续使用 FormCenter 预览，不混用批记录报表来源。

## Validation

- 继续校验批次可见性、正式批记录报表存在性、Jimu report JSON 存在性和 cell rule 治理状态。
- 缺少正式来源时 fail-fast，不返回空的默认成功快照。

## Auth / Validation / Error Behavior

- 继续沿用 `previewTask` 的批次可见性校验。
- 批记录报表缺失、Jimu 报表缺失或 cell rule 未确认继续 fail-fast，不吞异常。
- 未新增 schema、权限或迁移。

## Owned Files

- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordRuntimeSnapshotSupport.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceTest.java`

## BDD Scenarios

- BDD: 未打开主生产表预览包含辅助快照 -> Given 正式报表配置辅助行, When 调用 `previewTask`, Then 返回 `executionSnapshotJson.assistRows`。
- BDD: 无辅助配置返回空数组 -> Given 正式报表没有辅助行, When 调用 `previewTask`, Then `assistRows` 为 `[]`。
- BDD: 动态表单来源不混用 -> Given 当前任务是动态表单, When 调用 `previewTask`, Then 不调用 Jimu 报表 runtime snapshot。

## RED / GREEN

- RED: `mvn -o -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsUnopenedBatchRecordWithExecutionSnapshotAssistRows" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，未打开主生产表 preview 缺少 `executionSnapshotJson`。
- GREEN: 同命令 -> PASS，1 个目标 JUnit 通过。
- GREEN: `mvn -o -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，97 个执行记录服务回归通过。
- GREEN: `mvn -o -pl yudao-module-mes -am "-DskipTests" compile` -> PASS，MES reactor compile 通过。
- GREEN: `node src/test/js/mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> PASS，执行详情快照与 active 上下文隔离静态合同通过。

## Verification

- `previewTask` 目标 JUnit 验证有辅助行和无辅助行两种正式批记录报表快照。
- 执行记录服务完整测试类验证 runtime snapshot、active context、审批快照和 task/workstation 上下文隔离。

## Observability / Blockers

## Blockers

- 无新增日志或配置。
- 当前无 MES 编译或目标 JUnit blocker。
