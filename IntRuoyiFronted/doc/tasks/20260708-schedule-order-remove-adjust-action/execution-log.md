# 执行日志：删除排产工单调整按钮

- BDD: 排产工单行操作不再显示调整 -> Given 用户打开排产工单列表 / When 查看非冻结排产工单行操作 / Then 行内不再展示“调整”按钮，也不会绑定 `openAdjustDialog(row)`。
- BDD: 交期入口继续可用 -> Given 用户需要修改承诺交期 / When 查看非冻结排产工单行操作 / Then 仍可点击“交期”进入承诺交期弹窗。
- BDD: 调整专用弹窗被清理 -> Given 前端源码构建 / When 检查排产工单页面 / Then 不再存在“调整排产工单”弹窗、`adjustForm`、`submitScheduleOrderAdjust` 等专用代码。

## 执行记录

- 已完成经验门禁读取：`docs/powershell-memory.md`、`docs/experience-index.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`、`frontend-feature-delivery`、`references/frontend-contract.md`。
- 已确认本轮不涉及真实 E2E 写入、服务器操作、数据库修改、发布或受保护资源变更。
- RED: `node tests/e2e/mes-schedule-order-remove-adjust-action-static.spec.js` -> FAIL，失败原因符合预期：旧页面仍包含 `openAdjustDialog` 调整入口。
- GREEN: `node tests/e2e/mes-schedule-order-remove-adjust-action-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-schedule-order-row-actions-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-pro-schedule-order-freeze-audit-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-schedule-order-frozen-state-static.spec.js` -> PASS。
- GREEN: `node --check tests/e2e/mes-pro-schedule-order-pool-real-flow.e2e.js` -> PASS。
- GREEN: `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check:schedule` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260708-schedule-order-remove-adjust-action/frontend-feature-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-schedule-order-remove-adjust-action --mode preview` -> PASS，无删除项。
