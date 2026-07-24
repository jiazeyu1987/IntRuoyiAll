# Execution Log - 20260706-schedule-edhr-batchcode-nonblocking

## BDD
- BDD: eDHR 批次号缺失不回滚排产完成 -> Given 排产路线启用了 eDHR 批记录配置且生产工单未维护批次号 / When 自动排产应用完成并尝试创建 eDHR 执行批次 / Then 排产应用仍成功创建任务并同步已排数量，eDHR 批次创建不反向阻断排产。
- BDD: eDHR 批次服务自身仍校验前置条件 -> Given 直接调用 eDHR 排产完成批次创建命令且缺少批次号 / When eDHR 服务创建执行批次 / Then eDHR 服务仍返回缺少前置条件：批次号。
- BDD: 有批次号且 eDHR 配置完整时仍触发批次创建 -> Given 排产路线启用了 eDHR 批记录配置且生产工单已有批次号 / When 自动排产应用完成 / Then 继续调用 eDHR 执行批次创建命令。

## TDD Evidence
- RED: `mvn.cmd -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest#apply_shouldNotRollbackScheduleWhenEdhrBatchCreationMissesBatchCode test` -> FAIL，原实现会把 `排产完成创建 eDHR 批次缺少前置条件：批次号` 从 eDHR 批次创建链路抛出并回滚排产应用；新增断言期望排产成功、任务创建、已排数量同步，并写入 `EDHR_BATCH_CREATION` 告警。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#apply_shouldNotRollbackScheduleWhenEdhrBatchCreationMissesBatchCode" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/MesProFeedbackApprovalTaskAdapterTest.java,**/ApprovalCenterServiceImplTest.java" test` -> PASS，1 test, 0 failures, 0 errors。

## Commands
- `rg -n "openOrCreateFromScheduleCompletion|EdhrScheduleCompletionCreateCommand|PRO_EDHR_BATCH_EXECUTION_SCHEDULE_PREREQUISITE_MISSING" ...` -> 定位排产完成触发 eDHR 批次创建链路。
- `apply_patch` -> 将自动排产应用阶段的 eDHR 批次创建缺少前置条件异常收敛为排产告警，并更新回归测试。
- `python -X utf8 -c ...` -> PASS，确认修改文件中中文文本 UTF-8 读取正常，未出现常见乱码标记。
- `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#apply_shouldNotRollbackScheduleWhenEdhrBatchCreationMissesBatchCode" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/MesProFeedbackApprovalTaskAdapterTest.java,**/ApprovalCenterServiceImplTest.java" test` -> GREEN PASS。

## Root Cause
- 自动排产 `apply` 在任务创建、已排数量同步后同步调用 eDHR 执行批次创建。
- 当生产工单未维护批次号时，eDHR 服务按自身契约抛出 `PRO_EDHR_BATCH_EXECUTION_SCHEDULE_PREREQUISITE_MISSING`，该异常原先向外传播，导致排产事务整体失败。

## Fix
- 保留 eDHR 服务对批次号的强校验。
- 在排产完成触发 eDHR 批次创建时，仅将 `PRO_EDHR_BATCH_EXECUTION_SCHEDULE_PREREQUISITE_MISSING` 转换为 `EDHR_BATCH_CREATION` 警告并持久化到排产问题表。
- 非该错误码的 `ServiceException` 继续抛出，避免吞掉真实系统错误。
