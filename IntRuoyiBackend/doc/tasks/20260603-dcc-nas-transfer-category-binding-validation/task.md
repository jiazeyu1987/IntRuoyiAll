# 任务：NAS 转移校验所选类别目录绑定

## 任务目标

修复 DCC NAS 转移在所选文件类别未绑定目录时继续处理文件、最终把大量文件逐条失败为 `File category is not bound to a directory` 的问题。系统应在创建或执行转移任务前校验所选类别的有效目录绑定，缺失时直接阻断，不读取 NAS 文件、不创建 DCC 文件、不用默认类别或自动绑定兜底。

## Previous Task Check

- 上一个相关后端任务：`doc/tasks/20260603-dcc-delete-nas-transfer-categories/task.md`
- 状态：`completed`
- 影响：上一任务已清理历史 `NAS_TRANSFER` 类别，本任务只修正后续 NAS 转移前置校验和失败粒度，不回滚历史清理。

## BDD 场景

- BDD: 新建 NAS 转移时所选类别必须已绑定目录 -> Given 管理员选择一个启用但未绑定目录的文件类别 / When 创建 NAS 转移任务 / Then 后端直接拒绝创建任务并提示所选类别未绑定目录。
- BDD: 已有等待任务执行前重新校验类别绑定 -> Given NAS 转移等待任务引用的文件类别绑定已被删除或停用 / When 后台调度执行该任务 / Then 任务整体失败并记录明确原因，不继续读取 NAS 文件或逐文件提交。
- BDD: 所选类别绑定目录必须覆盖转入 DCC 目录 -> Given 文件类别已绑定目录但转入文件所在 DCC 目录不在该绑定目录子树内 / When 后台处理文件项 / Then 文件项在类别阶段失败，不上传原件或提交受控文件。

## Milestones

- [x] M1：建立任务文档，确认上一相关任务已完成。
- [x] M2：新增失败回归测试，复现类别无绑定时任务未提前阻断。
- [x] M3：实现所选类别目录绑定前置校验与目录覆盖校验。
- [x] M4：运行目标 Maven 测试并记录 RED/GREEN/REGRESSION。
- [x] M5：执行 closeout 预览，验证通过后提交本任务改动。

## Expected Verification

- RED：新增目标用例在旧实现下失败，证明旧逻辑不会提前阻断未绑定类别。
- GREEN：同一目标用例通过，未绑定类别被任务级阻断。
- REGRESSION：`DccControlledFileNasTransferServiceTest` 全量通过，既有“使用所选类别、不创建目录名类别”的行为不回归。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少目录绑定或绑定目录不覆盖目标目录时直接失败。
- `是否从根因和长期维护角度解决`：是。将 DCC 类别治理前置条件纳入 NAS 转移任务入口与执行入口，而不是在提交阶段批量失败后再隐藏错误。
- `是否存在临时补丁或绕过`：否。不自动创建绑定、不默认归入其他类别、不跳过受控文件提交校验。

## 当前状态

completed

## 已完成工作

- `transfer()` 创建入口现在要求所选类别存在启用的目录绑定，缺失时直接拒绝创建任务。
- `executeTask()` 调度入口基于任务快照重新校验所选类别绑定，绑定缺失或绑定目录不存在/停用时将任务整体标为 `FAILED`。
- 文件项处理前先确认转入 DCC 目录位于所选类别绑定目录子树内；不覆盖时文件项在 `category` 阶段失败，不读取 NAS 文件、不上传原件、不提交受控文件。
- 回归测试覆盖缺绑定创建阻断、等待任务缺绑定任务级失败、绑定不覆盖目标目录时文件级 fail-fast，以及既有所选类别导入行为。

## 验证结果

- RED：`mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileNasTransferServiceTest#transfer_rejectsSelectedCategoryWithoutDirectoryBinding+processWaitingTasks_failsTaskWhenSelectedCategoryBindingMissing -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，旧实现未提前阻断缺目录绑定类别。
- GREEN：同一命令 -> PASS，2 tests。
- REGRESSION：`mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileNasTransferServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，15 tests。
- REGRESSION：`mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileNasTransferServiceTest,DccControlledFileWorkflowServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，82 tests。
- BUG EVIDENCE VALIDATION：`python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence ruoyi-vue-pro\doc\tasks\20260603-dcc-nas-transfer-category-binding-validation\bug-regression-evidence.md` -> PASS。
- CLOSEOUT PREVIEW：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-dcc-nas-transfer-category-binding-validation --mode preview` -> PASS，delete `<none>`，blocked `<none>`，warnings `<none>`。

## Cleanup Keep

- `doc/tasks/20260603-dcc-nas-transfer-category-binding-validation/bug-regression-evidence.md`
