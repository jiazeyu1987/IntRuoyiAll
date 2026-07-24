# 执行日志：待同步差异隐藏紫框控件

## BDD

- BDD: 隐藏紫框内控件 -> Given 用户打开排产工单页待同步差异弹窗 When 页面渲染同步工单列表 Then 截图紫框标出的额外筛选项和独立搜索按钮不显示，标准列表模板内置筛选、重置、加入排产工单池和表格仍保留。

## TDD Evidence

- RED: `node tests/e2e/mes-pro-schedule-order-admission-hide-purple-controls-static.spec.js` -> FAIL，待同步差异弹窗额外筛选区仍显示 `工单编码`。
- GREEN: `node tests/e2e/mes-pro-schedule-order-admission-hide-purple-controls-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-pro-schedule-order-admission-unified-list-template-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-schedule-order-admission-hide-purple-controls/frontend-feature-evidence.md` -> PASS。
- GREEN: `task-closeout-cleanup --task-id 20260709-schedule-order-admission-hide-purple-controls --mode preview` -> PASS，无阻塞；已将前端证据文件列入 Cleanup Keep。
- BLOCKER: git-commit -> 当前前端仓存在大量既有脏改，且本轮触达的 `scheduleorder/index.vue` 与统一列表模板静态契约文件已处于未提交改动范围；为避免混入非本任务 hunk，未创建提交。

## 变更记录

- 创建任务目录与任务文档，记录经验门禁、设计约束和 BDD 场景。
- 移除待同步差异弹窗 `extra-filters` 插槽，隐藏工单编码、产品编号、入池状态、阻断原因额外筛选项。
- 移除动作区独立“搜索”按钮，保留重置、汇总标签和“选中工单加入排产工单池”按钮。
- 运行隐藏契约、标准列表模板契约、排产工单池契约，均通过。
- 更新任务状态为 completed，并记录提交阻塞原因。
