# Execution Log

## User Intent

- 用户要求：不管是 FIFO 自动分配还是手动分配给订单，只要分配满了，对应订单生产进度就要更新。
- 用户补充：分配数量允许为 0 或空，空就是 0。

## BDD Scenarios

- BDD: 满额分配更新生产进度 -> Given 活跃订单当前工序目标数量为 10 且尚未满额 When FIFO 自动分配或手动确认累计分配达到 10 Then 活跃订单生产进度显示为 100%。
- BDD: 空或 0 分配数量合法 -> Given 分配弹窗中某一行分配数量为空或 0 When 用户确认分配 Then 系统按 0 处理该行，不提示“必须为正整数”，且仍校验总分配量与剩余量。

## Evidence

- Preflight: 已读取 `bug-regression-fix-loop` 技能和 `references/bug-contract.md`。
- Preflight: 已读取任务、前端、后端、数据库、E2E、PowerShell 编码与技术栈规则；相关经验门禁记录在 task.md。
- Root cause: `MesReportAllocationCommandService.aggregateDesired` 把 null/0 分配数量当成非法值；`save` 在当前分配与请求一致时直接返回，未触发 `completionService.reconcileAffectedAllocations` 补同步生产进度。
- Change: null/0 分配行按 0 跳过，负数仍失败；当前分配与请求一致且已有分配行时仍调用订单工序完成进度重算，不新建分配版本。

## RED / GREEN

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesReportAllocationCommandServiceTest#shouldTreatNullAndZeroAllocationQuantitiesAsZeroWhenSaving" "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，旧逻辑在 `aggregateDesired` 抛 `PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED`。
- GREEN: `mvn -q -pl yudao-module-mes -am "-Dtest=MesReportAllocationCommandServiceTest#shouldTreatNullAndZeroAllocationQuantitiesAsZeroWhenSaving+manualFullAllocationSameAsCurrentMustStillReconcileCompletionProgress+fifoFullAllocationSameAsCurrentMustStillReconcileCompletionProgress" "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 tests / 0 failures / 0 errors。
- REGRESSION: `mvn -q -pl yudao-module-mes "-Dtest=MesReportAllocationCommandServiceTest" "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- STATIC: 残留旧校验文案和 `allocatedQuantity` 正数注解搜索为空。

## Blockers

- None currently.
