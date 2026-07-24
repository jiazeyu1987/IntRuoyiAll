# 任务：回滚版本候选读取 NAS 标准发布包

## 任务目标

修复运行控制台“回滚版本”候选来源错误的问题。回滚版本应读取 NAS 标准发布包目录，校验发布包根目录下的 `release-manifest.json` 并解析镜像标签；不得把发布包目录按备份点结构误读为 `manifest/manifest.json`。恢复数据候选仍然只读取备份点目录。

## 前置任务检查

- `20260530-runtime-control-candidate-directory-filter` 状态为 `Completed`。
- 当前仓库存在与本任务无关的 DCC/NAS 迁移和发布脚本未提交改动；本任务不修改这些文件。
- `20260530-dcc-dmr-nas-transfer-completion` 仍处于真实迁移复测中，属于独立任务；本任务不更改其任务文档或运行状态。

## BDD 场景

- BDD: 回滚候选读取标准发布包 -> Given NAS `Backup` 根目录包含标准发布包且发布包根目录存在 `release-manifest.json` / When 运行控制台加载回滚候选 / Then 服务端返回该发布包为可选回滚版本，并使用 `packageDirectoryName` 作为 `IMAGE_TAG`。
- BDD: 发布包缺 manifest 时按发布包规则阻断 -> Given NAS 发布包目录缺少 `release-manifest.json` / When 加载回滚候选 / Then 候选阻断原因应为缺少 `release-manifest.json`，不得再提示缺少备份点 `manifest.json`。
- BDD: 恢复数据仍读取备份点 -> Given 恢复数据需要数据备份证据 / When 加载恢复候选 / Then 服务端仍使用备份点目录和 `manifest/manifest.json`、checksum、演练报告、现场快照。

## 里程碑

- [x] M1：建立任务记录并确认已知前置状态。
- [x] M2：补充回滚候选发布包 RED 回归测试。
- [x] M3：实现回滚候选读取发布包 manifest。
- [x] M4：运行目标回归验证并记录 GREEN。
- [x] M5：运行 task-closeout-cleanup 预览并完成任务文档。

## 预期验证

- `mvn -pl yudao-module-infra "-Dtest=RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest,RuntimeControlSpringWiringTest" test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260530-runtime-rollback-release-package-candidates\bug-regression-evidence.md`

## Cleanup Keep

- `doc/tasks/20260530-runtime-rollback-release-package-candidates/bug-regression-evidence.md`

## Current Status

Completed. 回滚候选发布包来源修复已完成，目标回归、证据校验和 cleanup 预览均通过；正式服生效需后续按发布门禁部署。

## 已完成工作

- 新增 `RuntimeReleasePackageNasRepository`，专门读取 NAS 标准发布包目录。
- `rollback-app` 候选改为读取 `release-manifest.json`，使用 `packageDirectoryName` 作为 `IMAGE_TAG`。
- `restore-data` 候选继续读取备份点，不与发布包混用。
- 前端候选行在回滚模式显示“发布包”，恢复模式显示“备份”。

## 当前验证

- `mvn -pl yudao-module-infra "-Dtest=RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest,RuntimeControlSpringWiringTest,RuntimeControlServiceImplTest" test` -> PASS，43 tests。
- `node --check tests\e2e\runtime-control-rollback-app.e2e.js; node --check tests\e2e\runtime-control-release-package-static.spec.js; node tests\e2e\runtime-control-release-package-static.spec.js` -> PASS。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260530-runtime-rollback-release-package-candidates\bug-regression-evidence.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260530-runtime-rollback-release-package-candidates --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
