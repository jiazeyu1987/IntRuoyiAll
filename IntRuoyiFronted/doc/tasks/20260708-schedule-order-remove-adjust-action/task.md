# 任务：删除排产工单调整按钮

## 任务目标

删除排产工单列表行操作中的“调整”按钮，并清理该按钮专用的调整弹窗、前端状态和提交方法；保留“交期”按钮作为承诺交期修改入口，不修改后端接口、不改变冻结、完成、撤销、追溯等其它行操作。

## 经验门禁

- 已读取 `docs/powershell-memory.md`：本轮涉及 PowerShell 与中文文件读写，必须显式 UTF-8，不使用 `&&`。
- 已读取 `docs/experience-index.md`：命中“前端页面 / 表格 / 样式”，需遵循统一前端样式来源。
- 已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：本次仅删除行内入口，不做无关视觉重设计，保留密集表格行操作风格。
- 已读取 `frontend-feature-delivery` 与 `references/frontend-contract.md`：前端行为变更需记录 BDD、RED/GREEN、入口、组件与验证证据。
- 本轮只修改本机前端源码、静态测试和任务文档；不操作服务器、不修改数据库、不改真实租户数据。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；直接删除废弃入口及其专用状态/方法，避免隐藏按钮后留下死代码。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 排产工单行操作不再显示调整 -> Given 用户打开排产工单列表 / When 查看非冻结排产工单行操作 / Then 行内不再展示“调整”按钮，也不会绑定 `openAdjustDialog(row)`。
- BDD: 交期入口继续可用 -> Given 用户需要修改承诺交期 / When 查看非冻结排产工单行操作 / Then 仍可点击“交期”进入承诺交期弹窗。
- BDD: 调整专用弹窗被清理 -> Given 前端源码构建 / When 检查排产工单页面 / Then 不再存在“调整排产工单”弹窗、`adjustForm`、`submitScheduleOrderAdjust` 等专用代码。

## 里程碑

1. M1：建立任务文档与 RED 静态契约。`DONE`
2. M2：删除页面“调整”入口与专用调整逻辑。`DONE`
3. M3：同步更新既有静态/真实流测试契约。`DONE`
4. M4：运行聚焦验证并记录结果。`DONE`
5. M5：收尾清理预览并提交本次相关改动。`DONE`

## 预期验证

- RED：`node tests/e2e/mes-schedule-order-remove-adjust-action-static.spec.js` 先失败，证明旧页面仍存在“调整”入口或专用代码。
- GREEN：新增静态契约通过，既有排产工单静态契约通过。
- REGRESSION：排产工单页面 TypeScript 聚焦校验通过。

## 当前状态

`COMPLETED`：排产工单“调整”按钮、专用弹窗、状态和提交方法已删除；聚焦静态契约、真实流脚本语法、排产 TypeScript 检查、前端证据校验和 task-closeout-cleanup 预览均已通过。

## Current Status

completed.

## 验证结果

- RED：`node tests/e2e/mes-schedule-order-remove-adjust-action-static.spec.js` -> FAIL，失败原因符合预期：旧页面仍包含 `openAdjustDialog` 调整入口。
- GREEN：`node tests/e2e/mes-schedule-order-remove-adjust-action-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/mes-schedule-order-row-actions-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/mes-pro-schedule-order-freeze-audit-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/mes-schedule-order-frozen-state-static.spec.js` -> PASS。
- GREEN：`node --check tests/e2e/mes-pro-schedule-order-pool-real-flow.e2e.js` -> PASS。
- GREEN：`NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check:schedule` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260708-schedule-order-remove-adjust-action/frontend-feature-evidence.md` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-schedule-order-remove-adjust-action --mode preview` -> PASS，无删除项。

## Cleanup Keep

- `doc/tasks/20260708-schedule-order-remove-adjust-action/frontend-feature-evidence.md`
