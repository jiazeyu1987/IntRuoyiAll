# 执行日志：显示正式服只读状态与探针入口

- VERIFY：上一前端任务 `doc/tasks/20260604-runtime-control-ops-cards-visible/task.md` 已标记 `blocked`；并行旧任务 `doc/tasks/20260604-runtime-control-rollback-target-ui/task.md` 已标记 `blocked`。
- BDD: 正式服状态卡片可见 -> Given 后端返回正式服组件状态 / When 操作员打开运行控制台 / Then 页面显示正式服后端、前端、展厅和 OnlyOffice 状态卡片。
- BDD: 正式服探针入口可用 -> Given 操作员需要刷新探针 / When 查看探针面板 / Then 页面显示运行探针入口并展示正式服探针结果，不因为生产写动作保护而隐藏。
- BDD: 正式服写动作保护仍明确 -> Given 操作员尝试生产相关动作 / When 未输入 `PROD` 或候选不可用 / Then 页面继续阻止提交并展示阻断原因。
- RED: 后端 `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeProbeServiceImplTest,RuntimeControlHighRiskActionContractTest" test` -> FAIL，旧后端返回正式服 `BLOCKED/access-disabled`；前端已按后端状态渲染，无需生产代码修复。
- GREEN: `node tests\e2e\runtime-control-prod-readonly-status-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\runtime-control-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\runtime-control-ops-static.spec.js` -> PASS。
- CHECK: `pnpm ts:check` -> FAIL，Node 默认堆约 4GB OOM，非类型错误。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260604-runtime-control-prod-readonly-status/frontend-feature-evidence.md` -> PASS。
- CLOSEOUT PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-runtime-control-prod-readonly-status --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
