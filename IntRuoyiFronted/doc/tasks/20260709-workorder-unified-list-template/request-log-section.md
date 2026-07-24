# 2026-07-09 生产工单列表替换标准列表模板

## 用户需求

- 将生产订单列表替换成标准列表模板。

## 执行命令

- `Get-Content -Encoding utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md` -> PASS，读取前端交付技能。
- `Get-Content -Encoding utf8 docs/powershell-memory.md` -> PASS，读取 PowerShell / UTF-8 门禁。
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md` -> PASS，读取统一前端样式。
- `Get-Content -Encoding utf8 docs/experience-index.md` -> PASS，确认本任务命中 PowerShell 与前端页面 / 表格 / 样式门禁。
- `apply_patch` -> 创建 `doc/tasks/20260709-workorder-unified-list-template/` 任务文档、前端证据和生产工单标准模板静态验收用例。
- `node tests/e2e/mes-pro-workorder-unified-list-template-static.spec.js` -> RED FAIL，生产工单页尚未导入并使用 `UnifiedListTemplate`。
- `apply_patch` -> 将 `src/views/mes/pro/workorder/index.vue` 接入 `UnifiedListTemplate`，保留快速过滤、显示字段、列宽拖拽、分页和原业务动作。
- `apply_patch` -> 更新生产工单相关静态契约，适配通过标准列表模板渲染的工具栏与分页。
- `node tests/e2e/mes-pro-workorder-unified-list-template-static.spec.js`、`node tests/e2e/workorder-key-columns-static.spec.js`、`node tests/e2e/workorder-toolbar-red-box-cleanup-static.spec.js`、`node tests/e2e/unified-list-template-static.spec.js`、`node tests/e2e/user-table-column-config-static.spec.js`、`node tests/e2e/table-quick-filter-static.spec.js` -> GREEN PASS。
- `pnpm ts:check:schedule`、`NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` -> GREEN PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-workorder-unified-list-template/frontend-feature-evidence.md` -> GREEN PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-workorder-unified-list-template --mode preview` -> GREEN PASS，无阻塞。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-workorder-unified-list-template --mode apply` -> GREEN PASS，删除临时前端证据文件，无阻塞。
- `git commit` -> BLOCKED，当前前端仓存在大量前置未提交改动，且本轮修改的生产工单页与相关静态测试存在文件级重叠；为避免混入非本轮改动，未创建提交。
