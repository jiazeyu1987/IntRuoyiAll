# 工艺流程列表显示字段左移并删除重置列

## 任务目标

删除工艺流程列表工具栏右侧 `重置列` 按钮，并将 `显示字段` 按钮移动到截图红框位置，即快速过滤查询按钮右侧、导入导出操作左侧。

## 里程碑

1. 已完成：读取 PowerShell、经验索引、统一前端样式、前端交付契约和当前工艺流程列表实现。
2. 已完成：新增静态契约，先复现 `显示字段` 仍在右侧且 `重置列` 仍可见的 RED。
3. 已完成：最小调整 `显示字段` 位置并隐藏 `重置列`。
4. 已完成：运行目标静态测试、ESLint 和 TypeScript 检查。
5. 已完成：记录最终验证和提交状态。

## 预期验证

- `node tests/e2e/mes-pro-route-display-fields-left-no-reset-static.spec.js`
- `node tests/e2e/mes-pro-route-toolbar-remove-blue-actions-static.spec.js`
- `node tests/e2e/mes-pro-route-unified-list-template-static.spec.js`
- `node tests/e2e/mes-pro-route-actions.spec.js`
- `node node_modules/eslint/bin/eslint.js src/views/mes/pro/route/index.vue tests/e2e/mes-pro-route-display-fields-left-no-reset-static.spec.js tests/e2e/mes-pro-route-toolbar-remove-blue-actions-static.spec.js tests/e2e/mes-pro-route-unified-list-template-static.spec.js`
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm.cmd ts:check`

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；中文读写显式 UTF-8，命令串联不用 `&&`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；列表页保持紧凑工具栏、统一表格和标准分页。
- 前端交付：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`；不修改后端接口、权限、路由、数据状态，不引入 mock 或 fallback。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；通过标准模板的 `extra-filters` 插槽将显示字段放到快速过滤旁，并显式关闭重置列入口。
- `是否存在临时补丁或绕过`：否。

## 当前状态

COMPLETED：显示字段已移动到快速过滤查询按钮右侧，重置列入口已删除；目标静态契约、ESLint 和 TypeScript 检查均已通过。

## Current Status

completed
