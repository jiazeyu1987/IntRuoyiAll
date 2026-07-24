# 执行日志：排程日历指标点击弹框 前端实现

BDD: 月格任务指标打开任务详情 -> Given 某日期存在任务统计 / When 点击该日期“任务”指标 / Then 页面应打开该日期任务详情弹框。

BDD: 月格工单指标打开工单详情 -> Given 某日期存在工单统计 / When 点击该日期“工单”指标 / Then 页面应打开该日期工单详情弹框。

BDD: 月格短缺指标打开短缺详情 -> Given 某日期存在短缺统计 / When 点击该日期“短缺”指标 / Then 页面应打开该日期短缺明细弹框。

BDD: 白班夜班区域只读 -> Given 月格展示白班和夜班数量 / When 点击白班或夜班区域 / Then 页面不应打开详情弹框，并继续保留“夜班由工艺排产路线配置”的说明。

GREEN: previous-task-check -> PASS，上一前端任务 `20260628-smart-scheduling-four-route-defaults` 已完成。
GREEN: scope-discovery -> PASS，已确认变更集中在 `src/views/mes/pro/task/calendar/index.vue` 与排程日历相关静态验证。
GREEN: experience-preflight -> PASS，已在真实登录/只读页面验证前完成 `docs/login-access.md` 门禁、PowerShell UTF-8 门禁和本次只读验证边界确认。
RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-month-metric-dialog-static.spec.js` -> FAIL，当前月格点击仍以整格班次编辑为主，缺少“任务 / 工单 / 短缺”独立弹框入口。
GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-month-metric-dialog-static.spec.js` -> PASS，月格指标点击契约已满足。
GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-detail-cards-only-static.spec.js` -> PASS，日详情卡片数量与点击契约回归通过。
GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-tabs-static.spec.js` -> PASS，排程规则页仍保留夜班配置说明。
GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js` -> PASS，任务详情弹框列未回退。
GREEN: `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/schedule-calendar --target-text 返回排产 --timeout 90000` -> PASS，真实登录前置通过。
GREEN: readonly-playwright-calendar-metric-click -> PASS，真实只读验证确认点击 `2026-06-01` 月格中的“任务 / 工单”红框可打开对应弹框，点击“白班”黄框不打开弹框；截图输出到 `output/playwright/20260628-schedule-calendar-metric-dialog-click/calendar-metric-dialog-click.png`。
