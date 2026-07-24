# 任务：生产工单红框筛选区清理

## 任务目标

按用户截图删除生产工单列表查询区红框内的重复内容：快速过滤文字标签、工单编号、产品名称、产品编码、需求日期显式筛选项，以及重复的查询/重置按钮；保留快速过滤组件本身和导出、增量同步、展开折叠、显示字段等业务控件。

## 经验门禁

- 已读取 `docs/powershell-memory.md`：本轮涉及 PowerShell 与中文文件读写，必须显式 UTF-8，不使用 `&&`。
- 已读取 `docs/experience-index.md`：命中“前端页面 / 表格 / 样式”，需遵循统一前端样式来源。
- 已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：本次只清理查询栏重复内容，不做无关视觉重设计。
- 已读取 `frontend-feature-delivery` 与 `references/frontend-contract.md`：前端行为变更需记录 BDD、RED/GREEN、入口、组件与验证证据。
- 本轮只修改本机前端源码、静态测试和任务文档；不操作服务器、不修改数据库、不改真实租户数据。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；重复显式筛选入口删除后，筛选能力统一由 `TableQuickFilter` 承担。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 红框筛选区清理 -> Given 用户打开生产工单列表 / When 查看顶部查询区 / Then 不再显示快速过滤文字标签、工单编号、产品名称、产品编码、需求日期和重复查询/重置按钮。
- BDD: 业务控件保留 -> Given 用户需要执行生产工单业务操作 / When 查看查询区右侧 / Then 导出、增量同步、全部展开、全部折叠和显示字段配置仍可见。

## 里程碑

1. M1：建立任务文档与静态契约。`DONE`
2. M2：删除红框内重复查询内容。`DONE`
3. M3：运行聚焦静态验证与类型检查。`DONE`
4. M4：完善证据文档并提交本任务改动。`DONE`

## 预期验证

- RED：`node tests/e2e/workorder-red-box-filter-cleanup-static.spec.js` 在旧页面上失败，证明红框内容仍存在。
- GREEN：`node tests/e2e/workorder-red-box-filter-cleanup-static.spec.js` 通过。
- REGRESSION：`node tests/e2e/workorder-key-columns-static.spec.js` 通过，确认表格关键列未受影响。
- TYPE：`pnpm ts:check:schedule` 通过或明确记录阻塞。

## 当前状态

COMPLETED：已删除生产工单查询区红框内重复内容；静态契约、表格关键列回归、证据校验、类型检查和收尾清理预览均已通过。快速过滤组件与 Hook 已是受跟踪文件且无本次差异，生产工单页查询区改动作为本次可验证交付提交。

## 验证结果

- GREEN：`node tests/e2e/workorder-red-box-filter-cleanup-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/workorder-key-columns-static.spec.js` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260708-workorder-red-box-filter-cleanup\frontend-feature-evidence.md` -> PASS。
- GREEN：`pnpm ts:check:schedule` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-workorder-red-box-filter-cleanup --mode preview` -> PASS，无删除项。

## Cleanup Keep

- `doc/tasks/20260708-workorder-red-box-filter-cleanup/frontend-feature-evidence.md`
