# 执行日志：MES 报工成功后自动刷新排产工单

## 2026-06-27

- 初始化任务：根据用户需求“报工完之后，自动刷新一次对应的排产工单”，创建前端任务并限定在报工成功后的排产工单列表自动刷新。
- BDD: 整批确认报工成功后自动刷新排产工单 -> Given 用户在报工页成功确认当前导入批次 / When 页面切回正式报工 tab 并发出成功提示 / Then 排产工单页若已打开，应自动重新拉取列表以显示最新进度。
- BDD: 报工失败或取消时不刷新排产工单 -> Given 用户取消确认或后端返回失败 / When 报工流程结束 / Then 不发送排产工单刷新事件，也不触发多余列表刷新。
- INVESTIGATION: `feedback/index.vue` -> PASS，确认 `handleConfirmBatch()` 成功后当前只执行 `getImportRecordList()` 与 `getList()`，没有触发排产工单页刷新。
- INVESTIGATION: `scheduleorder/index.vue` -> PASS，确认页面已有 `getScheduleOrderList()` 刷新入口，但当前未订阅任何跨页面报工成功事件。
- INVESTIGATION: `src/hooks/web/useEmitt.ts` -> PASS，确认项目已有全局 `mitt` 事件总线，可用于最小代价的跨页面刷新同步。
- RED: `node -e "<assert HEAD 缺少 mes-schedule-order-refresh 事件常量与监听>"` -> FAIL，`AssertionError: HEAD 报工页还没有排产工单刷新事件常量`，证明基线版本尚不满足自动刷新需求。
- CHANGE: `feedback/index.vue` -> 在确认报工成功链路中引入统一事件常量 `mes-schedule-order-refresh`，并在 `confirmImportRecordBatch` 成功、列表刷新完成后派发刷新事件。
- CHANGE: `scheduleorder/index.vue` -> 订阅 `mes-schedule-order-refresh` 事件，收到后调用既有 `getScheduleOrderList()`，自动拉取最新排产工单进度。
- GREEN: `node tests/e2e/mes-feedback-schedule-order-refresh-static.spec.js` -> PASS，确认报工成功事件派发与排产工单页监听刷新合同同时存在。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260627-mes-feedback-refresh-scheduleorder-after-submit/frontend-feature-evidence.md` -> PASS，确认前端交付证据结构完整。
