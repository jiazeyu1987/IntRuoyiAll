# 执行日志：MES 应用重排确认链路超时修复

## BDD
- BDD: 应用重排确认链路长耗时不被 30 秒前端超时截断 -> Given 用户在手动重排弹框选择今天或明天 / When 点击确认应用重排 / Then 前端对 preflight、replan preview、replan apply 均使用手动重排专用长超时。
- BDD: 全局接口超时不被放大 -> Given 其他普通接口请求 / When 发起请求 / Then 仍使用全局 30000ms 默认超时。

## Evidence
- GREEN: screenshot-ocr-reproduction -> PASS, 截图文字包含 `接口请求超时，请刷新页面重试！ timeout of 30000ms exceeded`，位置在“应用重排开始日期”确认弹框后。
- ROOT_CAUSE: confirm-apply-chain-timeout -> `replanApply` 已配置 180000ms，但确认应用前重新执行的 `preflightScheduleOrders` 与 `replanPreview` 仍使用全局 30000ms 默认超时。
- GREEN: experience-preflight -> PASS, 已读取 PowerShell、经验索引、登录、bug 回归与前端交付规则。
- RED: frontend static timeout contract -> FAIL, `node tests\e2e\mes-replan-whole-day-apply-static.spec.js` 返回 `手动重排必须定义统一长耗时请求超时`，证明当前只覆盖 apply，未覆盖确认链路中的 preflight / preview。
- GREEN: frontend static timeout contract -> PASS, `node tests\e2e\mes-replan-whole-day-apply-static.spec.js`。
- GREEN: frontend syntax -> PASS, `node --check tests\e2e\mes-replan-whole-day-apply-real-flow.e2e.js`。
- GREEN: frontend schedule type check -> PASS, `pnpm ts:check:schedule`。
- GREEN: official login preflight -> PASS, `node scripts/preflight/login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/schedule-order --target-text 排产工单`，使用系统 Chrome 后真实登录进入目标页。
- GREEN: real Playwright E2E -> PASS, `node tests\e2e\mes-replan-whole-day-apply-real-flow.e2e.js`；status=PASS, scheduleOrder=SCH-TESTERPF102B88DA0E7-20260706-0001, workOrderId=925853, today=2026-07-06 00:00:00, tomorrow=2026-07-07 00:00:00。
- GREEN: readonly DB verification -> PASS, `mes_pro_task` 对 `work_order_id=925853` 查询返回最早任务时间 `2026-07-09 11:50:00`，任务数 `53`。
