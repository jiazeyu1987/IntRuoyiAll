# 执行日志：补全运行控制台恢复数据前端操作

BDD: 恢复数据可从前端选择目标环境 -> Given 操作员打开运行控制台恢复数据弹窗 / When 查看目标环境区域 / Then 只能选择测试服或备份服务器，不出现正式服作为恢复目标。

BDD: 恢复数据提交携带前端目标环境 -> Given 操作员选择恢复候选、目标环境和原因 / When 点击确认执行 / Then 前端请求必须携带 `targetEnvironment` 与 `selectedBackupCandidateId`，不得要求操作员直接调用接口。

BDD: 恢复数据正式服隔离提示明确 -> Given 操作员准备执行恢复数据 / When 查看预期结果和风险提示 / Then 页面必须说明仅覆盖所选测试/备份目标环境，并禁止影响正式服务器程序和数据。

VERIFY: 上一前端任务 `doc/tasks/20260603-runtime-control-recent-operations-visible/task.md` 状态为 `completed`。

RED: `node tests/e2e/runtime-control-restore-target-static.spec.js` -> FAIL，expected reason：`RuntimeControlTargetEnvironment` 仍为 `test | prod`，恢复数据弹窗没有 `restoreTargetEnvironmentOptions`，提交 payload 没有恢复数据目标环境。

GREEN: `node tests/e2e/runtime-control-restore-target-static.spec.js` -> PASS，恢复数据前端目标环境契约已补齐。

GREEN: `node tests/e2e/runtime-control-ops-static.spec.js` -> PASS。

GREEN: `node tests/e2e/runtime-control-static.spec.js` -> PASS。

GREEN: `node tests/e2e/runtime-control-release-package-static.spec.js` -> PASS。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

GREEN: `RUNTIME_CONTROL_E2E_BASE_URL=http://localhost:8081 node tests/e2e/runtime-control-restore-data.e2e.js` -> PASS，只打开恢复数据弹窗验证恢复目标和候选选择，未提交恢复动作。

GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260604-runtime-control-restore-target-ui/frontend-feature-evidence.md` -> PASS。

CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-runtime-control-restore-target-ui --mode preview` -> READY，delete `<none>`，blocked `<none>`，warnings `<none>`。

BLOCKER: 后端当前若仍只允许 `backup-now` 携带 `targetEnvironment`，真实恢复数据提交会被服务端拒绝；本任务不绕过后端接口。
