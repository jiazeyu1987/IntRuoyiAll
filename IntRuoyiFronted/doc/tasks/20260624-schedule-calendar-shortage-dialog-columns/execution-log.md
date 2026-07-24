# 执行日志：排程日历短缺弹框隐藏冗余列

## 2026-06-24

- `BDD: 短缺弹框隐藏红框列 -> Given 用户打开短缺明细或预览问题弹框 / When 弹框表格显示 / Then 不显示级别、工序、工作站列。`
- `BDD: 短缺弹框保留关键列 -> Given 用户打开短缺明细或预览问题弹框 / When 弹框表格显示 / Then 仍显示工单、物料、缺口、说明列。`
- `BDD: 工单链接保留 -> Given 短缺记录带工单 ID / When 用户点击工单 / Then 仍进入工单产线分析弹框。`
- 已读取 `docs/experience-index.md`，本任务命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- `RED: node tests/e2e/mes-pro-schedule-calendar-shortage-dialog-columns-static.spec.js -> FAIL, expected reason: 短缺弹框仍显示 label="级别"。`
- `GREEN: experience-preflight -> PASS，本次真实 Playwright 仅访问本机 http://localhost:8081，使用测试租户 aoteman 只读验证短缺弹框列，不操作测试服/正式服，不写入业务数据。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-shortage-dialog-columns-static.spec.js -> PASS，短缺弹框已隐藏级别、工序、工作站列，保留工单、物料、缺口、说明列。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-detail-cards-only-static.spec.js -> PASS。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-tabs-static.spec.js -> PASS。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-capacity-generation-static.spec.js -> PASS。`
- `GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS。`
- `BLOCKER: real-playwright-local-readonly -> 当前月份向前 12 个月均未找到短缺数 > 0 的日期，无法触发短缺弹框进行真实只读列验证。`
- `GREEN: python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260624-schedule-calendar-shortage-dialog-columns/frontend-feature-evidence.md -> PASS。`
- `GREEN: task-closeout-cleanup preview -> PASS，无删除项、无阻塞项、无警告。`
- 任务状态：已完成，等待提交。
