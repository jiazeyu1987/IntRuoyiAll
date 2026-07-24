# 20260709-schedule-order-sync-tab

## 任务目标

将排产工单页的“同步工单 / 待同步差异”从弹框交互改为页面页签交互，保留现有筛选、显示字段、分页、选择入池和接口契约。

## 里程碑

1. 已完成：记录 BDD 场景与 RED/GREEN 验证证据。
2. 已完成：将同步工单入口从 Dialog 切换为页签。
3. 已完成：保留待同步差异列表的统一列表模板、列配置、分页和入池动作。
4. 已完成：执行静态契约与类型检查验证。

## 预期验证

- `node tests/e2e/mes-schedule-order-sync-tab-static.spec.js`
- `node tests/e2e/mes-schedule-order-sync-without-promise-date-static.spec.js`
- `pnpm.cmd ts:check:schedule`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-schedule-order-sync-tab/frontend-feature-evidence.md`

## Current Status

completed

## 当前状态

已完成。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`，后续中文读写使用 UTF-8 路径。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，同步工单页签沿用统一列表模板与蓝/中性运营台样式。
- 真实 E2E：本轮先做静态契约与类型检查，不执行真实登录写入；如后续需要真实 E2E，需先读取登录文档并跑登录 preflight。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，直接调整交互结构为页签，不保留弹框兜底。
- 是否存在临时补丁或绕过：否。

## 完成记录

- 实现：`src/views/mes/pro/scheduleorder/index.vue` 增加 `scheduleOrderActiveTab` 页签状态，将“同步工单”按钮改为切换 `workOrderAdmission` 页签并加载待同步差异。
- 实现：移除“待同步差异”Dialog 块，待同步差异列表迁入“同步工单”页签，保留 `UnifiedListTemplate`、快速筛选、显示字段、分页、选择和入池动作。
- 测试：新增 `tests/e2e/mes-schedule-order-sync-tab-static.spec.js` 静态契约，确保同步工单不再以 Dialog 呈现。

## 最终验证结果

- RED: `node tests/e2e/mes-schedule-order-sync-tab-static.spec.js` -> FAIL, 缺少 `scheduleOrderActiveTab` 页签和同步工单页签。
- GREEN: `node tests/e2e/mes-schedule-order-sync-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-schedule-order-sync-without-promise-date-static.spec.js` -> PASS。
- GREEN: `pnpm.cmd ts:check:schedule` -> PASS。

## Cleanup Keep

- `doc/tasks/20260709-schedule-order-sync-tab/frontend-feature-evidence.md`
