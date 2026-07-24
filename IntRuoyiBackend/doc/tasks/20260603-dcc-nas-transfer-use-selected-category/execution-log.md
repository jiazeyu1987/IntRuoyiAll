# 执行记录：NAS 转移使用所选文件类别

## BDD

BDD: NAS 转移使用所选文件类别 -> Given 管理员选择文件类别“其他”并转移 NAS 目录 / When 系统导入该目录下任意层级文件 / Then 所有转入文件的 `category_id` 均为“其他”，不得按 NAS 目录名创建新的文件类别。

BDD: NAS 目录仍同步为 DCC 目录 -> Given NAS 转移包含多层目录 / When 系统导入目录结构 / Then DCC 目录树仍按 NAS 路径创建或复用，且目录访问规则继承逻辑保持不变。

BDD: 所选类别缺失或停用必须失败关闭 -> Given 转移任务引用的文件类别不存在或已停用 / When 执行 NAS 转移 / Then 系统必须明确失败，不得创建目录名类别或默认归入其他类别。

## TDD Evidence

- RED: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileNasTransferServiceTest#processWaitingTasks_usesSelectedCategoryInsteadOfCreatingDirectoryCategory -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, expected reason: 新增回归预期要求选择的文件类别被用作最终 `categoryId`，旧实现仍保留按目录解析/生成类别语义，测试未通过。
- GREEN: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileNasTransferServiceTest#processWaitingTasks_usesSelectedCategoryInsteadOfCreatingDirectoryCategory -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 1 test.
- REGRESSION: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileNasTransferServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 12 tests.

## Root Cause

- NAS 转移任务表字段名为 `template_category_id`，旧实现把用户选择的类别当作模板类别使用：导入文件时根据父 DCC 目录调用目录类别解析逻辑，必要时用目录名创建 `source=NAS_TRANSFER` 的 `dcc_file_category`，再复制模板类别治理配置。
- 界面语义是“选择文件类别”，用户选择“其他”期望所有转入文件归入“其他”；后端模板语义与业务语义不一致，导致 NAS 存放目录被污染为文件类别。

## Implementation Notes

- `transfer()` 与 `executeTask()` 继续 fail-fast 校验所选类别存在且启用。
- `processFileItem()` 现在调用 `assignSelectedCategoryForFileItem()`，将父目录任务项的 `resolvedCategoryId` 强制设为所选类别。
- 旧 `NASCAT-*` 类别生成路径已移除；NAS 目录同步和目录访问规则继承保留。

## Blockers

- none

## Closeout

- BUG EVIDENCE VALIDATION: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence ruoyi-vue-pro\doc\tasks\20260603-dcc-nas-transfer-use-selected-category\bug-regression-evidence.md` -> PASS.
- task-closeout-cleanup preview: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-dcc-nas-transfer-use-selected-category --mode preview` -> PASS, keep `task.md` / `execution-log.md` / `bug-regression-evidence.md`, delete `<none>`, blocked `<none>`, warnings `<none>`.
