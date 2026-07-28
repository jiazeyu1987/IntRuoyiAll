# Bug Regression Evidence: eDHR 动态表单查看入口

## Summary

- Bug: eDHR 批次详情右侧动态表单卡片点击“查看表单”后无法像主生产表一样打开查看。
- Expected: 动态表单“查看表单”应按动态表单上下文打开只读预览；主生产表查看链路保持按批记录表单上下文打开。

## Reproduction

- Path: eDHR 批次详情页，右侧单据卡片区域，点击动态表单卡片的“查看表单”。
- Observed from user screenshots: 主生产表卡片可查看；动态表单卡片点击后中间区域显示“当前节点没有可预览的批记录表单”。

## Root Cause

- Frontend: `BatchExecutionDetailPage.vue` 的 `shouldLoadTaskPreview` 只允许传统批记录任务进入中心只读预览，且用 `!task.formTemplateId && !task.formCenterInstanceId` 明确排除动态表单任务，导致选中动态表单卡片后只能落到空态。
- Backend: `previewTask` 在任务没有 `batchRecordReportId` 时直接抛 `PRO_EDHR_BATCH_EXECUTION_SPECIAL_NODE_INVALID`，没有为动态表单任务按 `formBindings/FormCenter` 上下文生成可视只读布局。
- Follow-up backend: 动态表单分流后只接受 `jimuSchemaJson.sheetLayoutJson`，未覆盖 FormCenter 页面正式支持的“已发布模板只有 `recognizedSchemaJson` 识别字段时生成视觉预览布局”路径，导致点击后报 `eDHR 审批快照无效`。
- Follow-up UI: 右侧红框内表单卡片虽然绑定了 `is-active` 选中类，但选中态仍使用蓝色背景，和左侧工序面板浅黄色选中反馈不一致，用户难以判断当前中间预览对应哪张表单。

## Regression Test

- Added: `IntRuoyiFronted/tests/e2e/edhr-dynamic-form-card-preview-static.spec.js`，锁定动态表单卡片选中态必须加载中心预览，且主生产表仍保留批记录报表来源。
- Updated: `IntRuoyiFronted/tests/e2e/edhr-loss-form-open-action-static.spec.js`，把旧的“动态表单不得进入预览接口”口径更新为“预览接口必须按后端 FormCenter 分流，不得误走批记录来源或跳过错误”。
- Added: `MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormTemplatePreviewWithoutBatchReportSource`，验证动态表单预览从 `FormTemplateVersionDO.jimuSchemaJson` 合并布局规则，且 `jimuReportGateway.getReportJson` 未被调用。
- Added: `MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormRecognizedFieldsPreviewWithoutJimuSchema`，验证仅有正式 `recognizedSchemaJson` 的已发布动态模板也能生成 FormCenter 只读布局，且不调用批记录报表来源。
- Updated: `IntRuoyiFronted/tests/e2e/edhr-dynamic-form-card-preview-static.spec.js`，锁定右侧当前选中表单卡片必须使用与左侧工序面板一致的浅黄色背景。

## RED

- RED: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> FAIL，失败原因：当前 `shouldLoadTaskPreview` 未包含 `isDynamicRouteFormPreviewTask(task)`，动态表单被 `formTemplateId/formCenterInstanceId` 排除。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormTemplatePreviewWithoutBatchReportSource" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> TIMEOUT at 120s before conclusive result; no pass result claimed.
- RED: `mvn -o -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormRecognizedFieldsPreviewWithoutJimuSchema" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，后端抛 `ServiceException: eDHR 审批快照无效`。
- RED: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> FAIL，右侧当前选中表单卡片仍为蓝色背景，未与左侧面板一致显示浅黄色。

## GREEN

- GREEN: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-loss-form-open-action-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-admin-preview-runtime-fix-static.spec.js` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormTemplatePreviewWithoutBatchReportSource" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS, Tests run: 1, Failures: 0, Errors: 0.
- GREEN: `mvn -o -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormRecognizedFieldsPreviewWithoutJimuSchema" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS, Tests run: 1, Failures: 0, Errors: 0。
- GREEN: `mvn -o -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormTemplatePreviewWithoutBatchReportSource" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS, Tests run: 1, Failures: 0, Errors: 0。
- GREEN: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> PASS，右侧当前选中表单卡片已使用浅黄色背景。
- GREEN: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-admin-filler-visibility-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-loss-form-open-action-static.spec.js` -> PASS。

## Verification

- `git diff --check -- <task files>` -> no whitespace errors; Git reported CRLF normalization warnings only.
- Static contracts and service regression together verify main production form and dynamic form source isolation.

## Risk And Scope

- Scope: eDHR 批次详情右侧卡片查看入口、中间预览选择逻辑、动态表单后端预览布局解析。
- Risk controlled: 动态表单预览只在具备 `formBindingKey/formTemplateId/formTemplateVersionId/formCenterInstanceId` 且无 `batchRecordReportId` 时启用；后端使用表单中心模板版本 JSON 或正式识别字段生成布局，不查询批记录报表 JSON；坏 JSON、空字段、规则无布局继续 fail-fast；主生产表继续使用 `batchRecordReportId` 来源。
- UI scope: 只调整右侧当前选中表单卡片的背景色，不改变按钮可见性、可点击状态、预览接口或数据来源。

## Blockers

- 当前仓库已有多项非本任务未提交改动且分支已 ahead 1；本任务未执行提交/推送，避免混入无关变更。
