# 执行日志：排产工单行操作收缩为查看、调整、冻结

## 2026-06-27

- 初始化任务：根据用户需求“排产工单里，只保留查看、调整、冻结三个按钮，其他的都删除”，创建前端任务并限定在排产工单行操作区。
- BDD: 排产工单行操作仅保留三个入口 -> Given 用户打开排产工单列表 / When 页面渲染任一行操作列 / Then 只显示查看、调整、冻结三个按钮，不再显示解冻或更多下拉。
- BDD: 已冻结排产工单不再显示解冻按钮 -> Given 排产工单已冻结 / When 页面渲染行操作列 / Then 仍只保留查看、调整、冻结三个入口，其中冻结按钮可见但不可再次执行。
- INVESTIGATION: `src/views/mes/pro/scheduleorder/index.vue` -> PASS，确认当前行操作区包含 `查看`、`调整`、`解冻/冻结` 与 `更多` 下拉，`更多` 内仍暴露对比、快照、删除、追溯。
- RED: `node tests/e2e/mes-schedule-order-row-actions-static.spec.js` -> FAIL，`AssertionError: 排产工单行操作不应再包含：openUnfreezeDialog(row)`，证明当前行操作区仍残留解冻入口。
- CHANGE: `src/views/mes/pro/scheduleorder/index.vue` -> 删除行操作中的 `解冻` 按钮和 `更多` 下拉，只保留 `查看`、`调整`、`冻结` 三个按钮；冻结按钮在已冻结行显示为禁用态。
- CHANGE: `tests/e2e/mes-schedule-order-row-actions-static.spec.js` -> 新增只校验行操作列片段的静态契约，避免把工具栏批量解冻和弹窗文案误判为行按钮。
- GREEN: `node tests/e2e/mes-schedule-order-row-actions-static.spec.js` -> PASS，确认行操作列仅保留查看、调整、冻结三个入口。
- GREEN: `node tests/e2e/mes-schedule-order-route-progress-view-static.spec.js` -> PASS，确认已有查看工艺排产路线的静态契约未回归。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260627-mes-schedule-order-row-actions-trim/frontend-feature-evidence.md` -> PASS，确认前端交付证据结构完整。
