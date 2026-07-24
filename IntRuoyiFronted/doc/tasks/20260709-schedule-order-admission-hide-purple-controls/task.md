# 20260709 待同步差异隐藏紫框控件

## 任务目标

- 排产工单页“待同步差异”弹窗中，隐藏截图紫框标出的控件。
- 不再显示额外筛选区中的工单编码、产品编号、入池状态、阻断原因。
- 不再显示动作区中的独立“搜索”按钮。
- 保留标准列表模板内置快速过滤、重置、选中工单加入排产工单池、汇总标签、表格列和分页。

## 里程碑

- [x] M1 建立任务文档、BDD 场景、设计约束与经验门禁。
- [x] M2 编写隐藏紫框控件静态契约，先得到 RED。
- [x] M3 移除目标控件渲染，保留原业务能力。
- [x] M4 运行静态契约和相关回归验证。
- [x] M5 更新任务记录并按仓库状态决定是否提交。

## 预期验证

- `node tests/e2e/mes-pro-schedule-order-admission-hide-purple-controls-static.spec.js`
- `node tests/e2e/mes-pro-schedule-order-admission-unified-list-template-static.spec.js`
- `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-schedule-order-admission-hide-purple-controls/frontend-feature-evidence.md`

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`；中文文件读写使用 UTF-8 aware 路径或 `apply_patch`，不使用默认编码写入。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本次只隐藏用户指定控件，不引入无关视觉重构。
- 前端功能交付：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`；按 BDD + TDD 记录证据。
- 本任务不涉及真实 E2E、服务器写入、数据库写入、发布、备份、恢复或 worktree 合并；无需执行高风险 `experience-preflight`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，直接移除用户指定控件的模板渲染，保留统一列表模板承载筛选入口。
- 是否存在临时补丁或绕过：否。

## Current Status

completed

## 当前状态

- Status: completed
- 已完成：定位截图控件位于 `src/views/mes/pro/scheduleorder/index.vue` 的待同步差异弹窗。
- 已完成：静态契约 RED 证明额外筛选项仍显示，随后移除额外筛选插槽和独立搜索按钮。
- 已完成：隐藏契约、待同步差异标准列表模板契约、排产工单池契约和前端 evidence 校验均通过。

## 最终验证结果

- RED: `node tests/e2e/mes-pro-schedule-order-admission-hide-purple-controls-static.spec.js` -> FAIL，待同步差异弹窗额外筛选区仍显示 `工单编码`。
- GREEN: `node tests/e2e/mes-pro-schedule-order-admission-hide-purple-controls-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-pro-schedule-order-admission-unified-list-template-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-schedule-order-admission-hide-purple-controls/frontend-feature-evidence.md` -> PASS。

## 提交状态

- Git commit -> BLOCKED，当前前端仓存在大量既有脏改，且 `src/views/mes/pro/scheduleorder/index.vue`、`tests/e2e/mes-pro-schedule-order-admission-unified-list-template-static.spec.js` 在本轮前已属于未提交改动范围；为避免混入非本任务 hunk，未创建提交。

## Cleanup Keep

- `doc/tasks/20260709-schedule-order-admission-hide-purple-controls/frontend-feature-evidence.md`
