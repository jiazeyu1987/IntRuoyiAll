# Execution Log

## User Intent

批记录管理员主区域不读取草稿；其他账户提交该批次执行内容后，管理员应能在主区域看到当前已提交内容。

## BDD

BDD: 管理员查看他人已提交批记录内容 -> Given 同一批次的填写人已提交某工序 execution；When 批记录管理员打开批次详情并选择该工序；Then 主区域渲染该已提交 execution 的 cellValuesJson，而不是管理员草稿或空模板预览。

BDD: 草稿和待打开不顶替主区域 -> Given 某工序只有草稿 execution 或待打开任务；When 批记录管理员打开批次详情；Then 主区域不展示草稿内容，不调用空 preview 冒充已提交内容，而显示暂无已提交内容。

## Current Evidence

- 前端主区域当前按 selectedExecution.formViewModel || selectedTaskPreview.formViewModel 取值。
- 后端 task preview 对未创建 execution 的任务返回 cellValuesJson = []。

## Implementation

- 主区域移除 `selectedTaskPreview` / `taskPreviewLoading` / `taskPreviewError` 渲染路径，不再调用 `/task/preview` 顶替已提交内容。
- 新增 `SUBMITTED_EXECUTION_REVIEW_STATUSES = [2, 3, 4]`，`selectedExecution` 只从已提交、已批准、填写完成的 execution review 中选择。
- 辅助模式只读预览的 `selectedPreviewFormViewModel` 只读取已提交 execution 的 `formViewModel`。
- 未匹配已提交 execution 时，主区域显示“暂无已提交批记录内容”。

## Verification Evidence

RED: baseline static assertion against pre-fix `BatchExecutionDetailPage.vue` -> FAIL, expected reason: baseline still rendered `selectedTaskPreview` in main area.

GREEN: `node tests/e2e/edhr-batch-admin-preview-runtime-fix-static.spec.js` -> PASS.

GREEN: `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js` -> PASS.

GREEN: `node tests/e2e/edhr-batch-admin-current-process-highlight-static.spec.js` -> PASS.

GREEN: `node tests/e2e/edhr-batch-main-area-fill-static.spec.js` -> PASS.

GREEN: `node tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js` -> PASS.

GREEN: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` -> PASS.

GREEN: `pnpm ts:check` -> PASS.

REGRESSION NOTE: `node tests/e2e/edhr-batch-detail-review-fusion-static.spec.js` -> FAIL at legacy assertion “融合详情页必须同时展示工序复盘主线和批次级无工序信息”; direct HEAD check shows the same label assertion fails outside this change, so it is recorded as an unrelated wide-contract blocker.
