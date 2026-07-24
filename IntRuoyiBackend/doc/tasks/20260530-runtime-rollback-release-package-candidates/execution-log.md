# Execution Log

BDD: 回滚候选读取标准发布包 -> Given NAS `Backup` 根目录包含标准发布包且发布包根目录存在 `release-manifest.json` / When 运行控制台加载回滚候选 / Then 服务端返回该发布包为可选回滚版本，并使用 `packageDirectoryName` 作为 `IMAGE_TAG`。

BDD: 发布包缺 manifest 时按发布包规则阻断 -> Given NAS 发布包目录缺少 `release-manifest.json` / When 加载回滚候选 / Then 候选阻断原因应为缺少 `release-manifest.json`，不得再提示缺少备份点 `manifest.json`。

BDD: 恢复数据仍读取备份点 -> Given 恢复数据需要数据备份证据 / When 加载恢复候选 / Then 服务端仍使用备份点目录和 `manifest/manifest.json`、checksum、演练报告、现场快照。

RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeRollbackCandidateServiceImplTest" test` -> FAIL，新增发布包回滚候选用例先因 `RuntimeControlRollbackCandidateRespVO#getReleaseTag()` 不存在编译失败，证明当前合同还没有表达 NAS 发布包来源。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeRollbackCandidateServiceImplTest" test` -> PASS，12 tests，回滚候选发布包 manifest 用例通过。

REGRESSION: `mvn -pl yudao-module-infra "-Dtest=RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest,RuntimeControlSpringWiringTest" test` -> FAIL，Spring wiring 测试上下文缺少 `NasSettingsService` 测试替身；补齐测试上下文后复跑。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest,RuntimeControlSpringWiringTest,RuntimeControlServiceImplTest" test` -> PASS，43 tests，确认回滚候选读取发布包、恢复候选仍读取备份点、运行控制动作派发和 Spring 装配均通过。

GREEN: `node --check tests\e2e\runtime-control-rollback-app.e2e.js; node --check tests\e2e\runtime-control-release-package-static.spec.js; node tests\e2e\runtime-control-release-package-static.spec.js` -> PASS，前端类型/静态发布包合同检查通过。

GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260530-runtime-rollback-release-package-candidates\bug-regression-evidence.md` -> PASS，缺陷证据满足 bug-regression contract。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260530-runtime-rollback-release-package-candidates --mode preview` -> PASS，keep `task.md`、`execution-log.md`、`bug-regression-evidence.md`，delete/blocked/warnings 均为 `<none>`。
