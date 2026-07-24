# 执行日志：排程日历日详情弹框列调整

## 2026-06-24

- `BDD: 日详情弹框隐藏低价值列 -> Given 用户打开白班详情或任务详情弹框 / When 任务明细表显示 / Then 不显示任务、班次、时间、车间列。`
- `BDD: 工序产品拆列显示 -> Given 任务行包含工序名称、产品编码和产品名称 / When 任务明细表显示 / Then 分别显示工序、产品两列，不再显示工序 / 产品合并列。`
- 已读取 `docs/experience-index.md`，本任务命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- `RED: node tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js -> FAIL, expected reason: 日详情任务表仍包含 label=\"任务\"。`
- `GREEN: implementation -> PASS，移除日详情任务表任务/班次/时间/车间列，将工序 / 产品拆为工序、产品两列。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js -> PASS。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-detail-cards-only-static.spec.js -> PASS。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-tabs-static.spec.js -> PASS。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-capacity-generation-static.spec.js -> PASS。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-shortage-dialog-columns-static.spec.js -> PASS。`
- `GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS。`
- `GREEN: Select-String removed-column-check -> PASS，目标源码中未检出旧合并列和黄框列模板残留。`
- `GREEN: python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260624-schedule-calendar-day-summary-dialog-columns/frontend-feature-evidence.md -> PASS。`
- `GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260624-schedule-calendar-day-summary-dialog-columns --mode preview -> PASS，ready，无删除项。`
- `BLOCKER: real-ui-day-summary-popup -> 本轮未执行真实页面点击验证；本次为展示列静态调整，不新增或改写业务数据。`
