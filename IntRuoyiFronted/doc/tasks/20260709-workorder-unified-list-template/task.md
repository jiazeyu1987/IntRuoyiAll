# Task: 生产工单列表替换标准列表模板

## 任务目标

- 将 `src/views/mes/pro/workorder/index.vue` 的生产工单列表接入 `UnifiedListTemplate`。
- 保留现有快速过滤、显示字段自动保存、列宽拖拽持久化、分页、同步状态栏、导出、增量同步和行操作。
- 不修改后端接口、权限、分页、排序、业务按钮逻辑和测试数据。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已按 `docs/powershell-memory.md` 执行，读写中文文件必须显式 UTF-8，命令串联不使用 `&&`。
- 前端页面 / 表格 / 样式：必须遵循 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，列表页接入标准模板时保留快速过滤、显示字段自动保存、列宽拖拽持久化和分页。
- 高风险动作：本任务为前端静态页面改造，不执行真实 E2E 写入、服务器操作、数据库 schema 修改、发布、备份或恢复。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，统一生产工单列表和既有标准列表模板的结构，减少页面重复工具栏/分页实现。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: workorder_list_uses_unified_template -> Given 用户打开 MES 生产工单列表 / When 页面渲染主列表 / Then 快速过滤、显示字段、表格和分页应由 UnifiedListTemplate 统一承载。`
- `BDD: workorder_business_actions_are_preserved -> Given 管理员查看生产工单列表 / When 页面切换到标准模板后 / Then 导出、增量同步、列宽拖拽和行级业务动作仍保留原权限与事件。`

## 里程碑

- [x] M1: 读取经验门禁、前端技能和统一前端样式。
- [x] M2: 新增生产工单标准模板静态验收用例并记录 RED。
- [x] M3: 改造生产工单列表接入 `UnifiedListTemplate`。
- [x] M4: 运行静态测试与类型检查。
- [x] M5: 任务收尾清理完成，并记录提交阻塞。

## 预期验证

- `node tests/e2e/mes-pro-workorder-unified-list-template-static.spec.js`
- `node tests/e2e/workorder-key-columns-static.spec.js`
- `node tests/e2e/workorder-toolbar-red-box-cleanup-static.spec.js`
- `node tests/e2e/unified-list-template-static.spec.js`
- `node tests/e2e/user-table-column-config-static.spec.js`
- `node tests/e2e/table-quick-filter-static.spec.js`
- `pnpm ts:check:schedule`
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-workorder-unified-list-template/frontend-feature-evidence.md`

## 当前状态

- 已完成：生产工单列表已接入 `UnifiedListTemplate`；静态契约、类型检查、证据校验与收尾清理预览均已通过。

## Current Status

completed

## 已完成验证

- `node tests/e2e/mes-pro-workorder-unified-list-template-static.spec.js` -> PASS。
- `node tests/e2e/workorder-key-columns-static.spec.js` -> PASS。
- `node tests/e2e/workorder-toolbar-red-box-cleanup-static.spec.js` -> PASS。
- `node tests/e2e/unified-list-template-static.spec.js` -> PASS。
- `node tests/e2e/user-table-column-config-static.spec.js` -> PASS。
- `node tests/e2e/table-quick-filter-static.spec.js` -> PASS。
- `pnpm ts:check:schedule` -> PASS。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` -> PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-workorder-unified-list-template/frontend-feature-evidence.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-workorder-unified-list-template --mode preview` -> PASS，无阻塞。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-workorder-unified-list-template --mode apply` -> PASS。

## 提交阻塞

- `git commit` 未执行完成：当前前端仓存在大量前置未提交改动，且本轮修改的生产工单页与相关静态测试存在文件级重叠。
- 为避免提交混入非本轮改动，保留工作区改动并记录阻塞；待前置改动归属清理或授权统一提交后，可再单独提交本任务。
