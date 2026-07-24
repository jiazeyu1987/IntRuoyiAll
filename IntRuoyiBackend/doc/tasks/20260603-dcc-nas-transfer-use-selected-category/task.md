# 任务：NAS 转移使用所选文件类别

## 任务目标

修复 DCC NAS 转移时“选择文件类别=其他”却按 NAS 目录自动生成文件类别的问题。转移任务应把所有转入文件归入用户选择的目标文件类别，不再因目录名自动创建 `NAS_TRANSFER` 文件类别。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260603-dcc-download-openable-pdf/task.md`
- 状态：`completed`
- 影响：上一任务处理 DCC 下载产物契约，本任务只修改 NAS 转移分类归属逻辑，不回滚上一任务。

## BDD 场景

- BDD: NAS 转移使用所选文件类别 -> Given 管理员选择文件类别“其他”并转移 NAS 目录 / When 系统导入该目录下任意层级文件 / Then 所有转入文件的 `category_id` 均为“其他”，不得按 NAS 目录名创建新的文件类别。
- BDD: NAS 目录仍同步为 DCC 目录 -> Given NAS 转移包含多层目录 / When 系统导入目录结构 / Then DCC 目录树仍按 NAS 路径创建或复用，且目录访问规则继承逻辑保持不变。
- BDD: 所选类别缺失或停用必须失败关闭 -> Given 转移任务引用的文件类别不存在或已停用 / When 执行 NAS 转移 / Then 系统必须明确失败，不得创建目录名类别或默认归入其他类别。

## Milestones

- [x] M1：建立任务文档，确认上一后端任务已完成。
- [x] M2：补充失败回归测试，复现选择“其他”仍生成目录名类别。
- [x] M3：修改 NAS 转移分类解析逻辑，导入文件统一使用任务选择类别。
- [x] M4：运行目标 Maven 回归并记录 RED/GREEN 证据。
- [x] M5：执行 closeout 预览并提交本任务改动。

## Expected Verification

- RED：`DccControlledFileNasTransferServiceTest` 新增用例先失败，旧实现会创建目录名类别并把文件归入新类别。
- GREEN：同一目标测试通过，导入文件使用所选类别，目录名类别不再新增。
- REGRESSION：`DccControlledFileNasTransferServiceTest` 通过，既有目录同步与导入测试不回归。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。所选类别缺失或停用继续失败关闭，不新增默认类别 fallback。
- `是否从根因和长期维护角度解决`：是。修正 NAS 转移分类归属语义，而不是隐藏前端显示或事后清理数据。
- `是否存在临时补丁或绕过`：否。不新增临时字段、不绕过权限、不用 mock 成功替代真实服务测试。

## 当前状态

completed

## 已完成工作

- 新增回归测试 `processWaitingTasks_usesSelectedCategoryInsteadOfCreatingDirectoryCategory`，覆盖选择“其他”后导入文件仍使用所选类别且不插入目录名文件类别。
- 修改 `DccControlledFileNasTransferServiceImpl`：NAS 转移创建任务与执行任务时均校验所选类别；文件导入前把父目录任务项的 `resolvedCategoryId` 设置为所选类别，不再调用目录名类别创建逻辑。
- 移除旧的 NASCAT 目录名类别生成私有路径及对应旧单测，避免后续误用。
- 同步既有 NAS 转移测试预期：目录仍创建/复用，文件类别不再创建或绑定，提交文件使用用户选择的类别。

## 验证结果

- RED：`mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileNasTransferServiceTest#processWaitingTasks_usesSelectedCategoryInsteadOfCreatingDirectoryCategory -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，新增选择类别回归预期在旧实现下不满足。
- GREEN：同一命令 -> PASS，目标用例通过。
- REGRESSION：`mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileNasTransferServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，12 tests。
- E2E：未执行真实 NAS 转移 E2E；该路径会创建 DCC 目录、文件和任务数据，本次通过服务级回归测试精确覆盖分类归属行为。
- CLOSEOUT PREVIEW：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-dcc-nas-transfer-use-selected-category --mode preview` -> PASS。

## Cleanup Keep

- `doc/tasks/20260603-dcc-nas-transfer-use-selected-category/bug-regression-evidence.md`
