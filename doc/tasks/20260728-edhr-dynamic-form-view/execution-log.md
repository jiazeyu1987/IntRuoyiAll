# Execution Log: eDHR 动态表单查看入口修复

## User Intent

用户指出批记录管理员右侧红框内主生产表可以查看，动态表单也应该可以查看，但当前动态表单无法查看。

## Initial State

- PRECHECK: `git -C E:\IntRuoyi status --short --branch` -> DIRTY，已有多个未提交改动；本任务只触碰动态表单查看问题相关文件。
- PRECHECK: `git -C E:\IntRuoyi\IntRuoyiFronted status --short --branch` -> DIRTY，同上。
- EXPERIENCE: `docs/experience-index.md` 存在，命中 eDHR 动态表单、表单槽位、查看表单和前端静态契约隔离门禁。

## BDD Scenarios

- BDD: 动态表单卡片可只读查看 -> Given eDHR 批次详情当前工序右侧存在动态表单卡片且包含动态表单实例上下文, When 用户点击该卡片“查看表单”, Then 中间预览区域应打开该动态表单的只读内容，而不是显示“当前节点没有可预览的批记录表单”。
- BDD: 主生产表查看链路不退化 -> Given 当前工序右侧存在主生产表卡片, When 用户点击主生产表“查看表单”, Then 仍按批记录表单执行记录上下文打开主生产表预览。
- BDD: 三类表单来源不混用 -> Given 同一工序同时有主生产表和动态表单, When 分别点击查看入口, Then 主生产表只读取逐工序批记录绑定，动态表单只读取 `formBindings` / 动态表单实例上下文。
- BDD: 已发布动态模板无 Jimu 布局仍可只读查看 -> Given eDHR 动态表单绑定的已发布 FormCenter 模板保留正式 `recognizedSchemaJson` 识别字段但未保存 `sheetLayoutJson` 包装, When 用户点击动态表单查看并调用任务预览, Then 后端应按 FormCenter 识别字段生成正式只读布局，而不是报“eDHR 审批快照无效”。

## Milestone Updates

