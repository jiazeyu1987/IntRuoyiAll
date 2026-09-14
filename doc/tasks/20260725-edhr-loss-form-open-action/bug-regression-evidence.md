# Bug Regression Evidence

## Bug Summary

用户在 eDHR 批次详情右侧选择“损耗单”动态表单时，页面仍显示红色错误“必填路线表单不允许跳过”。当前账号有查看权限但没有填写权限时，预期主动作显示“查看表单”，打开只读表单抽屉，且选择卡片不触发跳过、打开填写或 legacy 批记录预览接口。

## Expected

- 选择动态损耗单卡片不请求 `/task/preview`。
- 点击 `查看表单` 打开只读抽屉，动作按钮保持禁用。
- 页面不显示“必填路线表单不允许跳过”。
- 不调用打开填写、跳过表单或 MES/FormCenter 写请求。

## Reproduction

- Path: 本机 `http://localhost:8081` 登录 `芋道源码/admin`，进入 eDHR 批次详情，选择 REQUIRED 损耗单动态表单卡片。
- Focused RED: `node tests\e2e\edhr-loss-form-open-action-static.spec.js` -> FAIL，缺少 `shouldLoadTaskPreview` 门禁，动态表单选择态仍可能请求批记录预览。

## Root Cause

`BatchExecutionDetailPage.vue` 的 `selectProcessTask` 在没有匹配执行记录时直接调用 `loadTaskPreview`。`loadTaskPreview` 只排除了特殊节点，没有排除 `formTemplateId/formCenterInstanceId` 动态表单，导致表单中心路线表单误调用只适用于 legacy 批记录表单的 `getEdhrBatchTaskPreview`，最终把只读查看路径误报为“必填路线表单不允许跳过”。

## Regression Test

- Static contract updated: `IntRuoyiFronted/tests/e2e/edhr-loss-form-open-action-static.spec.js`
- Real E2E strengthened: `IntRuoyiFronted/tests/e2e/edhr-loss-form-open-action-real.e2e.js`

## RED:

- `node tests\e2e\edhr-loss-form-open-action-static.spec.js` -> FAIL, expected reason: `missing block: const shouldLoadTaskPreview = (task: EdhrBatchExecutionTaskRespVO) =>`

## GREEN:

- `node tests\e2e\edhr-loss-form-open-action-static.spec.js` -> PASS
- `node --check tests\e2e\edhr-loss-form-open-action-real.e2e.js` -> PASS
- `node tests\e2e\edhr-loss-form-open-action-real.e2e.js` -> PASS

## Verification

- Target: `batchExecutionId=900000000846`, `taskId=6557`, `requiredPolicy=REQUIRED`, `allowedActions=[]`, `formCenterInstanceId=312`, `formTemplateId=25`
- Assertions: `actionLabel=查看表单`, readonly action buttons disabled, `previewRequests=[]`, `skipErrorCount=0`, `blockedWrites=[]`
- Evidence: `doc/tasks/20260725-edhr-loss-form-open-action/real-e2e-output/readonly-loss-form-card-result.json`

## Risk And Scope

- Scope is limited to batch detail task preview loading. Legacy batch-record tasks with `batchRecordReportId` still use `getEdhrBatchTaskPreview`.
- Dynamic Form Center route forms no longer call legacy preview when selected; viewing still uses the existing readonly drawer path.
- No fallback, silent downgrade, or swallowed backend error was introduced.

## Blockers

- Final commit/push remains blocked by unrelated dirty files and existing branch state outside this task. Implementation and required verification are complete.
