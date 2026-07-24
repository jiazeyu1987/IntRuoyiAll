# 执行日志：流转关系图详情字段单项删除修复

- BDD: 删除单个已选字段 -> Given 左侧详情已添加“工序名称”和“工艺要求” / When 用户点击“工艺要求”旁的删除按钮 / Then 只移除“工艺要求”，“工序名称”仍保留。
- BDD: 删除按钮不触发外层交互 -> Given 用户正在选中某个工序并查看已选字段 / When 点击字段删除按钮 / Then 点击事件不向外层图节点、画布或其它容器传播，不导致选择态和字段清单整体重置。
- BDD: 被删除字段回到可选项 -> Given 用户删除一个已选字段 / When 再打开“选择字段”下拉 / Then 被删除字段重新出现在可添加选项中，其它已选字段仍从下拉中过滤。
- RED: `node tests/e2e/mes-route-flow-selectable-detail-fields-static.spec.js` -> FAIL, expected reason: 删除按钮缺少 `@click.stop`，删除点击可能冒泡污染外层选择态或字段清单。
- IMPLEMENTED: 字段删除按钮改为 `@click.stop="handleRemoveProcessDetailField(field.key)"`，保留原有单项 `filter((key) => key !== fieldKey)` 删除逻辑。
- GREEN: `node tests/e2e/mes-route-flow-selectable-detail-fields-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-flow-graph-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-flow-selected-process-detail-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-flow-key-process-sidebar-static.spec.js` -> PASS。
- GREEN: `node node_modules/eslint/bin/eslint.js src/views/mes/pro/route/RouteFlowGraphDesigner.vue tests/e2e/mes-route-flow-selectable-detail-fields-static.spec.js` -> PASS。
- BLOCKER: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> FAIL，非本轮文件 `src/views/mes/pro/scheduleorder/index.vue` 缺少 `openWorkOrderAdmissionTab`、`scheduleOrderActiveTab`、`handleScheduleOrderTabChange`。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260709-route-flow-detail-field-delete-one/bug-regression-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-route-flow-detail-field-delete-one/frontend-feature-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-route-flow-detail-field-delete-one --mode preview` -> PASS，delete 仅为 evidence 文件；因全量类型检查被非本轮文件阻塞，未 apply、未提交。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS，原非本任务类型错误已不存在。
- GREEN: changed-static-regression -> PASS，当前变更静态测试共 14 个全部通过。
- GREEN: commit-boundary -> PASS，重叠的流转图文件已完成统一回归并纳入独立前端提交。
