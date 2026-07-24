# 执行记录：NAS 转移校验所选类别目录绑定

## BDD

BDD: 新建 NAS 转移时所选类别必须已绑定目录 -> Given 管理员选择一个启用但未绑定目录的文件类别 / When 创建 NAS 转移任务 / Then 后端直接拒绝创建任务并提示所选类别未绑定目录。

BDD: 已有等待任务执行前重新校验类别绑定 -> Given NAS 转移等待任务引用的文件类别绑定已被删除或停用 / When 后台调度执行该任务 / Then 任务整体失败并记录明确原因，不继续读取 NAS 文件或逐文件提交。

BDD: 所选类别绑定目录必须覆盖转入 DCC 目录 -> Given 文件类别已绑定目录但转入文件所在 DCC 目录不在该绑定目录子树内 / When 后台处理文件项 / Then 文件项在类别阶段失败，不上传原件或提交受控文件。

## TDD Evidence

- RED: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileNasTransferServiceTest#transfer_rejectsSelectedCategoryWithoutDirectoryBinding+processWaitingTasks_failsTaskWhenSelectedCategoryBindingMissing -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, expected reason: 旧实现只校验所选类别存在且启用；新建任务未因缺目录绑定被拒绝，已有等待任务也没有在执行前任务级失败。
- GREEN: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileNasTransferServiceTest#transfer_rejectsSelectedCategoryWithoutDirectoryBinding+processWaitingTasks_failsTaskWhenSelectedCategoryBindingMissing -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 2 tests.
- REGRESSION: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileNasTransferServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 15 tests.
- REGRESSION: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileNasTransferServiceTest,DccControlledFileWorkflowServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 82 tests.
- BUG EVIDENCE VALIDATION: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence ruoyi-vue-pro\doc\tasks\20260603-dcc-nas-transfer-category-binding-validation\bug-regression-evidence.md` -> PASS.
- CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-dcc-nas-transfer-category-binding-validation --mode preview` -> PASS, keep `task.md` / `execution-log.md` / `bug-regression-evidence.md`, delete `<none>`, blocked `<none>`, warnings `<none>`.

## Root Cause

- NAS 转移在上一任务中已改为使用用户选择的文件类别，不再按 NAS 目录创建 `NAS_TRANSFER` 类别和绑定。
- 受控文件提交链路仍要求文件类别存在启用的目录绑定，并要求提交目录位于该绑定目录子树内。
- NAS 转移入口只校验所选类别存在且启用，没有校验目录绑定；因此缺绑定类别会创建任务并继续处理文件，直到每个文件调用 `submitControlledFileWithoutApproval(...)` 时才逐条失败为 `File category is not bound to a directory`。

## Implementation Notes

- 新增 `SelectedCategoryContext`，集中携带所选类别和绑定目录。
- 新建任务前使用 `categoryDirectoryBindingMapper.selectActiveByCategoryId(...)` fail-fast 校验绑定。
- 执行等待任务时从 `Snapshot.categoryBindingDirectoryId` 重新校验绑定，并确认绑定目录存在且启用。
- 文件项处理先执行目录覆盖校验，再读取 NAS 文件；不覆盖时记录 `failureStage=category`。

## Blockers

- none
