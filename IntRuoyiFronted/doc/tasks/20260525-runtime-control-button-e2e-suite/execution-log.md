# 执行日志：运行控制台五个运维按钮 E2E 用例

BDD: 发布测试服按钮安全打开 -> Given admin 拥有运维权限, When 点击 `发布测试服`, Then 弹窗显示发布范围且默认 `只发代码`，缺少原因时不提交动作请求。

BDD: 提升正式服按钮安全拦截 -> Given admin 拥有运维权限, When 点击 `提升正式服` 并填写原因但不输入 `PROD`, Then 不提交动作请求。

BDD: 立即备份按钮安全拦截 -> Given admin 拥有运维权限, When 点击 `立即备份` 并填写原因但不输入 `PROD`, Then 不提交动作请求。

BDD: 回滚版本按钮安全拦截 -> Given admin 拥有运维权限, When 点击 `回滚版本` 并填写原因和 `PROD` 但不填镜像标签, Then 不提交动作请求。

BDD: 恢复数据按钮安全拦截 -> Given admin 拥有运维权限, When 点击 `恢复数据` 并填写原因和 `PROD` 但不填备份点, Then 不提交动作请求。

RED: before adding files -> FAIL, no dedicated Playwright E2E files exist for the five runtime-control operation buttons.

REVIEW: subagent publish-test E2E -> PASS, file `tests/e2e/runtime-control-publish-test.e2e.js` verifies publish scope default/risk hint and no action POST when reason is missing.

REVIEW: subagent promote-prod E2E -> PASS, file `tests/e2e/runtime-control-promote-prod.e2e.js` verifies PROD guard and no action POST.

REVIEW: subagent backup-now E2E -> PASS, file `tests/e2e/runtime-control-backup-now.e2e.js` verifies no publish scope, PROD guard, and no action POST.

REVIEW: subagent rollback-app E2E -> PASS, file `tests/e2e/runtime-control-rollback-app.e2e.js` verifies required image tag guard and no action POST.

REVIEW: subagent restore-data E2E -> PASS, file `tests/e2e/runtime-control-restore-data.e2e.js` verifies required backup point guard and no action POST.

FIX: shared helper login -> updated `tests/e2e/runtime-control-ops-e2e-helper.js` so tenant input is filled only when visible; current test server defaults to `芋道源码` with the tenant input hidden.

GREEN: `node --check` for `runtime-control-ops-e2e-helper.js` and all five runtime-control operation E2E files -> PASS.

GREEN: real Playwright E2E with `NODE_PATH=C:\Users\BJB110\AppData\Local\npm-cache\_npx\e41f203b7505f1fb\node_modules` -> PASS, all five files passed against `http://172.30.30.58:8081`.

GREEN: `node tests\e2e\runtime-control-ops-static.spec.js` -> PASS.

GREEN: `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence doc\tasks\20260525-runtime-control-button-e2e-suite\qa-evidence.md` -> PASS.

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-runtime-control-button-e2e-suite --mode preview` -> PASS, cleanup preview kept `task.md` and `execution-log.md`, and marked temporary QA evidence for deletion.

GREEN: cleanup follow-up preview -> PASS, no temporary files remain under the task directory.

REGRESSION: `node --check` for helper and all five E2E files, `node tests\e2e\runtime-control-ops-static.spec.js`, and all five real Playwright E2E files -> PASS.
