# 任务：生产工单工具栏红框入口清理

## 任务目标

按用户截图删除生产工单列表工具栏红框内的 `全部展开`、`全部折叠` 和 `重置列` 入口；保留快速过滤、导出、增量同步、显示字段和保存能力。

## 经验门禁

- 已读取 `docs/powershell-memory.md`：本轮涉及 PowerShell 与中文文件读写，必须显式 UTF-8，不使用 `&&`。
- 已读取 `docs/experience-index.md`：命中“前端页面 / 表格 / 样式”，需遵循统一前端样式来源。
- 已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：本次只清理页面工具栏入口，不做无关视觉重设计。
- 已读取 `frontend-feature-delivery` 与 `references/frontend-contract.md`：前端行为变更需记录 BDD、RED/GREEN、入口、组件与验证证据。
- 本轮只修改本机前端源码、静态测试和任务文档；不操作服务器、不修改数据库、不改真实租户数据。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；只在生产工单页面隐藏本页不需要的工具栏入口，不修改通用组件行为。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 工具栏红框入口清理 -> Given 用户打开生产工单列表 / When 查看顶部工具栏 / Then 不再显示全部展开、全部折叠和重置列。
- BDD: 业务控件保留 -> Given 用户需要导出、同步或配置字段 / When 查看顶部工具栏 / Then 导出、增量同步、显示字段和保存仍可见。

## 里程碑

1. M1：建立任务文档与静态契约。`DONE`
2. M2：删除生产工单工具栏红框入口。`DONE`
3. M3：运行聚焦静态验证与类型检查。`DONE`
4. M4：完善证据文档并提交本任务改动。`DONE`

## 预期验证

- RED：`node tests/e2e/workorder-toolbar-red-box-cleanup-static.spec.js` 在旧页面上失败，证明红框入口仍存在。
- GREEN：`node tests/e2e/workorder-toolbar-red-box-cleanup-static.spec.js` 通过。
- REGRESSION：`node tests/e2e/workorder-red-box-filter-cleanup-static.spec.js` 通过，确认上一轮查询区清理未回退。
- TYPE：`pnpm ts:check:schedule` 通过或明确记录阻塞。

## 当前状态

COMPLETED：已删除生产工单工具栏红框内的 `全部展开`、`全部折叠` 和 `重置列`；静态契约、上一轮查询区回归、关键列回归、类型检查、证据校验和收尾清理预览均已通过。

## 验证结果

- GREEN：`node tests/e2e/workorder-toolbar-red-box-cleanup-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/workorder-red-box-filter-cleanup-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/workorder-key-columns-static.spec.js` -> PASS。
- GREEN：`pnpm ts:check:schedule` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260708-workorder-toolbar-red-box-cleanup\frontend-feature-evidence.md` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-workorder-toolbar-red-box-cleanup --mode preview` -> PASS，无删除项。

## Cleanup Keep

- `doc/tasks/20260708-workorder-toolbar-red-box-cleanup/frontend-feature-evidence.md`
