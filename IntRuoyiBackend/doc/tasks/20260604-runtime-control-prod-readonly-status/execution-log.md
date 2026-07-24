# 执行日志：开放正式服只读状态与探针检查

- VERIFY：上一后端任务 `doc/tasks/20260604-runtime-control-rollback-target-backend/task.md` 已标记 `blocked`，避免与本任务混合。
- BDD: 正式服写动作继续受保护 -> Given 正式服发布、重启、回滚或恢复缺少 `PROD` 确认或有效候选 / When 操作员提交动作 / Then 后端继续阻断动作且不执行远端脚本。
- BDD: 正式服状态可见 -> Given `prod.accessEnabled=false` 用于保护写动作 / When 运行控制台加载概览 / Then 后端仍返回正式服组件状态卡片，并执行只读状态脚本。
- BDD: 正式服探针可检查 -> Given 探针配置包含正式服目标 / When 操作员运行探针 / Then 后端对正式服目标执行只读 HTTP 探针并返回真实 PASS/NO_GO/BLOCKED 结果。
- RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeProbeServiceImplTest,RuntimeControlHighRiskActionContractTest" test` -> FAIL，`RuntimeControlServiceImplTest.getOverviewShouldReadProductionStatusWhenWriteAccessIsDisabled` 期望 `running`，旧实现返回 `BLOCKED`。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeProbeServiceImplTest,RuntimeControlHighRiskActionContractTest" test` -> PASS，47 tests passed。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260604-runtime-control-prod-readonly-status/backend-api-evidence.md` -> PASS。
- CLOSEOUT PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-runtime-control-prod-readonly-status --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
