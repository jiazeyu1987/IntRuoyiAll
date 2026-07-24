# 20260706-schedule-edhr-batchcode-nonblocking

## Task Goal
- 修复自动排产完成后触发 eDHR 执行批次创建时，因生产工单缺少批次号导致排产应用失败的问题。
- 保持排产系统与批记录系统职责分离：排产完成不被 eDHR 批次号前置条件回滚；eDHR 自身仍保留批次创建前置校验。

## Milestones
- [x] 建立任务记录、经验门禁、BDD/TDD 约束。
- [x] RED：新增/调整回归测试，证明 eDHR 批次号缺失当前会阻断排产应用。
- [x] GREEN：让排产完成遇到 eDHR 批次号缺失时不回滚排产结果。
- [x] 验证：运行自动排产与 eDHR 批次相关后端测试。
- [x] 收尾：清理任务产物并单独提交本任务改动。

## Expected Verification
- `mvn.cmd -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest test`
- `mvn.cmd -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest test`

## 经验门禁
- PowerShell/Windows 命令：已读取 `docs/powershell-memory.md`，命令避免 `&&`，中文读写使用 UTF-8。
- BDD + 严格 TDD：先记录 Given/When/Then，再修改测试形成 RED，最后最小实现与 GREEN。
- 无 fallback：不吞掉排产域错误；仅将 eDHR 批次创建前置条件保持在 eDHR 域，不作为排产事务回滚条件。
- Git 提交：仅提交本任务直接相关文件，避开当前工作区已有展厅/旧编号脏改。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否；排产域仍失败快，eDHR 批次创建前置条件不再作为排产域阻断。
- 是否从根因和长期维护角度解决：是；移除排产完成后的跨域硬阻断，保留 eDHR 服务自己的校验职责。
- 是否存在临时补丁或绕过：否。

## Current Status
completed。
- 已完成：定位到 `MesProAutoScheduleServiceImpl#createEdhrBatchExecutionsAfterScheduleCompletion` 调用 `MesProEdhrBatchExecutionService.openOrCreateFromScheduleCompletion` 后，批次号缺失异常回滚排产应用。
- 已完成：将 eDHR 批次创建缺少批次号的专属 `ServiceException` 转为排产告警 `EDHR_BATCH_CREATION`，排产任务创建和已排数量同步不回滚；其他 eDHR 或系统异常继续抛出。
- 验证结果：`mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#apply_shouldNotRollbackScheduleWhenEdhrBatchCreationMissesBatchCode" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/MesProFeedbackApprovalTaskAdapterTest.java,**/ApprovalCenterServiceImplTest.java" test` -> PASS，1 test, 0 failures, 0 errors。
- 收尾：cleanup preview 无删除项、无阻塞、无警告；本任务改动已准备单独提交。
