# Bug Regression Evidence

## Bug Summary

批记录/工艺路线配置右侧动态表单列表仍显示“记录本”开关，且前端会保留历史关闭状态，不符合“记录本选项不显示，默认全部打开”的要求。

## Expected Behavior

用户查看动态表单配置卡片时不应看到“记录本”开关；新增、读取、草稿快照和保存的表单绑定均按 `recordbookEnabled: true` 处理。

## Reproduction

- RED: `node tests\e2e\edhr-recordbook-config-default-open-static.spec.js` -> FAIL。
- Expected reason: 旧源码仍包含 `data-route-process-setting-field="recordbook-enabled"`。

## Root Cause

`RouteFlowGraphDesigner.vue` 同时在模板中渲染记录本开关，并在读取/保存路径中使用 `recordbookEnabled !== false` 保留历史关闭值，导致用户仍能看到并保存关闭状态。

## Fix

删除记录本开关及其处理器，统一将动态表单绑定的 `recordbookEnabled` 写为 `true`，并同步静态/真实 E2E 合同移除旧禁用样本分支。

## Verification

- GREEN: `node tests\e2e\edhr-recordbook-config-default-open-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-recordbook-batch-sync-static.spec.js` -> PASS。
- GREEN: `node --check tests\e2e\edhr-recordbook-batch-sync-real.e2e.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Risk And Regression Scope

风险集中在原先依赖 UI 关闭记录本的路线配置流程；按本次需求该关闭入口已正式废弃。保留全局记录本开关合同，不改变全局开关 API。

## Blockers And Follow-up

- 无当前功能阻塞。
- 并发非本任务改动仍存在，提交阶段需按补丁范围隔离。
