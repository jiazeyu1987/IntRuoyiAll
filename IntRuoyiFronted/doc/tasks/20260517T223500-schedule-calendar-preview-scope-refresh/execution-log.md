# Execution Log

BDD: schedule calendar preview refreshes latest scope before empty warning -> Given 排程日历页面已打开且前端缓存的 scope 工单范围可能已过期 When 用户点击生成预览 Then 页面先向后端刷新最新可排产工单范围，再决定是否提示“当前没有可参与自动排产的已确认自制工单”

BDD: task page preview refreshes latest filtered scope before empty warning -> Given 生产排产列表页面的筛选条件保持不变但后端工单范围已变化 When 用户点击自动排产生成预览 Then 页面先按当前筛选条件重新拉取 scope，再决定是否提示“当前筛选范围没有可排产工单”

- 2026-05-17 22:35 Asia/Shanghai: 当前真实运行态 `http://127.0.0.1:8081/mes/pro/schedule-calendar` 已复现到 scope 计数与预览请求链路，可继续补 stale-scope 防护。
- RED: `node doc/tasks/20260517T223500-schedule-calendar-preview-scope-refresh/scripts/verify-preview-scope-refresh.cjs` -> FAIL, `previewAutoSchedule` 与 `handleAutoSchedulePreview` 在空 scope 提示前未主动刷新最新工单范围。
- GREEN: `node doc/tasks/20260517T223500-schedule-calendar-preview-scope-refresh/scripts/verify-preview-scope-refresh.cjs` -> PASS, 两个预览入口都在空 scope 判断前先刷新最新范围。
- GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session schedule-calendar-preview-scope-refresh-fresh run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517T223500-schedule-calendar-preview-scope-refresh\scripts\probe-schedule-calendar-preview-scope.mjs` -> PASS, 真实 `8081 -> 48081` 路径返回 `scopeWorkOrderTotal=2`、`scopeWorkOrderCodes=["881MO090789","881MO090796"]`，点击 `生成预览` 后成功发出 `/mes/pro/auto-schedule/preview` 请求。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; npm run ts:check` -> PASS.
