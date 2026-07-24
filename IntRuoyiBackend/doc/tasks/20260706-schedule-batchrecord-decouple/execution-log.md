# Execution Log - 20260706-schedule-batchrecord-decouple

## BDD
- BDD: 缺少批次号不阻断排产 -> Given 工艺批记录路线已启用且生产工单未维护批次号 / When 执行排产前检查 / Then 排产前检查不产生 `BLOCKED_BATCH_CODE_REQUIRED` 阻断，结果仍可通过排产域校验。
- BDD: 批记录路线模板缺失不阻断排产 -> Given 工艺批记录路线配置缺少默认批记录或绑定无效 / When 执行排产前检查 / Then 排产前检查不产生 `BLOCKED_BATCH_ROUTE_CONFIG_INVALID` 阻断，批记录问题由批记录系统自身处理。
- BDD: 排产域阻断仍保留 -> Given 排产自身路线、工序、日历或产能不满足 / When 执行排产前检查 / Then 继续按排产域规则返回阻断。

## TDD Evidence
- RED: `mvn.cmd -pl yudao-module-mes -Dtest=MesProScheduleOrderPreflightServiceTest test` -> FAIL，`preflight_shouldNotBlockMissingBatchCodeWhenBatchRouteEnabled` 与 `preflight_shouldNotBlockInvalidBatchRouteConfigWhenDefaultReportMissing` 仍返回 `BLOCKED`，证明旧实现仍由批记录域阻断排产前检查。
- GREEN: `mvn.cmd -pl yudao-module-mes -Dtest=MesProScheduleOrderPreflightServiceTest test` -> PASS，4 tests, 0 failures, 0 errors。

## Commands
- `rg -n "appendBatchPreflightIssues|BLOCKED_BATCH_CODE_REQUIRED|BLOCKED_BATCH_ROUTE_CONFIG_INVALID|BatchRouteValidationResult" ...MesProScheduleOrderServiceImpl.java` -> 定位排产前检查批记录阻断链路。
- `apply_patch` -> 调整两条排产前检查回归测试，使批记录批次号/模板绑定问题不再期待阻断排产。
- `mvn.cmd -pl yudao-module-mes -Dtest=MesProScheduleOrderPreflightServiceTest test` -> RED FAIL，2 failures。
- `apply_patch` -> 移除排产前检查中的批记录路线、批次号、批记录模板绑定校验与相关 helper。
- `rg -n "BLOCKED_BATCH_CODE_REQUIRED|BLOCKED_BATCH_ROUTE_CONFIG_INVALID|appendBatchPreflightIssues|MesProBatchRecordReport|MesProRouteUseProcessBatchRecord|batchRouteAction|BatchRouteValidationResult" ...MesProScheduleOrderServiceImpl.java` -> PASS，服务内不再存在批记录前置阻断。
- `mvn.cmd -pl yudao-module-mes -Dtest=MesProScheduleOrderPreflightServiceTest test` -> GREEN PASS，4 tests, 0 failures, 0 errors。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260706-schedule-batchrecord-decouple --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260706-schedule-batchrecord-decouple --mode apply` -> PASS，deleted_paths 为 `<none>`。
- `mvn.cmd -pl yudao-module-mes -Dtest=MesProScheduleOrderPreflightServiceTest test` -> REGRESSION PASS，4 tests, 0 failures, 0 errors。
