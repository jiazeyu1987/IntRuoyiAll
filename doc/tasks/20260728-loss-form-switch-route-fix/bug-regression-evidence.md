# Bug Regression Evidence

## Bug Summary

“切换填写人”弹窗选择 `张可莹`（`工艺路线表单槽位 · 损耗单`）时，系统应该切换并打开该候选对应的损耗单表单槽位任务，但当前报错 `eDHR 批次缺少唯一批记录路线`。

## Expected Behavior

表单槽位候选必须使用任务冻结的 `formBindingKey/formTemplateId/formTemplateVersionId/formCenterInstanceId` 打开 FormCenter 动态表单上下文。损耗单表单槽位不得走批记录报表唯一路线解析，也不得用批记录表单、工序开始或 `MAIN` 槽位替代。

## Reproduction

- 用户路径：eDHR 填写页 -> 打开“切换填写人” -> 选择 `张可莹`（`工艺路线表单槽位 · 损耗单`）。
- RED: 旧源码静态断言 -> FAIL，旧 `MesProEdhrBatchExecutionServiceImpl` 缺少动态路线表单辅助行分流，会继续读取传统批记录 execution snapshot。

## Root Cause

`openTask` 已能识别动态路线表单任务并保留 FormCenter 上下文，但 `buildTaskOpenResp -> resolveVisibleAssistRows` 对辅助填写行仍统一读取 `task.executionId` 对应的传统批记录 execution snapshot。动态损耗单表单槽位没有传统批记录 execution 路线，切换填写人时因此落入批记录路线唯一性错误。

## Regression Test

- 更新 `IntRuoyiBackend/yudao-module-mes/src/test/js/mes-edhr-assist-filler-switch-snapshot-static.spec.cjs`，锁定动态路线表单损耗单必须从 FormCenter 模板解析辅助行。
- 更新 `MesProEdhrBatchExecutionServiceTest#openTask_dynamicRouteFormFillerSwitchUsesTemplateAssistRowsWithoutExecutionRoute`，覆盖无批记录 execution 路线时仍能打开动态损耗单候选，并返回 `formCenterInstanceId`、`assistUserId` 和过滤后的 `assistRows`。

## RED

RED:

`git show HEAD:IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java` 源码静态检查 -> FAIL as expected，旧源码没有 `resolveDynamicRouteFormVisibleAssistRows` 和 `isDynamicRouteFormTask(task)` 分流。

## GREEN

GREEN:

- `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> PASS。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_dynamicRouteFormFillerSwitchUsesTemplateAssistRowsWithoutExecutionRoute" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_dynamicRouteFormFillerSwitchUsesTemplateAssistRowsWithoutExecutionRoute+openTask_returnsDynamicRouteFormContextWithoutBatchReportExecution+previewTask_returnsDynamicRouteFormTemplatePreviewWithoutBatchReportSource" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS。

## Verification

Verification: 静态合同、后端定向 JUnit 和 MES reactor compile 均通过；验证范围覆盖截图症状的动态损耗单表单槽位打开链路，以及相邻动态表单打开/预览路径。

## Risk And Regression Scope

风险集中在 eDHR `openTask` 返回的辅助填写行投影。修复只对 `isDynamicRouteFormTask` 分流，不改变传统 `MAIN` 批记录表单的 execution snapshot 读取，也不改变表单槽位打开、跳过、预览和 FormCenter 实例创建规则。

## Blockers

真实页面 E2E 仍待融合后按可用本地运行态和正式样本复验；本次已用后端 JUnit 覆盖业务打开路径和静态合同覆盖前后端契约。