- M0 in_progress: 读取 `bug-regression-fix-loop`、项目任务规则、前端开发规则、PowerShell 编码规则和经验索引，开始定位前端链路。
- M1 completed: 定位到 `BatchExecutionDetailPage.vue` 中心预览只渲染 `selectedExecution.formViewModel` / `selectedTaskPreview.formViewModel`，但 `shouldLoadTaskPreview` 明确排除 `formTemplateId/formCenterInstanceId` 动态表单；后端 `previewTask` 在 `batchRecordReportId` 为空时直接抛 `PRO_EDHR_BATCH_EXECUTION_SPECIAL_NODE_INVALID`。
- RED: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> FAIL，断言“右侧动态表单卡片选中态必须加载中心预览”失败。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormTemplatePreviewWithoutBatchReportSource" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> TIMEOUT at 120s before conclusive result; no pass result claimed. Frontend RED captured the visible regression.
- M2 completed: 新增 `edhr-dynamic-form-card-preview-static.spec.js` 和后端 `previewTask_returnsDynamicRouteFormTemplatePreviewWithoutBatchReportSource`，覆盖动态表单中心预览和不走批记录报表来源。
- M3 completed: 前端新增 `isDynamicRouteFormPreviewTask`，仅完整 `formBindingKey/formTemplateId/formTemplateVersionId/formCenterInstanceId` 且无 `batchRecordReportId` 的动态表单可进入中心预览；后端 `previewTask` 在传统报表缺失错误前分流动态表单，从 `FormTemplateVersionDO.jimuSchemaJson` 合并 `cellRules/signatureCellMarkers` 生成只读布局。
- GREEN: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-loss-form-open-action-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-admin-preview-runtime-fix-static.spec.js` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormTemplatePreviewWithoutBatchReportSource" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS, Tests run: 1, Failures: 0, Errors: 0.
- CHECK: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue IntRuoyiFronted/tests/e2e/edhr-dynamic-form-card-preview-static.spec.js IntRuoyiFronted/tests/e2e/edhr-loss-form-open-action-static.spec.js IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceTest.java` -> no whitespace errors; Git reported CRLF normalization warnings only.
- EXPERIENCE: 读取 `project-experience-consolidation`；将动态表单中心预览可以走统一 `/task/preview` 但后端必须 FormCenter 分流、不走批记录来源的经验合并到 `docs/e2e-rules.md#eDHR 路线表单跳过口径门禁`。
- BUG-EVIDENCE-CHECK: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence E:\IntRuoyi\doc\tasks\20260728-edhr-dynamic-form-view\bug-regression-evidence.md` -> PASS。
- CLEANUP-PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-edhr-dynamic-form-view --mode preview` -> keep task/exec/verification/bug evidence; delete none; blocked none; warnings none。
- CLEANUP-APPLY: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-edhr-dynamic-form-view --mode apply` -> applied; deleted none。
- M4 completed: 更新 bug regression evidence、verification report、经验门禁和 task 状态；因工作区存在大量非本任务既有改动，未执行提交/推送，避免混入无关变更。
- FOLLOW-UP in_progress: 用户反馈点击后提示“eDHR 审批快照无效”；定位到当前动态表单预览只接受 `jimuSchemaJson.sheetLayoutJson` 字符串，未覆盖 FormCenter 页面正式支持的“已发布模板仅有识别字段时生成视觉预览布局”路径。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormRecognizedFieldsPreviewWithoutJimuSchema" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> TIMEOUT at 184s while Maven was resolving/reading dependencies; no test result claimed. Captured PID 34960 stack in Maven dependency resolution and stopped only this task-owned Maven process.
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormRecognizedFieldsPreviewWithoutJimuSchema" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> TIMEOUT at 244s with no new surefire report; stopped only this task-owned Maven process.
- RED: `mvn -o -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormRecognizedFieldsPreviewWithoutJimuSchema" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `previewTask` threw `ServiceException: eDHR 审批快照无效` from `buildDynamicRouteFormPreviewSheetLayout`.
- M5 completed: 新增后端回归 `previewTask_returnsDynamicRouteFormRecognizedFieldsPreviewWithoutJimuSchema`，覆盖已发布动态模板无 `jimuSchemaJson.sheetLayoutJson` 但有正式 `recognizedSchemaJson` 时仍应生成只读预览。
- FIX: `buildDynamicRouteFormPreviewSheetLayout` 先解析已保存 Jimu 布局；无布局时按 FormCenter `recognizedSchemaJson` 生成与前端 `buildTemplateVisualPreviewModel` 对齐的两列只读布局、字段规则和签名 marker；坏 JSON、空识别字段、只有规则无布局仍 fail-fast。
- GREEN: `mvn -o -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormRecognizedFieldsPreviewWithoutJimuSchema" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS, Tests run: 1, Failures: 0, Errors: 0.
- GREEN: `mvn -o -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsDynamicRouteFormTemplatePreviewWithoutBatchReportSource" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS, Tests run: 1, Failures: 0, Errors: 0.
- GREEN: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-loss-form-open-action-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-admin-preview-runtime-fix-static.spec.js` -> PASS。
- CHECK: `git diff --check -- <task files>` -> no whitespace errors; Git reported CRLF normalization warnings only.
- EXPERIENCE: 将“动态表单 `/task/preview` 未保存布局时可按 FormCenter `recognizedSchemaJson` 生成只读布局，仍禁止误走批记录报表来源”的经验合并到 `docs/e2e-rules.md#eDHR 路线表单跳过口径门禁`。
- BUG-EVIDENCE-CHECK: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence E:\IntRuoyi\doc\tasks\20260728-edhr-dynamic-form-view\bug-regression-evidence.md` -> PASS。
- CLEANUP-PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-edhr-dynamic-form-view --mode preview` -> keep task/exec/verification/bug evidence; delete none; blocked none; warnings none。
- CLEANUP-APPLY: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-edhr-dynamic-form-view --mode apply` -> applied; deleted none; blocked none; warnings none。
- M6 completed: 跟进修复“eDHR 审批快照无效”动态表单模板解析回归；因工作区仍存在大量非本任务改动，任务保持 `ready_for_closeout`，未执行提交/推送。
- FOLLOW-UP in_progress: 用户反馈右侧红框表单卡片不容易看出哪个被选中；预期与左侧工序面板一致，当前选中的表单卡片背景变黄色。
- BDD: 右侧当前表单卡片黄底选中态 -> Given eDHR 批次详情右侧红框内存在多个表单卡片, When 用户点击其中一个表单卡片并切换中间预览, Then 当前选中卡片应显示与左侧工序面板一致的浅黄色背景，其他卡片保持普通背景。
- RED: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> FAIL，右侧卡片 `.edhr-batch-detail__rail-process-form-item.is-active` 仍为蓝色背景 `#eef5ff`，未使用左侧同款浅黄色选中反馈。
- FIX: 将 `.edhr-batch-detail__rail-process-form-item.is-active` 背景色改为 `#fff8e6`，保留蓝色边框和左侧强调条，点击、预览和后端数据来源逻辑不变。
- GREEN: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-admin-filler-visibility-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-loss-form-open-action-static.spec.js` -> PASS。
- CHECK: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence E:\IntRuoyi\doc\tasks\20260728-edhr-dynamic-form-view\frontend-feature-evidence.md` -> PASS。
- CHECK: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence E:\IntRuoyi\doc\tasks\20260728-edhr-dynamic-form-view\bug-regression-evidence.md` -> PASS。
- CHECK: `git diff --check -- <task frontend/doc files>` -> no whitespace errors; Git reported CRLF normalization warnings only。
- EXPERIENCE-CHECK: 已读取 `project-experience-consolidation`；本次为局部视觉选中态修复，未新增可复用长期经验规则，经验保留在任务证据中。
- CLEANUP-PREVIEW: `task_closeout.py --task-id 20260728-edhr-dynamic-form-view --mode preview` -> keep task/exec/verification/bug/frontend evidence; delete none; blocked none; warnings none。
- CLEANUP-APPLY: `task_closeout.py --task-id 20260728-edhr-dynamic-form-view --mode apply` -> applied; deleted none; blocked none; warnings none。
- M7 completed: 右侧红框当前选中表单卡片已改为浅黄色背景；工作区仍有大量非本任务改动，任务保持 `ready_for_closeout`，未执行提交/推送。
