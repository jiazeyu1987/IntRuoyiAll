# 工艺流程列表接入标准列表模板

## 任务目标

将工艺流程列表页接入 `UnifiedListTemplate` 标准列表模板，保留现有查询、分页、导入导出、编辑、复制、删除、状态切换和跳转能力，同时增加标准快速过滤、显示字段自动保存、列宽拖拽持久化和统一分页。

## 里程碑

1. 已完成：读取 PowerShell、前端样式、前端交付和统一列表模板相关经验门禁。
2. 已完成：编写标准列表模板静态契约测试并先验证失败。
3. 已完成：按现有 API 和权限契约最小改造工艺流程列表页。
4. 已完成：运行目标静态测试、类型检查和前端证据校验。
5. 已完成：记录收尾清理预览、最终验证和提交状态。

## 预期验证

- `node tests/e2e/mes-pro-route-unified-list-template-static.spec.js`
- `node tests/e2e/mes-pro-route-columns.spec.js`
- `node tests/e2e/mes-pro-route-actions.spec.js`
- `pnpm.cmd ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-route-list-unified-template/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-route-list-unified-template --mode preview`

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；中文读写必须显式 UTF-8，命令串联不用 `&&`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；列表页必须采用蓝白灰运营台风格、紧凑工具栏、统一表格和分页。
- 前端交付：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`；保留现有 API、路由、权限和状态边界，不引入 mock、fallback 或静默降级。
- 真实 E2E：本任务当前为列表模板静态与类型契约改造，不执行登录后写入或服务器操作；若后续进入真实 E2E，需先读取 `docs/login-access.md` 并记录 `GREEN: experience-preflight -> PASS`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；仅接入标准模板和列配置，不新增兜底分支。
- `是否从根因和长期维护角度解决`：是；使用项目标准列表模板承载快速过滤、显示字段、列宽和分页，不复制旧搜索栏结构。
- `是否存在临时补丁或绕过`：否。

## 当前状态

COMPLETED：工艺流程列表已接入标准列表模板；目标静态契约、行操作回归、ESLint、TypeScript 检查、前端证据校验和 closeout preview 均通过。`mes-pro-route-columns.spec.js` 仍被非本轮 `RouteForm.vue` 负责人字段缺失阻塞，本轮不越界修改表单。

## 完成记录

- 已将旧搜索栏、独立列表和独立分页替换为 `UnifiedListTemplate`。
- 已接入快速过滤、显示字段、列宽拖拽持久化和标准分页，tableKey 为 `mes.pro.route.main`。
- 已保留新增、导入 Markdown、导入 Sheet1 Excel、导入路线 Excel、导出、编辑、复制、删除、状态切换、排产配置和批记录配置入口。
- 收尾预览通过；预览建议删除 `frontend-feature-evidence.md`，本轮仅执行 preview，不执行删除。
