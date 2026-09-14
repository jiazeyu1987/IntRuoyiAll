# Verification Report: eDHR 动态表单查看入口修复

## Result

- Status: ready_for_closeout。
- Outcome: 右侧动态表单卡片选中后可进入中心只读预览；“查看表单”抽屉动作保留；主生产表仍按正式批记录表单来源预览。

## Implementation Summary

- Frontend: `BatchExecutionDetailPage.vue` 增加 `isDynamicRouteFormPreviewTask`，只允许完整 FormCenter 上下文的动态表单进入中心预览，避免请求未配置任务。
- Backend: `previewTask` 在传统 `batchRecordReportId` 缺失报错前分流动态表单，从 `FormTemplateVersionDO.jimuSchemaJson` 读取 `sheetLayoutJson/cellRules/signatureCellMarkers` 并合并为 `FormViewModel`。
- Follow-up backend: 动态表单预览新增 FormCenter `recognizedSchemaJson` 识别字段布局生成路径，已发布模板未保存 `sheetLayoutJson` 包装时也能生成只读布局；`layout/rows` 等已有正式布局结构继续按布局解析，空或坏结构仍报错。
- Follow-up UI: 右侧红框内当前选中表单卡片背景改为 `#fff8e6`，与左侧工序面板浅黄色选中反馈一致。
- Source isolation: 动态表单不调用 `jimuReportGateway.getReportJson`；主生产表仍调用批记录报表来源。

## Verification

- RED: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> FAIL，动态表单仍被预览门禁排除。
- GREEN: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-loss-form-open-action-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-admin-preview-runtime-fix-static.spec.js` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormTemplatePreviewWithoutBatchReportSource" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS, Tests run: 1, Failures: 0, Errors: 0.
- RED: `mvn -o -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormRecognizedFieldsPreviewWithoutJimuSchema" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，抛 `eDHR 审批快照无效`。
- GREEN: `mvn -o -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormRecognizedFieldsPreviewWithoutJimuSchema" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS, Tests run: 1, Failures: 0, Errors: 0。
- GREEN: `mvn -o -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormTemplatePreviewWithoutBatchReportSource" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS, Tests run: 1, Failures: 0, Errors: 0。
- RED: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> FAIL，右侧当前选中表单卡片仍为蓝色背景。
- GREEN: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-admin-filler-visibility-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-loss-form-open-action-static.spec.js` -> PASS。
- CHECK: frontend feature evidence validator -> PASS。
- CHECK: bug regression evidence validator -> PASS。
- CLEANUP: `task_closeout.py --task-id 20260728-edhr-dynamic-form-view --mode preview/apply` -> keep task evidence including bug/frontend evidence, delete none, blocked none, warnings none.
- CHECK: `git diff --check -- <task files>` -> no whitespace errors; CRLF normalization warnings only.
- CLEANUP: `task_closeout.py --task-id 20260728-edhr-dynamic-form-view --mode preview/apply` -> keep task evidence, delete none, blocked none, warnings none.
- EXPERIENCE: Updated `docs/e2e-rules.md#eDHR 路线表单跳过口径门禁` with FormCenter `/task/preview` source isolation and `recognizedSchemaJson` preview generation rules.

## Remaining Notes

- Real browser E2E was not run because this fix is covered by static and service contracts in the current turn; local runtime availability was not required.
- Commit/push not performed because the worktree already contains many unrelated modified/untracked files; mixing them into a baseline/task commit would exceed this fix scope.
