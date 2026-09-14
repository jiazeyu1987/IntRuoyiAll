# Execution Log

## User Intent

- 用户反馈：点击重排仍提示 `排产资源缺少班次小时配置，routeProcessId=926632, workstationId=980008`。
- 期望：老报工数据只影响已完成/剩余量，剩余任务按当前最新工艺路线重新找工序、工作站、产线、产能；班次小时缺失默认 `10.5`。

## BDD Scenarios

- BDD: 重排缺班次小时使用默认值 -> Given 当前工艺路线剩余工序绑定的工作站缺少班次小时 / When 点击重排 / Then 不报 `排产资源缺少班次小时配置`，按 `10.5` 小时计算班次产能。
- BDD: 其它资源缺失仍 fail-fast -> Given 当前工艺路线剩余工序没有可用工作站或产能 / When 点击重排 / Then 仍返回正式资源配置错误，不用 `10.5` 掩盖资源缺失。

## Preflight Evidence

- Read: `bug-regression-fix-loop` skill and `references/bug-contract.md`.
- Read: `backend-api-delivery` skill and `references/backend-contract.md`.
- Read: `docs/task-closeout-rules.md`.
- Read: `docs/backend-development.md`.
- Read: `docs/powershell-encoding.md`.
- Read: `docs/powershell-memory.md`.
- Experience index routing matched: `docs/backend-development.md#第三方报工直报正式链路门禁`, `docs/powershell-memory.md#maven-单模块陈旧依赖门禁`.
- Git state: `int_main` has unrelated dirty files from parallel tasks; this task will use explicit path staging only.
- Project experience consolidation: merged the durable lesson into `docs/backend-development.md#第三方报工直报正式链路门禁` and added routing keywords in `docs/experience-index.md`; no new long-term experience document needed.

## TDD Log

- RED: `workdir=E:\IntRuoyi\IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldDefaultWorkbenchShiftHoursWhenAnyWorkstationMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `PRO_SCHEDULE_ORDER_SHIFT_HOURS_REQUIRED`，错误参数包含 `routeProcessId=3, workstationId=980008`。
- GREEN: `workdir=D:\IntRuoyiWorktree\verify-replan-shift-hours-default-20260806\IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldDefaultWorkbenchShiftHoursWhenAnyWorkstationMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `workdir=D:\IntRuoyiWorktree\verify-replan-shift-hours-default-20260806\IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldDefaultWorkbenchShiftHoursWhenAnyWorkstationMissing,MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldUseDefaultShiftHoursWhenWorkbenchShiftHoursMissing,MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldUseWorkbenchShiftHoursForUnboundFiniteHourlyConfig" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 3, Failures: 0, Errors: 0, Skipped: 0。

## Implementation Notes

- Root cause: 点击重排刷新排产工序时仍走 `MesProAutoScheduleServiceImpl.resolveUnifiedWorkbenchShiftHoursOrNull`；该路径会扫描工作站班次小时，旧逻辑先把缺失值默认成 `10.5`，再与已有 `8.00` 比较，最终把“缺失班次小时”误判为“统一班次小时配置冲突/缺失”，抛出用户看到的 `排产资源缺少班次小时配置`。
- Fix: `resolveUnifiedWorkbenchShiftHoursOrNull` 先识别任一工作站班次小时为空或非正数，直接返回用户授权默认 `10.5`；只有所有工作站都存在正数班次小时后，才比较是否存在多个不同正数配置。
- Scope: 本次只覆盖班次小时缺失默认值；当前工艺路线剩余工序缺工作站、缺可用产能等正式资源错误仍由原 fail-fast 校验抛出。
- Isolation: 主工作区 Maven target 被并行 Maven 进程占用，目标 GREEN 在 `D:\IntRuoyiWorktree\verify-replan-shift-hours-default-20260806` 隔离验证 worktree 完成；未启动服务，未占用端口。

## Verification Evidence

- PASS: 新增回归用例覆盖 `workstationId=980008` 缺少班次小时且其它工作站有 `8.00` 的混合场景，确认重排刷新使用 `10.5` 并计算班次产能 `270.0000030`。
- PASS: 相邻班次小时用例确认缺失默认值与已有统一工作站班次小时逻辑均未回归。
- PASS: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260806-replan-shift-hours-default-regression/bug-regression-evidence.md` -> PASS。
- PASS: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260806-replan-shift-hours-default-regression/backend-api-evidence.md` -> PASS。
- PASS: `git diff --check -- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImpl.java IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImplTest.java doc/tasks/20260806-replan-shift-hours-default-regression` -> PASS，仅 CRLF working-copy 提示。
- PASS: Removed detached verification worktree `D:\IntRuoyiWorktree\verify-replan-shift-hours-default-20260806`; remaining worktree list no longer contains it.
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260806-replan-shift-hours-default-regression --mode preview` -> ready，kept `task.md` / `execution-log.md` / `verification-report.md`，delete list only temporary evidence files。
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260806-replan-shift-hours-default-regression --mode apply` -> applied，deleted `backend-api-evidence.md` and `bug-regression-evidence.md`。
- PASS: `rg -n "排产资源缺少班次小时配置|defaultShiftHoursWhenMissing|默认10.5小时|缺班次小时" docs\experience-index.md docs\backend-development.md` -> PASS，经验索引可定位到后端门禁。
- PASS: `git diff --check -- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImpl.java IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImplTest.java doc/tasks/20260806-replan-shift-hours-default-regression docs/backend-development.md docs/experience-index.md` -> PASS，仅 CRLF working-copy 提示。
- PASS: Implementation commit `c76f3fec0` (`修复重排班次小时默认值回归`) created with explicit-path staging; unrelated parallel task files remained unstaged。
- Pending: final closeout commit and push verification。

## Blockers

- None.
