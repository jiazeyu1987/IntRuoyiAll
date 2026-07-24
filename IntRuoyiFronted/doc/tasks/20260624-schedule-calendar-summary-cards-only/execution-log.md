# 执行日志：排程日历日详情仅保留六个摘要卡片

## 2026-06-24

- `BDD: 日详情只展示六个卡片 -> Given 用户进入排程日历日详情 tab / When 详情加载完成 / Then 页面只显示任务、工单、白班、夜班、短缺、锁定六个摘要卡片，不内联显示物料汇总和任务列表。`
- `BDD: 点击卡片查看对应明细 -> Given 日详情卡片有数量 / When 用户点击任务、工单、白班、夜班、锁定或短缺 / Then 对应弹框展示任务、工单或短缺明细。`
- `BDD: 排程规则页签不受影响 -> Given 用户切换到排程规则 tab / When 查看规则表单 / Then 保存规则、模拟推进、产能生成、自动排产仍可见。`
- 已读取 `docs/experience-index.md`，本任务命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- `RED: node tests/e2e/mes-pro-schedule-calendar-detail-cards-only-static.spec.js -> FAIL, expected reason: 日详情 tab 仍内联显示 当日物料汇总。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-detail-cards-only-static.spec.js -> PASS，日详情 tab 只保留 6 个摘要卡片，内联明细已移除。`
- `REGRESSION: node tests/e2e/mes-pro-schedule-calendar-tabs-static.spec.js -> FAIL，既有页签合同仍要求 selectedDaySubtitle，和本次只显示 6 个卡片的新要求冲突；已更新该合同为校验六卡片入口。`
- `GREEN: node tests/e2e/mes-pro-schedule-calendar-capacity-generation-static.spec.js -> PASS。`
- `GREEN: node scripts/schedule-calendar-inline-shift-editor.test.mjs -> PASS，5 tests passed。`
- `GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS。`
- `GREEN: experience-preflight -> PASS，本次真实 Playwright 仅访问本机 http://localhost:8081，使用测试租户 aoteman 只读验证排程日历日详情布局，不操作测试服/正式服，不写入业务数据。`
- `GREEN: real-playwright-local-readonly -> PASS，默认进入 /mes/pro/schedule-calendar 后日详情 tab 只显示 6 个卡片、无内联物料汇总/任务列表，排程规则 tab 可切换。`
- `GREEN: real-playwright-local-click-dialog -> PASS，当前月无正数任务日期；通过页面月份切换向前 1 个月找到 2026-05-23 任务数 1，点击任务卡片成功打开对应明细弹框。`
- `GREEN: python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260624-schedule-calendar-summary-cards-only/frontend-feature-evidence.md -> PASS。`
- `GREEN: task-closeout-cleanup preview -> PASS，无删除项、无阻塞项、无警告。`
- 任务状态：已完成，等待提交。
