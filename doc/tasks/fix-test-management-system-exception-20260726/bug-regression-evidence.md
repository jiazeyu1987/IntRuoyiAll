# Bug Regression Evidence

## Bug Summary

访问「测试管理」时提示系统异常。

## Expected Behavior

页面应正常加载测试管理数据；无数据时显示空状态，不应提示系统异常。

## Reproduction

- `node .\tests\e2e\system-codex-test-management-real.e2e.js` 在真实本机前端路径复现失败。
- `/admin-api/system/codex-test-case/page` HTTP 200，但响应体为 `{"code":500,"msg":"系统异常","data":null}`。

## Root Cause

- 本地 Docker MySQL 已应用 `system_codex_test_execution_case` 运行监控进度字段，但未应用 `20260726_system_codex_test_case_project.sql`。
- 当前后端 `CodexTestCaseDO` / `CodexTestCaseMapper` 已读取并过滤 `system_codex_test_case.project`，缺字段时测试项分页接口抛出数据库异常，前端显示系统异常。

## Regression Test

- 使用既有真实只读 E2E `IntRuoyiFronted/tests/e2e/system-codex-test-management-real.e2e.js` 覆盖「系统管理 > 测试管理」页面访问。
- 使用迁移契约 `IntRuoyiBackend/script/tests/test_codex_test_case_project_migration.py` 覆盖 `system_codex_test_case.project` 字段迁移。
- 使用前端静态契约 `IntRuoyiFronted/tests/e2e/system-codex-test-management-static.spec.js` 覆盖测试管理页面、Runner API 和 Runner fail-fast 请求超时契约。

## RED

RED: `node .\tests\e2e\system-codex-test-management-real.e2e.js` -> FAIL, `codex test case page API should return business code 0`, actual `500`。

## GREEN

GREEN: `python -X utf8 -m pytest script\tests\test_codex_test_case_project_migration.py -q` -> PASS, `2 passed in 0.13s`。
GREEN: `node .\tests\e2e\system-codex-test-management-static.spec.js` -> PASS, exit code 0。
GREEN: `node .\tests\e2e\system-codex-test-management-real.e2e.js` -> PASS, `PASS: system codex test management real E2E`。
GREEN: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestCaseServiceImplTest,CodexTestExecutionServiceImplTest,CodexTestRunnerServiceImplTest,CodexTestRunnerControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`。

## Verification

- 测试管理页面真实路径已恢复，测试项分页 API 返回业务成功码，页面不再显示 `系统异常`。

## Risk And Regression Scope

- 测试管理页面、Codex Runner 自动测试接口、测试项列表/领取/结果写入相关链路。

## Blockers And Follow-Up

- 本地问题已修复并验证通过。
- 最终 Git closeout 未完成：当前分支存在非本任务 ahead 提交和非本任务 dirty/untracked 文件，需单独确认后才能安全提交并推送。
