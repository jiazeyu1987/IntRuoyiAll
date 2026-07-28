# Frontend Feature Evidence

## Feature

在 eDHR 批次执行详情右侧特殊节点操作区展示特殊工序填写人，覆盖 `来料检报告`、`灭菌报告`、`成品检报告`、`成品检记录`。

## Acceptance

选中任一特殊工序时，右侧操作区必须显示“填写人”，并使用后端返回的当前 `task.fillableUsers`；不得根据当前登录人、创建人、更新人或硬编码角色名推断填写人。

## BDD:

BDD: 特殊节点右侧展示配置填写人 -> Given 批次详情接口返回特殊节点 `fillableUsers` When 用户选中特殊节点 Then 右侧特殊节点操作区显示这些填写人，并与路线开始节点附件负责人配置一致。

## RED:

RED: `node tests/e2e/edhr-special-node-filler-display-static.spec.js` -> FAIL，特殊节点操作区未渲染填写人。

## GREEN:

GREEN: `node tests/e2e/edhr-special-node-filler-display-static.spec.js` -> PASS。

GREEN: `node doc\tasks\20260727-edhr-special-node-filler-from-route-start\e2e-special-node-filler-yudao-real.cjs` -> PASS。

## Verification

- 修改范围：`IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`，在特殊节点操作区复用 `edhr-batch-detail__rail-process-form-filler` 与 `resolveTaskCardFillersText(selectedTaskForEvidence)`。
- 真实 E2E：`芋道源码/admin`，`http://localhost:8081` + `http://127.0.0.1:48081`，结果写入 `doc/tasks/20260727-edhr-special-node-filler-from-route-start/e2e-artifacts/special-node-filler-yudao-real.json`。

## Blockers

- `node tests/e2e/edhr-special-node-attachment-actions-static.spec.js` 当前失败在既有“待提交附件删除接口”断言，不属于本次特殊节点填写人展示范围。
- 主工作区 dirty 且混有并行任务改动，本轮未提交/推送。
