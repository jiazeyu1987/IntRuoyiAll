# Backend API Evidence

## Scope

Scope: `MesProEdhrBatchExecutionServiceImpl#openTask` 的动态路线表单任务打开响应，尤其是 `buildTaskOpenResp -> resolveVisibleAssistRows` 返回给前端的 `executionPageQuery.assistRows`。

## Contract

Contract: 当批次任务是动态路线表单任务（`ROUTE_FORM`、无 `batchRecordReportId`、有 `formBindingKey/formTemplateId`）时，打开响应必须保留 `formBindingKey/formTemplateId/formTemplateVersionId/formCenterInstanceId`，并从任务冻结的 FormCenter 模板版本读取 `edhrAssistRows`。传统批记录任务继续读取 execution snapshot；两条链路不得混用。

## Validation

Validation: 后端仍使用 `requireOpenWorkTaskContext`、`requireTaskFillAbility` 和 `resolveAssistUserIdForOpenTask` 校验所选填写人。动态路线表单缺少完整 FormCenter 上下文、模板版本不存在或模板 ID 不一致时，继续 fail-fast 抛出 `PRO_EDHR_BATCH_EXECUTION_TASK_CONTEXT_REQUIRED`，不返回默认成功或空任务上下文。

## BDD

BDD: 表单槽位候选切换到损耗单 -> Given eDHR 填写页切换填写人弹窗展示 `张可莹` 且载体为 `工艺路线表单槽位 · 损耗单` When 用户选择该候选 Then 后端应打开该候选对应的 FormCenter 损耗单任务，不得进入批记录路线唯一性解析。

BDD: 批记录和表单槽位边界保持独立 -> Given 同一工序存在 `MAIN` 批记录表单和 `LOSS_REPORT` 表单槽位 When 选择表单槽位候选 Then 打开响应必须包含 FormCenter 上下文且 `executionId` 可为空，不得调用传统批记录 `openOrCreateByContext`。

BDD: 后端错误不被吞掉 -> Given 所选任务缺正式表单槽位上下文 When 后端无法打开 Then 返回正式错误码，不吞异常、不默认成功、不静默切回当前填写人。

## RED

RED: 旧 HEAD 源码静态断言 -> FAIL as expected，旧 `resolveVisibleAssistRows` 缺少 `isDynamicRouteFormTask(task)` 分流和 `resolveDynamicRouteFormVisibleAssistRows`，会继续读取传统批记录 execution snapshot。

## GREEN

GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> PASS。

GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_dynamicRouteFormFillerSwitchUsesTemplateAssistRowsWithoutExecutionRoute+openTask_returnsDynamicRouteFormContextWithoutBatchReportExecution+previewTask_returnsDynamicRouteFormTemplatePreviewWithoutBatchReportSource" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 2。

GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS。

## Verification

Verification: 后端 JUnit 断言动态损耗单任务打开后 `executionId == null`、`formCenterInstanceId` 保持、`assistUserId` 为所选填写人、`assistRows` 按责任范围过滤，并验证 `singleExecutionService.openOrCreateByContext` never called。

## Observability

打开成功仍通过 `recordOperationAudit("BATCH_EXECUTION_TASK", ..., "OPEN", ...)` 记录操作审计，审计 payload 包含 `executionPageQuery`，可追溯 `formBindingKey/formCenterInstanceId/assistUserId/assistRows`。

## Blockers

Blockers: 未执行真实页面 E2E；需融合回 `int_main` 后基于可用本地运行态和正式样本复验。无 schema、迁移或配置变更。
