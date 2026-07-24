# 任务：排产工单调整按钮改为调整优先级

## 任务目标

将排产工单列表行操作中的“调整”按钮加回，但该按钮只用于调整订单优先级，不恢复上一版“调整排产工单”综合弹窗，不允许修改承诺交期、备注或修改原因。

## 经验门禁

- 已读取 `docs/powershell-memory.md`：本轮涉及 PowerShell 与中文文件读写，必须显式 UTF-8，不使用 `&&`。
- 已读取 `docs/experience-index.md`：命中“前端页面 / 表格 / 样式”，需遵循统一前端样式来源。
- 已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：本次只恢复一个行内文字入口和紧凑弹窗，不做无关视觉重设计。
- 已读取 `frontend-feature-delivery` 与 `references/frontend-contract.md`：前端行为变更需记录 BDD、RED/GREEN、入口、组件与验证证据。
- 本轮只修改本机前端源码、静态测试和任务文档；不操作服务器、不修改数据库、不改真实租户数据。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；复用既有 `/mes/pro/schedule-order/priority` 专用接口，避免综合更新接口误改交期或备注。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 调整按钮仅调整优先级 -> Given 用户打开排产工单列表 / When 点击非冻结行“调整” / Then 弹出“调整优先级”弹窗，只展示当前优先级和新优先级输入。
- BDD: 优先级调整使用专用接口 -> Given 用户填写新优先级 / When 点击保存 / Then 前端调用 `MesProScheduleOrderApi.updatePriority`，只提交 `id` 和 `priorityNo`。
- BDD: 不恢复综合调整弹窗 -> Given 前端源码构建 / When 检查排产工单页面 / Then 不存在“调整排产工单”、`adjustForm.promiseDate`、`adjustForm.remark` 或综合 `updateScheduleOrder` 调整提交。

## 里程碑

1. M1：建立任务文档与 RED 静态契约。`DONE`
2. M2：加回“调整”行操作和优先级弹窗。`DONE`
3. M3：更新既有排产工单静态/真实流契约。`DONE`
4. M4：运行聚焦验证并记录结果。`DONE`
5. M5：收尾清理预览并提交本次相关改动。`DONE`

## 预期验证

- RED：`node tests/e2e/mes-schedule-order-priority-adjust-action-static.spec.js` 先失败，证明当前页面尚未提供只调整优先级入口。
- GREEN：新增静态契约通过，既有排产工单行操作/工单池/冻结静态契约通过。
- REGRESSION：`node --check tests/e2e/mes-pro-schedule-order-pool-real-flow.e2e.js` 通过，`NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check:schedule` 通过。

## 当前状态

`COMPLETED`：排产工单“调整”按钮已加回为只调整优先级入口；聚焦静态契约、真实流脚本语法、排产 TypeScript 检查、前端证据校验和 task-closeout-cleanup 预览均已通过。

## Current Status

completed.

## 验证结果

- RED：`node tests/e2e/mes-schedule-order-priority-adjust-action-static.spec.js` -> FAIL，失败原因符合预期：当前页面尚未加回受 update 权限保护的“调整”按钮。
- GREEN：`node tests/e2e/mes-schedule-order-priority-adjust-action-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/mes-schedule-order-remove-adjust-action-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/mes-schedule-order-row-actions-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/mes-pro-schedule-order-freeze-audit-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/mes-schedule-order-frozen-state-static.spec.js` -> PASS。
- GREEN：`node --check tests/e2e/mes-pro-schedule-order-pool-real-flow.e2e.js` -> PASS。
- GREEN：`NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check:schedule` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260708-schedule-order-priority-adjust-action/frontend-feature-evidence.md` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-schedule-order-priority-adjust-action --mode preview` -> PASS，无删除项。

## Cleanup Keep

- `doc/tasks/20260708-schedule-order-priority-adjust-action/frontend-feature-evidence.md`
