# Bug Regression Evidence

## Bug

真实 E2E 已确认后端详情接口返回 4 个特殊工序的 `fillableUsers`，但批次执行详情右侧特殊节点操作区没有展示填写人，导致用户无法在页面上确认 `来料检报告`、`灭菌报告`、`成品检报告`、`成品检记录` 的填写人来源。

## Expected

批次执行详情选中任一特殊工序时，右侧特殊节点操作区应展示“填写人”，文本必须来自该特殊 `task.fillableUsers`，并与工艺路线 `工序开始 > 批记录附件` 负责人解析结果一致。

## Reproduction

RED: `node tests/e2e/edhr-special-node-filler-display-static.spec.js` -> FAIL，右侧特殊节点操作区缺少 `edhr-batch-detail__special-node-filler` 和 `resolveTaskCardFillersText(selectedTaskForEvidence)`。

真实复现：`node doc\tasks\20260727-edhr-special-node-filler-from-route-start\e2e-special-node-filler-yudao-real.cjs` -> FAIL，等待 `.edhr-batch-detail__rail-process-form-filler` 显示 `瑛泰管理员、黎敏` 超时。

## Root Cause

`BatchExecutionDetailPage.vue` 只在普通工序的右侧单据卡片中渲染 `edhr-batch-detail__rail-process-form-filler`，特殊节点分支只渲染上传、跳过、完成按钮，没有复用 `resolveTaskCardFillersText` 展示当前特殊 task 的填写人。

## GREEN:

GREEN: `node tests/e2e/edhr-special-node-filler-display-static.spec.js` -> PASS。

GREEN: `node doc\tasks\20260727-edhr-special-node-filler-from-route-start\e2e-special-node-filler-yudao-real.cjs` -> PASS，批次 `900000000878`、路线 `922119/V15` 的 4 个特殊工序页面填写人展示与负责人配置一致。

## Verification

- `node tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js` -> PASS，普通单据填写人展示未回归。
- `node tests/e2e/edhr-special-node-display-name-static.spec.js` -> PASS，特殊节点业务名称展示未回归。
- `node tests/e2e/edhr-special-node-attachment-actions-static.spec.js` -> FAIL，既有待提交附件删除接口静态断言失败；失败点不属于本次填写人展示修复。

## Blockers

- 主工作区仍包含大量并行 dirty 改动，当前任务未执行提交/推送，不能标记 `completed`。
- 历史隔离 worktree `D:\IntRuoyiWorktree\edhr-special-node-filler-e2e-20260727` 的删除仍需用户明确授权丢弃该 worktree 临时改动。
