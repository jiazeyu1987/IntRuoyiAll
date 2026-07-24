# 执行日志：MES 手动重排预览后应用按钮仍不可点击回归修复

## 2026-06-26

- 初始化任务：根据用户截图复查手动重排抽屉状态，确认页面在“预览重排”成功后仍显示“预览参数已变化，请重新预览后再应用”。
- 初步排查：`src/views/mes/pro/scheduleorder/index.vue` 中 `replanPreviewStale` 通过 `JSON.stringify(lastReplanRequest.value) !== JSON.stringify(currentRequest)` 判定预览失效；`buildReplanRequest()` 会带 `reason`，但 `buildReplanRequestIfReady()` 不带 `reason`，存在同一预览被立即判成失效的风险。
- RED: `node tests/e2e/mes-pro-schedule-order-replan-apply-enabled-static.spec.js` -> FAIL，断言“预览请求不应包含业务原因”失败，确认当前源码会把 `reason` 混入预览请求，导致后续 stale 比对口径不一致。
- CHANGE: `src/views/mes/pro/scheduleorder/index.vue` 调整 `buildReplanRequest()`，预览请求只保留 `scheduleOrderIds/startTime/capacityMode/preserveManualLockedTasks`，业务原因继续仅在 `applyReplan()` 时附带提交。
- GREEN: `node tests/e2e/mes-pro-schedule-order-replan-apply-enabled-static.spec.js` -> PASS
- GREEN: `node tests/e2e/mes-pro-schedule-order-replan-scope-static.spec.js` -> PASS
- GREEN: `node tests/e2e/mes-pro-schedule-order-protected-task-readable-static.spec.js` -> PASS
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS
- GREEN: finalize-task-doc -> PASS，已将 `task.md` 当前状态更新为“已完成”，并补齐最终验证结果区块。
