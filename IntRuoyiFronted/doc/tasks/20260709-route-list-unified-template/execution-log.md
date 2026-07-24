# 执行日志

## BDD

- `BDD: 工艺流程列表接入标准模板 -> Given 用户打开工艺流程列表 / When 页面渲染 / Then 列表使用 UnifiedListTemplate，快速过滤、显示字段、分页和表格在同一标准模板内呈现。`
- `BDD: 工艺流程列表保留业务操作 -> Given 用户具备工艺流程权限 / When 在标准模板工具栏和行操作中操作 / Then 新增、导入 Markdown、导入 Sheet1 Excel、导入路线 Excel、导出、编辑、复制、删除、状态切换和配置跳转保持原有权限与处理函数。`
- `BDD: 工艺流程列配置持久化 -> Given 用户调整显示字段或拖拽列宽 / When 列表触发列配置保存 / Then 使用 tableKey mes.pro.route.main 保存显示字段和列宽。`

## TDD

- `RED: node tests/e2e/mes-pro-route-unified-list-template-static.spec.js -> FAIL, 页面仍使用旧搜索栏，未导入 UnifiedListTemplate。`
- `GREEN: node tests/e2e/mes-pro-route-unified-list-template-static.spec.js -> PASS`
- `GREEN: node tests/e2e/mes-pro-route-actions.spec.js -> PASS`
- `GREEN: node node_modules/eslint/bin/eslint.js src/views/mes/pro/route/index.vue tests/e2e/mes-pro-route-unified-list-template-static.spec.js -> PASS`
- `GREEN: NODE_OPTIONS=--max-old-space-size=8192 pnpm.cmd ts:check -> PASS`
- `GREEN: python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-route-list-unified-template/frontend-feature-evidence.md -> PASS`
- `GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-route-list-unified-template --mode preview -> PASS`
- `BLOCKER: node tests/e2e/mes-pro-route-columns.spec.js -> FAIL, 非本轮 RouteForm.vue 缺少 ownerName 负责人字段，本轮仅改造工艺流程列表，不越界修复表单。`

## 实现记录

- `src/views/mes/pro/route/index.vue` 已接入 `UnifiedListTemplate`，稳定 tableKey 为 `mes.pro.route.main`。
- 路线编码、路线名称、状态迁入标准快速过滤；显示字段、列宽拖拽和分页由标准模板承载。
- 原有新增、导入 Markdown、导入 Sheet1 Excel、导入路线 Excel、导出、编辑、复制、删除、状态切换、排产配置和批记录配置入口保持不变。
- closeout preview 通过；`frontend-feature-evidence.md` 为预览删除候选，本轮未执行 apply。
