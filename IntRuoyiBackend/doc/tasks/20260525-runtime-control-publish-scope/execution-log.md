# 执行日志：运行控制台发布范围选项后端

BDD: 发布测试服默认只发代码 -> Given 运维人员提交 `publish-test` 且选择 `code-only`, When 后端派发脚本, Then 调用受控测试发布脚本并附加 `-SkipDatabaseSync -SkipMinioSync`，审计记录 `publishScope=code-only`。

BDD: 发布测试服可带数据发布 -> Given 运维人员提交 `publish-test` 且选择 `with-data`, When 后端派发脚本, Then 调用受控测试发布脚本且不附加跳过数据参数，审计记录 `publishScope=with-data`。

BDD: 提升正式服只发代码仍需 PROD -> Given 运维人员提交 `promote-prod` 且选择 `code-only`, When 缺少 `PROD` 确认, Then 请求失败且不执行；When 确认完整, Then 调用正式提升脚本并附加跳过数据参数。

BDD: 非发布动作不接受发布范围 -> Given 运维人员提交备份、回滚或恢复动作, When 请求携带 `publishScope`, Then 后端失败关闭并提示参数不适用。

RED: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> FAIL, expected missing `RuntimeControlActionReqVO.setPublishScope(...)` and missing `RUNTIME_CONTROL_ACTION_PARAMETER_INVALID`.

RED: `python -m pytest script\tests\test_runtime_control_ops_scripts.py script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL, expected `RuntimeControlOperationAction` does not declare publishScope/code-only/with-data and `promote-int-ruoyi-test-to-prod.ps1` lacks `SkipDatabaseSync` / `SkipMinioSync`.

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> PASS, 13 tests passed.

GREEN: `python -m pytest script\tests\test_runtime_control_ops_scripts.py script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 25 tests passed.

GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260525-runtime-control-publish-scope\backend-api-evidence.md` -> PASS.

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-runtime-control-publish-scope --mode preview` -> BLOCKED for apply gates, preview completed and listed only `backend-api-evidence.md` as task artifact delete candidate; backend main worktree dirty and linked worktree cannot fast-forward merge into `int_main`.
