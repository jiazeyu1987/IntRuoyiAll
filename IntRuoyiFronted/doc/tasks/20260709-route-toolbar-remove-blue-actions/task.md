# 工艺流程列表删除蓝框按钮

## 任务目标

删除工艺流程列表标准模板工具栏中截图蓝框内的 `搜索`、`重置`、`新增`、`导入 Markdown`、`导入 Sheet1 Excel` 按钮，并将 `导入路线 Excel` 改名为 `导入`；保留快速过滤内置查询、路线 Excel 导入、导出、显示字段和重置列能力。

## 里程碑

1. 已完成：读取 PowerShell、经验索引、前端交付契约和上一轮工艺流程列表任务记录。
2. 已完成：新增静态契约，先复现蓝框按钮仍存在的 RED。
3. 已完成：最小删除目标按钮和废弃函数。
4. 已完成：运行目标静态测试、ESLint 和 TypeScript 检查。
5. 已完成：按最新需求删除两个旧导入按钮并重命名路线 Excel 导入按钮，重新执行验证。

## 预期验证

- `node tests/e2e/mes-pro-route-toolbar-remove-blue-actions-static.spec.js`
- `node tests/e2e/mes-pro-route-unified-list-template-static.spec.js`
- `node tests/e2e/mes-pro-route-actions.spec.js`
- `node node_modules/eslint/bin/eslint.js src/views/mes/pro/route/index.vue tests/e2e/mes-pro-route-toolbar-remove-blue-actions-static.spec.js`
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm.cmd ts:check`

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；中文读写显式 UTF-8，命令串联不用 `&&`。
- 前端交付：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`；不修改后端接口、权限、路由和数据状态，不引入 mock 或 fallback。
- 前端列表样式：沿用 `UnifiedListTemplate`；本轮仅删除截图蓝框内多余按钮，并统一保留一个路线 Excel 导入入口。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；删除标准模板工具栏内重复的手动搜索/重置/新增入口，避免与快速过滤和导入导出操作混杂。
- `是否存在临时补丁或绕过`：否。

## 当前状态

COMPLETED：蓝框内搜索、重置、新增、导入 Markdown、导入 Sheet1 Excel 按钮已删除；路线 Excel 导入按钮已改名为 `导入`；目标静态契约、标准列表回归、行操作回归、ESLint、TypeScript 检查、前端证据校验和 closeout preview 均已通过。

## 完成记录

- 已删除工艺流程列表工具栏蓝框内 `搜索`、`重置`、`新增` 三个按钮。
- 已删除废弃的 `handleQuery`、`resetQuery` 函数。
- 已删除 `导入 Markdown`、`导入 Sheet1 Excel` 两个旧导入按钮及对应弹窗引用。
- 已将 `导入路线 Excel` 按钮文案改为 `导入`，继续使用路线 Excel 导入入口。
- 已保留快速过滤内置查询、导出、显示字段和重置列入口。
- 目标静态契约、标准列表回归、行操作回归、ESLint、TypeScript 检查、前端证据校验和 closeout preview 已通过。
- 收尾预览建议删除 `frontend-feature-evidence.md`，本轮仅执行 preview，不执行删除。
