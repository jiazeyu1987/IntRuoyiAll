# 执行日志：排产工单调整按钮改为调整优先级

- BDD: 调整按钮仅调整优先级 -> Given 用户打开排产工单列表 / When 点击非冻结行“调整” / Then 弹出“调整优先级”弹窗，只展示当前优先级和新优先级输入。
- BDD: 优先级调整使用专用接口 -> Given 用户填写新优先级 / When 点击保存 / Then 前端调用 `MesProScheduleOrderApi.updatePriority`，只提交 `id` 和 `priorityNo`。
- BDD: 不恢复综合调整弹窗 -> Given 前端源码构建 / When 检查排产工单页面 / Then 不存在“调整排产工单”、`adjustForm.promiseDate`、`adjustForm.remark` 或综合 `updateScheduleOrder` 调整提交。

## 执行记录

- 已完成经验门禁读取：`docs/powershell-memory.md`、`docs/experience-index.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`、`frontend-feature-delivery`、`references/frontend-contract.md`。
- 已确认本轮不涉及真实 E2E 写入、服务器操作、数据库修改、发布或受保护资源变更。
- RED: `node tests/e2e/mes-schedule-order-priority-adjust-action-static.spec.js` -> FAIL，失败原因符合预期：当前页面尚未加回受 update 权限保护的“调整”按钮。
- GREEN: `node tests/e2e/mes-schedule-order-priority-adjust-action-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-schedule-order-remove-adjust-action-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-schedule-order-row-actions-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-pro-schedule-order-freeze-audit-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-schedule-order-frozen-state-static.spec.js` -> PASS。
- GREEN: `node --check tests/e2e/mes-pro-schedule-order-pool-real-flow.e2e.js` -> PASS。
- GREEN: `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check:schedule` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260708-schedule-order-priority-adjust-action/frontend-feature-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-schedule-order-priority-adjust-action --mode preview` -> PASS，无删除项。
