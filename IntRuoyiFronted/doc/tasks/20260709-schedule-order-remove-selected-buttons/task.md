# 20260709-schedule-order-remove-selected-buttons

## Current Status

completed

## 任务目标

删除排产工单页截图红框选中的按钮：排产工单页签工具栏中的 `同步工单` 按钮，以及选中行后出现的 `批量冻结`、`批量解冻`、`批量删除` 按钮。

## 里程碑

1. 已完成：记录 BDD 场景与 RED 验证证据。
2. 已完成：删除目标按钮及未使用的前端入口逻辑。
3. 已完成：更新受影响静态契约。
4. 已完成：运行目标验证并记录结果。

## 预期验证

- `node tests/e2e/mes-schedule-order-remove-selected-buttons-static.spec.js`
- `node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js`
- `pnpm.cmd eslint src/views/mes/pro/scheduleorder/index.vue tests/e2e/mes-schedule-order-remove-selected-buttons-static.spec.js tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js`

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`，中文读写必须使用 UTF-8 路径。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，删除按钮时保留统一列表模板的运营台工具栏结构。
- 前端交付契约：已读取 `frontend-feature-delivery` 证据契约，记录目标、非目标、BDD 与验证证据。
- 真实 E2E：本轮仅删除前端按钮并做静态/类型验证，不执行真实登录写入；如后续要求真实浏览器验证，需先读取登录文档并跑登录 preflight。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，直接删除不应展示的工具栏按钮及对应未使用入口，不保留隐藏式兜底。
- 是否存在临时补丁或绕过：否。

## 完成记录

- 实现：删除排产工单页签工具栏中的 `同步工单`、`批量冻结`、`批量解冻`、`批量删除` 按钮。
- 实现：删除仅由上述工具栏按钮调用的 `openWorkOrderAdmissionTab`、`handleBatchFreeze`、`handleBatchUnfreeze`、`handleBatchDelete` 入口函数。
- 测试：新增 `mes-schedule-order-remove-selected-buttons-static` 静态契约，并更新原页签工具栏契约，要求不再渲染选中按钮。

## 最终验证结果

- RED: `node tests/e2e/mes-schedule-order-remove-selected-buttons-static.spec.js` -> FAIL, 排产工单页签工具栏仍渲染 `同步工单`。
- GREEN: `node tests/e2e/mes-schedule-order-remove-selected-buttons-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js` -> PASS。
- GREEN: `pnpm.cmd exec eslint src/views/mes/pro/scheduleorder/index.vue tests/e2e/mes-schedule-order-remove-selected-buttons-static.spec.js tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js --format stylish` -> PASS。

## Cleanup Keep

- `doc/tasks/20260709-schedule-order-remove-selected-buttons/frontend-feature-evidence.md`
