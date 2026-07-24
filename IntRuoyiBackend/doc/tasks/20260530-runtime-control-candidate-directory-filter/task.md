# 任务：运行控制台过滤非备份点候选目录

## 任务目标

修复运行控制台“回滚版本/恢复数据”候选列表把 NAS `Backup` 根目录下的普通目录误当作备份点的问题。候选服务必须与 backup-ops 脚本保持一致，只暴露符合真实备份点命名规则 `yyyyMMdd-HHmmss` 的目录；非备份点目录不得进入候选清单并造成“缺少 manifest.json；缺少镜像标签”的误导阻断。

## 前置任务检查

- `20260530-runtime-control-promote-release-selector` 状态为 `Completed`。
- 后端仓库存在与本任务无关的未提交改动：`script/deploy/publish-int-ruoyi.ps1`、`script/tests/test_publish_int_ruoyi_to_test_tooling.py`、`doc/tasks/20260529-showroom-release-truth-refactor/`、`yudao-module-showroom/output/imagegen/three-way-stopcock-1-list-card-native.png`。本任务不修改这些文件。

## BDD 场景

- BDD: 回滚候选过滤普通目录 -> Given NAS `Backup` 根目录同时包含 `reference`、`26-05-30_00-11-31` 和真实备份点目录 / When 运行控制台加载回滚候选 / Then 服务端只返回真实备份点目录，普通目录不显示为已阻断候选。
- BDD: 恢复候选过滤普通目录 -> Given NAS `Backup` 根目录同时包含普通目录和真实备份点目录 / When 运行控制台加载恢复候选 / Then 服务端只返回真实备份点目录，避免用非备份点制造 manifest/checksum 阻断信息。

## 里程碑

- [x] M1：确认前置任务状态与当前工作区未提交改动。
- [x] M2：补充候选目录过滤回归测试并记录 RED。
- [x] M3：实现服务端备份点目录过滤。
- [x] M4：运行目标回归验证并记录 GREEN。
- [x] M5：运行 task-closeout-cleanup 预览并完成任务文档。

## 预期验证

- `mvn -pl yudao-module-infra "-Dtest=RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest" test`

## Current Status

Completed. 回滚/恢复候选目录过滤已修复，回归验证与 cleanup 预览均通过。

## Cleanup Keep

- `doc/tasks/20260530-runtime-control-candidate-directory-filter/bug-regression-evidence.md`

## 已完成工作

- 在 `RuntimeRollbackCandidateServiceImplTest` 和 `RuntimeRestoreCandidateServiceImplTest` 中新增普通目录过滤回归用例。
- 在 `RuntimeBackupNasRepository` 中统一过滤备份点目录，只保留符合 `yyyyMMdd-HHmmss` 的真实备份点目录。
- 确认回滚候选、恢复候选和备份点列表服务目标回归通过。

## 最终验证

- `mvn -pl yudao-module-infra "-Dtest=RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest" test` -> PASS，18 tests。
- `mvn -pl yudao-module-infra "-Dtest=RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest,RuntimeBackupDrillServiceImplTest" test` -> PASS，22 tests。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260530-runtime-control-candidate-directory-filter\bug-regression-evidence.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260530-runtime-control-candidate-directory-filter --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
