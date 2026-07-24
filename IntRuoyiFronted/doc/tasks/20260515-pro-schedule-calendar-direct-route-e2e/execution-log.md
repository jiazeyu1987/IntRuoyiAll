# Execution Log: 生产排程日历直达路由恢复与 E2E

BDD: 生产排程日历支持真实直达路由 -> Given 用户已登录系统 / When 用户直接访问 `/mes/pro/schedule-calendar` / Then 页面应打开生产排程日历而不是 404。

BDD: 生产排程日历沿用冻结工单范围排除 -> Given 临时冻结功能已上线 / When 用户通过直达路由打开生产排程日历 / Then 页面涉及工单范围加载时仍应携带 `temporaryFrozen=false`。

RED: direct route `http://127.0.0.1:8081/mes/pro/schedule-calendar` -> FAIL, before the route patch the real page fell back to the frontend 404 route after login and could not render schedule-calendar content.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session schedule-calendar-direct-route run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-pro-schedule-calendar-direct-route-e2e\scripts\verify-schedule-calendar-direct-route.mjs` -> PASS, returned `{"url":"http://127.0.0.1:8081/mes/pro/schedule-calendar","title":"瑛泰管理系统 - 排程日历","requestUrl":"http://localhost:48081/admin-api/mes/pro/work-order/page?status=1&type=1&temporaryFrozen=false&pageNo=1&pageSize=200","httpCode":200,"apiCode":0}`.

GREEN: frontend commit -> PASS，`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` 提交 `15d3eceb 任务: 恢复排程日历直达路由`
