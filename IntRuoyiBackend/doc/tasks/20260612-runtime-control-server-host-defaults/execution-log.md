# 执行日志：修复运行控制台 test.serverHost 缺失

BDD: 运行控制台默认属性包含测试服固定 host -> Given 后端以默认 `RuntimeControlProperties` 初始化运行控制台远程环境 / When 查询或清理远程根分区测试服 / Then 后端应使用固定测试服 `172.30.30.58` 构造命令，不应因 `test.serverHost` 缺失而阻断。

BDD: 正式服写动作边界保持不变 -> Given 默认运行控制台属性包含正式服固定 host / When 未授权用户执行正式服写动作 / Then 正式服写动作仍保持禁用或需要显式确认，不因补齐 host 放松授权边界。

## M1 审计

- 现象：用户报告 `运行控制台动作缺少必填参数：test.serverHost`。
- 代码定位：`RuntimeRemoteRootDiskServiceImpl.requireKnownEnvironment()` 在 `properties.getEnvironments().get(targetEnvironment).getHost()` 为空时抛出 `RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED`。
- 初步根因：`RuntimeControlProperties.defaultEnvironments()` 中 `test/prod/backup` 的默认 host 为空；`application-local.yaml` 会补齐 host，但非 local profile 或仅使用默认属性时会缺失。
- 前置状态：后端最近任务 `20260612-report-recognition-select-word-file` 已标记 `BLOCKED_ON_EXTERNAL_BACKEND_COMPILE`，本任务不处理该外部编译阻塞。

## M2 RED

- RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeRemoteRootDiskServiceImplTest#getStatusShouldUseDefaultFixedTestHostWithoutProfileSpecificOverride" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`ServiceException: 运行控制台动作缺少必填参数：test.serverHost`。

## M3 GREEN

- 变更：在 `RuntimeControlProperties` 定义 `TEST_SERVER_HOST/PROD_SERVER_HOST/BACKUP_SERVER_HOST`，默认远程环境直接带固定 host；远程根分区服务的目标证明复用同一常量表。
- 变更：`RuntimeControlServiceImplTest` 补充默认属性固定 host 断言；`RuntimeRemoteRootDiskServiceImplTest` 增加默认属性下测试服状态查询回归测试。
- 变更：`RuntimeControlServiceImplTest` 的恢复候选夹具补充 `manifest/dcc-backup-manifest.json`，满足当前候选门禁。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeRemoteRootDiskServiceImplTest#getStatusShouldUseDefaultFixedTestHostWithoutProfileSpecificOverride" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，1 test passed。

## M4 REGRESSION

- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeRemoteRootDiskServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，71 tests passed。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeRemoteRootDiskServiceImplTest,RuntimeRestoreCandidateServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，85 tests passed。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence ruoyi-vue-pro\doc\tasks\20260612-runtime-control-server-host-defaults\bug-regression-evidence.md` -> PASS，Bug regression evidence is valid。

## M5 收尾

- GREEN: `git -C ruoyi-vue-pro diff --check -- <本次后端文件>` -> PASS，仅 Git CRLF 提示。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260612-runtime-control-server-host-defaults --mode preview` -> PASS，保留 `task.md`、`execution-log.md`、`bug-regression-evidence.md`，无删除项、无阻塞、无警告。
