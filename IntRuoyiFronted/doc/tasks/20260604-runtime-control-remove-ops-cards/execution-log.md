# 执行日志：删除运行控制台三个运维卡片

- BDD: 三个运维卡片不再渲染 -> Given 操作员进入运行控制台 / When 页面加载完成 / Then 页面不显示 `站内信告警`、`责任人矩阵`、`备份演练` 三个卡片。
- BDD: 删除卡片不破坏责任人门禁 -> Given 操作员打开高风险操作弹窗 / When 前端需要展示责任人 / Then `ownerMatrix` 数据仍可加载并用于弹窗责任人提示。
- BDD: 删除卡片不隐藏错误 -> Given 运行控制台其它接口失败 / When 页面加载或操作 / Then 仍通过现有错误机制暴露失败，不新增 fallback 或静默成功。
- VERIFY: 上一前端任务 `doc/tasks/20260604-runtime-control-rollback-target-ui/task.md` 状态为 `completed`。
- RED: `node tests/e2e/runtime-control-remove-ops-cards-static.spec.js` -> FAIL，原因：页面仍 import `OpsAlertInboxCard`，三个卡片尚未删除。
- FIX: 删除 `OpsAlertInboxCard`、`OpsOwnerMatrixPanel`、`OpsBackupDrillPanel` 的页面使用、imports、组件文件、卡片专用 alert/backup 状态和旧可见性 E2E；保留 `ownerMatrix` 数据加载给高风险动作责任人门禁使用。
- FIX: 更新 `runtime-control-foolproof-static.spec.js`，把三个卡片改为禁止项，并把本地端口断言修正为当前固定入口 `8081/48081`，同时避免把“生成镜像标签”发布包说明误判为手工镜像标签输入。
- GREEN: `node tests/e2e/runtime-control-remove-ops-cards-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/runtime-control-foolproof-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/runtime-control-foolproof-timeout-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/runtime-control-ops-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/runtime-control-static.spec.js` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260604-runtime-control-remove-ops-cards\frontend-feature-evidence.md` -> PASS。
- CLOSEOUT PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-runtime-control-remove-ops-cards --mode preview` -> READY，delete `<none>`，blocked `<none>`，warnings `<none>`。
