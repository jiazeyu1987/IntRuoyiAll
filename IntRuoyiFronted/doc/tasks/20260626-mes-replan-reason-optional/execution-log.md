# 执行日志：MES 手动重排应用取消业务原因必填

## 2026-06-26

- 初始化任务：接收用户“点击应用重排的时候报错，不需要填理由”的变更请求，确认属于手动重排 apply 行为变更。
- BDD: 手动重排应用不再要求填写业务原因 -> Given 用户已生成有效重排预览且排产前检查无阻断 / When 用户不填写业务原因直接点击应用重排 / Then 前端不得再提示“请填写本次重排的业务原因”，而应继续发起 apply 请求。
- BDD: 业务原因仍可选填并随 apply 一起提交 -> Given 用户已生成有效重排预览 / When 用户填写业务原因后点击应用重排 / Then 前端仍应把最新 reason 一并提交给后端。
- BDD: 其他 apply 门禁保持不变 -> Given 用户未完成有效预览或排产前检查存在阻断 / When 用户点击应用重排 / Then 页面仍应继续拦截对应前置问题，不因本次变更放宽其他校验。
- RED: `node tests/e2e/mes-pro-schedule-order-replan-scope-static.spec.js` -> FAIL，断言“应用重排不应再因缺少业务原因被前端本地阻断”失败，确认 `index.vue` 仍存在 `throw new Error('请填写本次重排的业务原因')`。
- CHANGE: `src/views/mes/pro/scheduleorder/index.vue` -> 将“重排原因”输入框文案改为选填，删除 `applyReplan()` 的本地必填拦截，并把 apply 请求中的 `reason` 改为 `replanForm.reason?.trim() || undefined`。
- GREEN: `node tests/e2e/mes-pro-schedule-order-replan-scope-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-pro-schedule-order-replan-apply-enabled-static.spec.js` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
