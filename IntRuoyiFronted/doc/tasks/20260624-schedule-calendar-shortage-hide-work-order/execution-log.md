# 执行日志：排程日历短缺弹框隐藏工单列

## 2026-06-24

- `BDD: 短缺弹框隐藏工单列 -> Given 用户打开短缺明细或预览问题弹框 / When 弹框表格显示 / Then 不显示工单列。`
- `BDD: 物料列与缺口保留 -> Given 短缺行包含物料编码、物料名称和缺口 / When 弹框表格显示 / Then 保留物料编码、物料名称和缺口列。`
- 已读取 `docs/experience-index.md`，本任务命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- `RED: node tests/e2e/mes-pro-schedule-calendar-shortage-dialog-columns-static.spec.js -> FAIL, expected reason: 短缺弹框仍包含 label=\"工单\"。`
- `GREEN: implementation -> PASS，移除短缺弹框工单列和仅服务该列的工单打开/展示辅助函数。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-shortage-dialog-columns-static.spec.js -> PASS。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-detail-cards-only-static.spec.js -> PASS。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-tabs-static.spec.js -> PASS。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-capacity-generation-static.spec.js -> PASS。`
- `GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS。`
- `GREEN: python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260624-schedule-calendar-shortage-hide-work-order/frontend-feature-evidence.md -> PASS。`
- `GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260624-schedule-calendar-shortage-hide-work-order --mode preview -> PASS，ready，无删除项。`
- `BLOCKER: real-ui-shortage-popup -> 当前测试数据近期已确认缺少可触发短缺弹框的日期；本轮不新增或改写业务数据，不用造数掩盖真实数据状态。`
