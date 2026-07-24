# 执行日志：排程日历日详情产线显示名称

## 2026-06-24

- `BDD: 日详情产线显示名称 -> Given 用户打开白班详情或任务详情弹框 / When 任务明细表显示 / Then 产线列显示产线名称而不是产线编码/名称拼接。`
- `BDD: 工单汇总产线聚合名称 -> Given 用户打开工单详情弹框 / When 工单汇总表显示产线 / Then 聚合产线名称而不是产线编码/名称拼接。`
- 已读取 `docs/experience-index.md`，本任务命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- `RED: node tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js -> FAIL, expected reason: 日详情任务表仍直接绑定 prop=\"lineTitle\"。`
- `GREEN: implementation -> PASS，新增 lineNameTitle，任务明细产线列与工单汇总产线聚合改用产线名称。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js -> PASS。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-detail-cards-only-static.spec.js -> PASS。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-tabs-static.spec.js -> PASS。`
- `GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS。`
- `GREEN: python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260624-schedule-calendar-day-summary-line-name/frontend-feature-evidence.md -> PASS。`
- `GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260624-schedule-calendar-day-summary-line-name --mode preview -> PASS，ready，无删除项。`
- `BLOCKER: real-ui-day-summary-popup -> 本轮未执行真实页面点击验证；本次为展示字段静态调整，不新增或改写业务数据。`
