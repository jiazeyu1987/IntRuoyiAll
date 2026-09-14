# Frontend Feature Evidence

## Feature Goal

批次详情主区域按用户截图红框要求展示：无已提交内容时为空表单，有已提交内容时为已提交表单单元内容。

## Acceptance

- 无 submitted execution 内容时，主区域加载正式预览模板并显示空表单。
- 有 submitted `formViewModel` 时，主区域优先显示 submitted 单元格内容。
- task preview 只能提供空表单模板壳，不能把草稿 `cellValuesJson` 展示为已提交内容。

## Non-Goals

- 不改变批记录表单、表单槽位、工序开始三类配置的数据来源。
- 不改变保存、提交、签名、审核接口。
- 不用草稿单元值或历史 execution 直连冒充已提交内容。

## UI Entry Points

- `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`
- `IntRuoyiFronted/src/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue`

## API Contracts And Data States

- 已提交内容：来自 `review-timeline.executionReviews[].formViewModel`。
- 空表单：可复用当前选中表单正式预览模板，但必须清空 `cellValuesJson`，不得展示草稿内容。

## BDD Scenarios

- BDD: 空表单壳显示 -> Given 无 submitted execution 内容 When 主区域渲染 Then 显示空白只读表单。
- BDD: submitted 内容优先 -> Given 存在 submitted formViewModel When 主区域渲染 Then 显示 submitted 单元内容。

## Verification

- RED: `node tests/e2e/edhr-batch-admin-preview-runtime-fix-static.spec.js` -> FAIL，缺少空表单壳数据源。
- GREEN: `node tests/e2e/edhr-batch-admin-preview-runtime-fix-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/edhr-batch-first-screen-detail-defer-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/edhr-loss-form-open-action-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/edhr-batch-detail-preview-scroll-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `node tests/e2e/edhr-batch-admin-preview-runtime-fix.e2e.js` -> PASS，表单和模板 sheet 可见，MES 写请求为空，console/page errors 为空。
- Responsive/accessibility/loading/empty/error/permission checks: skeleton/loading、error alert、无可预览模板空态、只读页面无写请求已覆盖。

## Blockers

- 暂无。
