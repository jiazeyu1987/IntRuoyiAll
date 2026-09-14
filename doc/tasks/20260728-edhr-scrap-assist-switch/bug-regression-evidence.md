# Bug Regression Evidence: eDHR Scrap Assist Switch

## Bug Summary
- 用户在 eDHR 执行页“切换填写人”弹窗选择“张可莹 / 工艺路线表单槽位 / 损耗单”时，期望切换到损耗单填写链路。
- 旧行为把 FormCenter 表单槽位候选继续导向传统 eDHR 批记录填写页，触发“eDHR 批次缺少唯一批记录路线”或先要求 `executionId`。

## Expected Behavior
- FormCenter 表单槽位候选必须先通过 `openTask` 校验所选 `assistUserId`。
- 若返回 `formCenterInstanceId + formTemplateId`，前端应跳转批次详情并携带 `openRouteForm=1 + batchTaskId + workTaskId + assistUserId`，由批次详情路线表单抽屉打开损耗单。
- 传统批记录任务仍按 `executionId` 进入 `/mes/pro/feedback/edhr-execution/form`。

## Reproduction Path
- 在 eDHR 执行页打开“切换填写人”。
- 选择同一工序下“工艺路线表单槽位 / 损耗单 / 张可莹”候选。
- 旧实现中 `navigateToAssistBatchTask` 在 FormCenter 分支前先执行 `if (!opened.executionId)`，并且批次详情二次打开不透传 `assistUserId`。

## Root Cause
- 辅助切换的批次任务导航没有区分传统批记录任务和 FormCenter 表单槽位任务。
- FormCenter 槽位任务没有传统 `executionId`，但旧代码先按传统 execution 路径校验，导致损耗单链路被错误导向批记录执行上下文。

## Regression Test
- 新增 `IntRuoyiFronted/tests/e2e/edhr-switch-filler-formcenter-slot-static.spec.js`。
- 覆盖 FormCenter 分支必须先于 `executionId` guard、必须跳转批次详情 `openRouteForm=1`、必须写入 `batchTaskId/workTaskId/assistUserId`、详情页二次 `openTask` 必须透传 `assistUserId`。

## Verification

RED: `node tests\e2e\edhr-switch-filler-formcenter-slot-static.spec.js` -> FAIL，原因是旧实现没有 FormCenter 槽位详情页分支，先要求 `executionId`。

GREEN: `node tests\e2e\edhr-switch-filler-formcenter-slot-static.spec.js` -> PASS。
- `node tests\e2e\edhr-switch-filler-selectability-static.spec.js` -> PASS。
- `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> PASS。
- `node tests\e2e\edhr-work-task-formcenter-navigation-static.spec.js` -> PASS。
- `node tests\e2e\edhr-loss-form-open-action-static.spec.js` -> PASS。
- `node tests\e2e\edhr-dynamic-form-card-preview-static.spec.js` -> PASS。
- `node tests\e2e\edhr-batch-detail-assist-preview-switch-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS。

## Risk And Regression Scope
- 风险集中在前端导航分流；传统批记录任务仍保留原 `executionId` guard 和填写页路径。
- 批次详情自动打开路径新增 `assistUserId` 透传，只在 `openRouteForm=1` 且 route query 指向同一任务时生效。

## Blockers And Follow-up Actions
- 无产品阻塞。
- 本地 `int_main` 有并行脏改动且当前任务分支包含无关基线历史，融合时必须把本任务提交 cherry-pick 到干净 `origin/int_main` 分支后推送。
