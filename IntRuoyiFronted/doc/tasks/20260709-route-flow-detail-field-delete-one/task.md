# 流转关系图详情字段单项删除修复

## 任务目标

- 修复流转关系图左侧“选择字段”已添加字段删除行为：删除一个字段时只能移除当前字段，不能清空全部已选字段。
- 保留字段添加、字段可选项回补、工序选中、关键工序设置、连接线编辑和底部保存原有行为。
- 本轮仅修改本机前端源码、静态回归测试和任务证据，不改后端接口、不操作服务器、不执行真实写入型 E2E。

## 里程碑

- [x] M1 创建任务记录，读取 PowerShell、前端样式、缺陷修复和前端交付门禁。
- [x] M2 先补 RED 静态回归，锁定单项删除契约。
- [x] M3 最小修复删除按钮事件与删除处理契约。
- [x] M4 运行目标静态回归、lint / 类型检查和证据校验。
- [x] M5 预览任务收尾清理，按混合工作区提交边界处理本轮改动。

## 预期验证

- `node tests/e2e/mes-route-flow-selectable-detail-fields-static.spec.js`
- `node tests/e2e/mes-route-flow-graph-static.spec.js`
- `node tests/e2e/mes-route-flow-selected-process-detail-static.spec.js`
- `node tests/e2e/mes-route-flow-key-process-sidebar-static.spec.js`
- `node node_modules/eslint/bin/eslint.js src/views/mes/pro/route/RouteFlowGraphDesigner.vue tests/e2e/mes-route-flow-selectable-detail-fields-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260709-route-flow-detail-field-delete-one/bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-route-flow-detail-field-delete-one/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-route-flow-detail-field-delete-one --mode preview`

## BDD 场景

- BDD: 删除单个已选字段 -> Given 左侧详情已添加“工序名称”和“工艺要求” / When 用户点击“工艺要求”旁的删除按钮 / Then 只移除“工艺要求”，“工序名称”仍保留。
- BDD: 删除按钮不触发外层交互 -> Given 用户正在选中某个工序并查看已选字段 / When 点击字段删除按钮 / Then 点击事件不向外层图节点、画布或其它容器传播，不导致选择态和字段清单整体重置。
- BDD: 被删除字段回到可选项 -> Given 用户删除一个已选字段 / When 再打开“选择字段”下拉 / Then 被删除字段重新出现在可添加选项中，其它已选字段仍从下拉中过滤。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；中文读写显式 UTF-8，不使用 `&&`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本次只修复交互行为，不做视觉重设计。
- 缺陷回归修复：已读取 `bug-regression-fix-loop` 与 `bug-contract.md`；先补可失败回归，再做最小修复。
- 前端交付：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`；保留现有 API 和状态边界，不引入 mock、fallback 或吞异常。
- 高风险动作：本轮不登录、不写入真实业务数据、不操作测试服/正式服，因此不触发登录和服务器写入门禁。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。删除按钮必须具备单项删除和事件隔离契约，避免点击删除污染外层选择态或整体清单状态。
- 是否存在临时补丁或绕过：否。

## 当前状态

COMPLETED：删除单项字段问题已完成最小修复；目标静态回归、全部当前变更静态测试、ESLint、证据校验和全量 `pnpm.cmd ts:check` 均通过，原非本任务类型检查阻塞已不存在。

## Current Status

completed
