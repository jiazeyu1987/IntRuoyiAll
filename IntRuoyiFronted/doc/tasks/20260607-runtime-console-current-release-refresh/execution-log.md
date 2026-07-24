# 执行日志：运行控制台标记测试通过刷新当前发布包

BDD: 标记测试通过读取最新测试服发布包 -> Given 测试服已部署发布包 A / When 操作员打开“标记测试通过”弹窗 / Then 页面先刷新运行概览并展示当前测试服 `releaseTag`，允许在恢复集候选可用时提交。

BDD: 禁止伪造当前测试服发布包 -> Given 概览刷新失败或测试服未返回 `currentReleaseTag` / When 操作员打开或提交“标记测试通过” / Then 页面阻止提交并暴露缺失前置条件。

BDD: 恢复数据到测试服不要求 PROD -> Given 操作员打开“恢复数据”弹窗且目标为测试服 / When 选择测试服恢复集候选 / Then 页面不显示 `PROD` 确认；切换备份服务器目标时才显示 `PROD` 确认。

## Evidence

- RED: `node tests\e2e\runtime-control-release-package-static.spec.js` -> FAIL，`opening mark-release-tested must refresh overview before reading the current test release tag`。现有 `openOperation` 打开“标记测试通过”时只刷新候选，不刷新概览，页面可能读取 stale overview 并显示当前测试服发布包为“无”。
- GREEN: `node tests\e2e\runtime-control-release-package-static.spec.js` -> PASS。`openOperation` 对 `operationUsesCurrentTestReleaseTag(action.action)` 的动作先刷新概览，再读取当前测试服 `releaseTag`。
- GREEN: `node --check doc\tasks\20260607-runtime-console-current-release-refresh\scripts\runtime-console-full-goal.e2e.js` -> PASS，真实 UI 全链路脚本语法检查通过。
- RED: `node doc\tasks\20260607-runtime-console-current-release-refresh\scripts\runtime-console-full-goal.e2e.js` -> FAIL，登录后等待跳转超时；未提交任何运行控制台动作。后续定位为本机后端 `127.0.0.1:48081` 停止监听，已转后端重启脚本根因修复。
- RED: `RUNTIME_CONTROL_E2E_BASE_URL=http://localhost:8081 node tests\e2e\runtime-control-restore-data.e2e.js` -> FAIL，`测试服恢复 PROD 确认 should not be visible`。根因：`operationRequiresProd('restore-data')` 对恢复数据动作不区分目标环境，测试服恢复被错误要求 `PROD`。
- GREEN: `RUNTIME_CONTROL_E2E_BASE_URL=http://localhost:8081 node tests\e2e\runtime-control-restore-data.e2e.js` -> PASS。测试服恢复不显示 `PROD`，切换备份服务器恢复目标后显示 `PROD`。
- GREEN: `node tests\e2e\runtime-control-ops-static.spec.js` -> PASS，`backup-now` / `rollback-app` / `restore-data` 的 `PROD` 门禁按目标环境判断，`promote-prod` / `promote-backup` 仍强制 `PROD`。
- GREEN: `node tests\e2e\runtime-control-restore-target-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\runtime-control-release-package-static.spec.js` -> PASS。
- GREEN: `node --check doc\tasks\20260607-runtime-console-current-release-refresh\scripts\runtime-console-full-goal.e2e.js` -> PASS。
- GREEN: `node --check doc\tasks\20260607-runtime-console-current-release-refresh\scripts\diagnose-mark-release-tested.e2e.js` -> PASS。

## Final UI Run

- GREEN: `node doc\tasks\20260607-runtime-console-current-release-refresh\scripts\runtime-console-full-goal.e2e.js` with `RUNTIME_CONTROL_FULL_GOAL_ALLOW=1` -> PASS，完整从步骤 1 重新执行，ReleaseTag A 为 `20260607_ui_code_only_onlyoffice_A_043314`。
- GREEN: `构建发布包` -> PASS，operationId `f45a3095-a28c-423c-94cf-e2257e2120f5`，`publishScope=code-only`，`includeOnlyOffice=true`，NAS 路径 `Backup/ReleasePackage/20260607_ui_code_only_onlyoffice_A_043314`。
- GREEN: `部署发布包到测试服` -> PASS，operationId `c035f04c-f8db-4555-b9fc-c6c0b3307056`，发布包 A 部署到 `172.30.30.58`。
- GREEN: `标记测试通过` -> PASS，operationId `404bea05-1954-40c1-b7a1-0fdadd8e9e30`，验证结论为 `验证结论：20260607_ui_code_only_onlyoffice_A_043314 已通过测试服部署与健康检查，可上线备份服务器`。
- GREEN: `上线备份服务器` -> PASS，operationId `41455001-0c1e-44e0-8ef1-9dc4d7ab6cf6`，发布包 A 部署到备份服务器 `172.30.30.59`。
- GREEN: `立即备份` 选择测试服 -> PASS，operationId `599de693-0aec-414d-9362-cc3b37a1971f`，生成备份点 `20260607-050200`。
- GREEN: `恢复数据` 选择测试服 -> PASS，operationId `21336b5c-a5b2-4bd0-8e65-f61536e37f41`，恢复点 `20260607-050200`，目标环境 `test`。
- GREEN: independent operation status sweep -> PASS，以上 6 个 operation JSON 均为 `status=succeeded`。
- GREEN: independent HTTP health sweep -> PASS，`172.30.30.58` 与 `172.30.30.59` 的 `48081/actuator/health`、`8081/`、`8080/healthcheck`、`8083/` 均返回 HTTP 200。
- GREEN: production boundary assertion -> PASS，UI 驱动记录 `No promote-prod action and no targetEnvironment=prod were submitted by this run.`；本次任务未对正式服务器 `172.30.30.57` 提交发布、重启、写入或恢复动作。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260607-runtime-console-current-release-refresh --mode preview` -> PASS，`status: ready`，`blocked: <none>`；仅执行预览，未删除本地验证证据。
