# 20260709-schedule-order-tab-controls-toolbar

## Current Status

completed

## 任务目标

将排产工单页顶部全局控制按钮迁入各自页签的工具栏区域：排产工单页签显示排产工单控制按钮，同步工单页签显示同步工单控制按钮与状态统计。

## 里程碑

1. 已完成：记录 BDD 场景与 RED/GREEN 验证证据。
2. 已完成：将排产工单控制按钮移入排产工单页签工具栏。
3. 已完成：将同步工单显示字段按钮补入同步工单页签工具栏。
4. 已完成：调整页签内工具栏样式，避免页签外重复按钮。
5. 已完成：完成静态契约、既有回归、TypeScript 与证据校验。

## 预期验证

- `node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js`
- `node tests/e2e/mes-schedule-order-sync-tab-static.spec.js`
- `node tests/e2e/mes-schedule-order-sync-without-promise-date-static.spec.js`
- `pnpm.cmd ts:check:schedule`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-schedule-order-tab-controls-toolbar/frontend-feature-evidence.md`

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`，中文读写必须使用 UTF-8 路径。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，按钮归位沿用统一列表模板的运营台工具栏样式。
- 真实 E2E：本轮仅做前端结构调整和静态契约验证，不执行真实登录写入；如后续要求真实浏览器验证，需先读取登录文档并跑登录 preflight。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，直接调整页签内控制区结构，不保留页签外重复按钮。
- 是否存在临时补丁或绕过：否。

## 完成记录

- 实现：移除排产工单 `ContentWrap` 标题栏按钮组，让控制按钮进入各自页签工具栏。
- 实现：排产工单页签工具栏保留同步工单、导出、手动重排、批量操作和排产工单显示字段。
- 实现：同步工单页签工具栏保留状态统计、重置、选中工单加入排产工单池和同步工单显示字段。
- 测试：新增 `tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js` 静态契约。
- 修正：排产工单页签列表增加独立 `schedule-order-pool__schedule-template`，强制按钮区换到筛选行下方的页签内容工具栏。
- 修正：删除顶部 `ContentWrap` 标题文案，让排产工单控制按钮回到筛选行右侧的页签内紫框位置。
- 修正：删除同步工单页签状态统计条，并将重置、入池、显示字段按钮移到筛选行右侧黄色位置。
- 修正：排产工单与同步工单列表占满页脚上方剩余空间，表头和分页固定，仅表体中间区域滚动。

## 最终验证结果

- RED: `node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js` -> FAIL, 页签外标题栏仍渲染同步工单等全局按钮。
- GREEN: `node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-schedule-order-sync-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-schedule-order-sync-without-promise-date-static.spec.js` -> PASS。
- GREEN: `pnpm.cmd ts:check:schedule` -> PASS。
- RED: `node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js` -> FAIL, 页面卡片未占满可视高度、同步工单仍使用固定 520 高度、不可排原因列仍限制宽度导致右侧空白。
- GREEN: `node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-schedule-order-sync-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-schedule-order-sync-without-promise-date-static.spec.js` -> PASS。
- GREEN: `pnpm.cmd ts:check:schedule` -> PASS。
- RED: `node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js` -> FAIL, 同步工单页签 actions 工具栏仍要求状态统计且按钮区仍可被强制换行。
- GREEN: `node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-schedule-order-sync-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-schedule-order-sync-without-promise-date-static.spec.js` -> PASS。
- GREEN: `pnpm.cmd ts:check:schedule` -> PASS。
- RED: `node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js` -> FAIL, 页面仍渲染 ContentWrap title 且排产工单 toolbar-actions 被强制换到筛选行下方。
- GREEN: `node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-schedule-order-sync-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-schedule-order-sync-without-promise-date-static.spec.js` -> PASS。
- GREEN: `pnpm.cmd ts:check:schedule` -> PASS。
- RED: `node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js` -> FAIL, 排产工单页签缺少独立 schedule template class 和 toolbar-actions 换行约束。
- GREEN: `node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-schedule-order-sync-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-schedule-order-sync-without-promise-date-static.spec.js` -> PASS。
- GREEN: `pnpm.cmd ts:check:schedule` -> PASS。

## Cleanup Keep

- `doc/tasks/20260709-schedule-order-tab-controls-toolbar/frontend-feature-evidence.md`
