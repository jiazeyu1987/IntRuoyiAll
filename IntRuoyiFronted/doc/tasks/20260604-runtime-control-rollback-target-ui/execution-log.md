# 执行日志：运行控制台回滚版本目标环境前端操作

## BDD

- BDD: 回滚版本可选择目标环境 -> Given 操作员打开“回滚版本”弹窗 / When 查看目标环境区域 / Then 只能选择测试服或备份服务器。
- BDD: 回滚版本提交携带目标环境 -> Given 操作员选择目标环境、版本候选和原因 / When 点击确认执行 / Then 请求必须携带 `targetEnvironment` 与 `selectedImageCandidateId`。
- BDD: 回滚版本生产隔离提示明确 -> Given 操作员准备回滚应用版本 / When 查看预期结果 / Then 页面说明只回滚应用版本，不恢复数据，不影响正式服务器程序和数据。

## RED

- RED: `node tests/e2e/runtime-control-rollback-target-static.spec.js` -> FAIL，原因：前端缺少 `rollbackTargetEnvironmentOptions`，回滚版本弹窗没有目标环境选择契约。

## GREEN

- GREEN: `node tests/e2e/runtime-control-rollback-target-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/runtime-control-restore-target-static.spec.js` -> PASS，确认新增回滚目标环境没有破坏恢复数据目标环境契约。
- GREEN: `node tests/e2e/runtime-control-ops-static.spec.js` -> PASS。
- GREEN: `node --check tests/e2e/runtime-control-rollback-app.e2e.js` -> PASS。
- GREEN: `$env:RUNTIME_CONTROL_E2E_BASE_URL='http://localhost:8081'; node tests/e2e/runtime-control-rollback-app.e2e.js` -> PASS，只打开“回滚版本”弹窗验证回滚目标和候选选择，未提交回滚动作。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: frontend feature evidence validator -> PASS。

## REGRESSION

- GREEN: `git diff --check` -> PASS，仅有 Windows 行尾规范化提示。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-runtime-control-rollback-target-ui --mode preview` -> PASS，delete `<none>`，blocked `<none>`，warnings `<none>`。
