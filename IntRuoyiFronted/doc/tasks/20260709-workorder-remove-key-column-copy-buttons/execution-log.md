# 执行日志：生产工单删除每列复制按钮

## BDD

- BDD: 删除生产工单关键列复制按钮 -> Given 用户打开生产工单列表 When 表格展示工单编号、产品编码、产品名称、规格型号、计划数量 Then 每列右侧不再显示复制按钮，工单编号仍可点击进入详情。

## TDD Evidence

- RED: `node tests/e2e/workorder-remove-key-column-copy-buttons-static.spec.js` -> FAIL，工单编号列仍渲染 `work-order-key-copy` 复制按钮。
- GREEN: `node tests/e2e/workorder-remove-key-column-copy-buttons-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/workorder-key-columns-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/workorder-code-copy-button-static.spec.js` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-workorder-remove-key-column-copy-buttons/frontend-feature-evidence.md` -> PASS。
- BLOCKER: git-commit -> 当前前端仓存在大量既有脏改；为避免混入非本任务 hunk，未创建提交。

## 变更记录

- 创建任务目录与任务文档，记录经验门禁、设计约束和 BDD 场景。
- 移除生产工单列表工单编号、产品编码、产品名称、规格型号、计划数量列的复制按钮。
- 移除 `handleCopyKeyField`、`handleCopyWorkOrderCode` 和剪贴板写入逻辑。
- 移除不再使用的 `.work-order-key-copy` 样式。
- 更新任务状态为 completed，并记录提交阻塞原因。
