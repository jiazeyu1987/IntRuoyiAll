# Bug Regression Evidence

## Bug Summary

主端口 `int_main` 复验发现 eDHR 执行页仍调用 `/batch-record-cell-link/prefill` 并把未落库值注入本地草稿，违反“创建/打开执行记录时自动落库预填值”的正式语义。

用户补充截图后，继续确认另一个与症状一致的后端缺陷：传统批记录打开链路传空 `taskId`，导致执行记录可能不按当前粗洗工序批次任务隔离。

用户继续补充后，确认第三个缺陷：批记录表单的生产工单链接已带过去，但 `formBindings` 动态表单（损耗单、过程检验记录）的生产工单链接没有带过去。

## Expected Behavior

执行页只从执行详情已保存的 `detail.cellValues` / `cellValuesJson` hydrate 草稿状态；如果后端没有落库，前端不得调用 `/prefill` 兜底展示成功态。

## Reproduction

- RED: `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js` -> FAIL。

## Root Cause

执行页仍保留自动落库改造前的前端预填路径：加载 DRAFT 执行详情后调用 `BatchRecordCellLinkApi.getPrefill`，再把接口返回的 `prefills` 注入本地草稿。该路径会让页面显示未落库值，和后端创建/打开执行记录自动落库并由执行详情返回已保存值的新语义冲突。

传统批记录打开请求还存在后端上下文缺陷：`MesProEdhrBatchExecutionServiceImpl.buildOpenOrCreateExecutionReq(...)` 使用 `.setTaskId(null)`，和批次任务隔离门禁冲突。修复后改为 `.setTaskId(task.getId())`，让执行记录按当前 `batchExecutionId + batchTaskId` 查询/创建，并持久化当前任务 ID。

动态表单缺陷的根因是 `FORM_TEMPLATE_VERSION` 预填链路只接收 `workOrderId`，`PRODUCTION_WORK_ORDER.batchCode` 被读取为 `mes_pro_work_order.batch_code`。当 eDHR 批次执行本身有批号、但生产工单主表批号为空时，损耗单/过程检验记录无法拿到批号；传统批记录链路已使用执行上下文批号，所以表现为“批记录表单能带，动态表单不能带”。

## Regression Test

- `IntRuoyiFronted/tests/e2e/edhr-cell-link-auto-persist-static.spec.js`
- `IntRuoyiBackend/yudao-module-mes/src/test/js/mes-edhr-cell-link-task-id-context-static.spec.cjs`
- `IntRuoyiBackend/yudao-module-mes/src/test/js/mes-edhr-dynamic-form-cell-link-batch-code-static.spec.cjs`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordcelllink/MesProBatchRecordCellLinkServiceImplTest.java`

## RED

- RED: `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js` -> FAIL，执行页仍保留旧 `/prefill` 草稿注入路径。
- RED: `node IntRuoyiBackend/yudao-module-mes/src/test/js/mes-edhr-cell-link-task-id-context-static.spec.cjs` -> FAIL，后端打开请求仍写成 `.setTaskId(null)`。
- RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest#buildFormTemplateVersionPrefillData_resolvesProductionBatchCodeFromExecutionContext" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，动态表单预填接口缺少执行上下文批号参数。

## GREEN

- GREEN: `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-pre-release-editable-submit-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-fill-workspace-worktask-permission-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `node IntRuoyiBackend/yudao-module-mes/src/test/js/mes-edhr-cell-link-task-id-context-static.spec.cjs` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_withoutProductionTaskContext_stillOpensBatchRecordWithoutScheduleReference+openTask_ignoresSingleWorkOrderProductionTaskWhenOpeningBatchRecord" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_bindsExistingSingleExecutionContext" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-execution-real-flow.e2e.js` -> PASS，已在 `测试租户/codexedhrcell01` 通过真实前端批次详情“打开填写”路径断言执行 `1579` 的 `1:5` 单元格显示已落库批号 `EDHR-CELL-20260728-104808`。
- GREEN: slot 7 (`8088/48088`) 修复后运行态 `node tests/e2e/edhr-batch-execution-real-flow.e2e.js` -> PASS，证据 `real-e2e-slot7-evidence.md`。
- GREEN: `node IntRuoyiBackend/yudao-module-mes/src/test/js/mes-edhr-dynamic-form-cell-link-batch-code-static.spec.cjs` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-DskipTests" compile` -> PASS。
- BLOCKED: `node tests/e2e/mes/batch-record-cell-link-static.spec.js` -> FAIL，当前失败点为并行新增的 `templateId?: number` API 合同断言，不属于本次执行页草稿预填回归。
- BLOCKED: dynamic-form focused JUnit GREEN is blocked by unrelated product-name dropdown testCompile errors for missing `getProductNameOptions(String, boolean)`.

## Verification

- Static regression confirms the execution page no longer imports or calls `BatchRecordCellLinkApi.getPrefill`.
- Adjacent eDHR contracts confirm pre-release editable submit and work task permission behavior remain covered.
- Type verification confirms removing the draft prefill state leaves no Vue/TypeScript compile errors.
- The broader batch-record cell-link static contract is not used as this task's GREEN gate because it is currently blocked by an unrelated form-template API assertion.
- Real Playwright E2E passed on `int_main` main runtime after authorized test-tenant fixture repair; `task/open` returned `cellLinkAutoPersist.status=NO_CHANGE_ALREADY_APPLIED`, and both execution detail and original-form page input showed `EDHR-CELL-20260728-104808`.
- Isolated slot 7 Playwright E2E passed after loading the task-id backend fix; this verifies the screenshot-consistent root cause without relying on the already-running main backend jar.
- Dynamic-form static contract confirms both create-instance and open-instance paths pass `batch.getBatchCode()` into `buildFormTemplateVersionPrefillData(...)`, and the `batchCode` source branch reads `executionBatchCode` instead of `workOrder.batchCode`.

## Risk And Regression Scope

风险集中在 eDHR 执行页草稿 hydrate、传统批记录打开上下文和动态表单 FormCenter 草稿自动预填；修复不得改变已保存执行详情、字段审计、附件和只读追踪模式的既有读取链路。

## Blockers

- 本次回归和真实 E2E 无剩余 blocker；并行宽合同 `node tests/e2e/mes/batch-record-cell-link-static.spec.js` 仍阻塞在非本任务表单模板 API 断言。
- 动态表单真实 Playwright E2E 尚未完成；需要任务自有测试数据验证损耗单/过程检验记录打开后 FormCenter `form_data_json` 已落入链接批号。
