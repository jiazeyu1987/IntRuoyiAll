# 执行日志：排程日历短缺弹框拆分物料列

## 2026-06-24

- `BDD: 短缺弹框隐藏页签和说明列 -> Given 用户打开短缺明细或预览问题弹框 / When 弹框表格显示 / Then 不显示错误/阻塞、警告页签和说明列。`
- `BDD: 物料列拆成编码和名称 -> Given 短缺行包含物料编码和名称 / When 弹框表格显示 / Then 分别显示物料编码、物料名称两列，不再显示合并的物料列。`
- `BDD: 工单和缺口保留 -> Given 短缺行包含工单和缺口 / When 弹框表格显示 / Then 工单链接和缺口数值仍可见。`
- 已读取 `docs/experience-index.md`，本任务命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- `RED: node tests/e2e/mes-pro-schedule-calendar-shortage-dialog-columns-static.spec.js -> FAIL, expected reason: 短缺弹框仍包含 <el-tabs。`
- `GREEN: implementation -> PASS，移除短缺弹框顶部分组页签和说明列，物料拆分为物料编码/物料名称两列，保留工单链接与缺口列。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-shortage-dialog-columns-static.spec.js -> PASS。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-detail-cards-only-static.spec.js -> PASS。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-tabs-static.spec.js -> PASS。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-capacity-generation-static.spec.js -> PASS。`
- `GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS。`
- `GREEN: python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260624-schedule-calendar-shortage-material-split/frontend-feature-evidence.md -> PASS。`
- `GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260624-schedule-calendar-shortage-material-split --mode preview -> PASS，ready，无删除项。`
- `BLOCKER: real-ui-shortage-popup -> 当前本机测试数据此前已确认当前月及前 12 个月没有短缺数量大于 0 的日期；本轮未为测试新增或改写业务数据，避免用造数掩盖真实数据状态。`
