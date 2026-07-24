# 执行日志

- 2026-06-16：创建任务记录，确认目标组件为 `src/views/mes/pro/route-use/RouteUsePage.vue`。
- BDD: 红框区域不显示 -> Given 用户打开工艺排产路线配置弹窗 / When 页面渲染排产用途配置 / Then 弹窗摘要不显示用途标签、启用工序预览条和日历规则列。
- BDD: 产能与公式列按新文案显示 -> Given 用户查看排产用途配置表格 / When 表格渲染排产字段 / Then 小时产能输入保留 2 位小数，公式 a 列显示“系数”，公式 b 列显示“固定值”。
- RED: `node tests\e2e\mes-route-use-config-display-static.spec.js` -> FAIL，当前配置弹窗摘要区域仍显示用途标签。
- GREEN: `node tests\e2e\mes-route-use-config-display-static.spec.js` -> PASS。
- REGRESSION: `node tests\e2e\mes-process-use-route-tabs-static.spec.js` -> FAIL，历史断言仍期待旧单批记录 `scope.row.batchRecordReportId`；`HEAD` 当前组件已使用多批记录 `report.batchRecordReportId`。
- 2026-06-16：修正 `mes-process-use-route-tabs-static.spec.js` 的批记录下拉契约，使其与当前多批记录实现及 `mes-edhr-multi-batch-route-static.spec.js` 保持一致。
- GREEN: `node tests\e2e\mes-process-use-route-tabs-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-edhr-multi-batch-route-static.spec.js` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: experience-preflight -> PASS，本次只使用本机 `http://localhost:8081` 做只读页面验证，已读取登录门禁，不访问测试服/正式服，不执行保存写入。
- BLOCKER: Browser modal visual check -> 测试租户/aoteman 登录预检失败，返回“账号密码不正确”；本机 `芋道源码/admin` 可只读进入 `/mes/pro/schedule-route`，但列表为 0 条，无法打开目标配置弹窗。未使用 mock、接口绕过或创建数据替代。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260616-route-use-config-display-tuning\frontend-feature-evidence.md` -> PASS。
- GREEN: `git diff --check -- src\views\mes\pro\route-use\RouteUsePage.vue tests\e2e\mes-route-use-config-display-static.spec.js tests\e2e\mes-process-use-route-tabs-static.spec.js doc\tasks\20260616-route-use-config-display-tuning` -> PASS，仅 LF/CRLF 提示。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260616-route-use-config-display-tuning --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
