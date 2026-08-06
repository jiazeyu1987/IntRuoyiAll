# Execution Log

## User Intent

- 用户确认：如果工作站没有班次小时配置，默认按 `10.5` 小时计算。
- 用户确认：排产管理员的班次/小时设置不允许为空，默认值是 `10.5`。

## BDD Scenarios

- BDD: 缺工作站班次小时使用默认值 -> Given 当前最新工艺路线剩余工序绑定的工作站没有班次小时配置 / When 点击排产或重排 / Then 系统按 `10.5` 小时计算班次产能，不再报“排产资源缺少班次小时配置”。
- BDD: 排产管理员班次小时默认必填 -> Given 排产管理员打开班次/小时设置 / When 页面初始化或用户清空输入 / Then 字段默认显示 `10.5`，保存时不允许空值或非正数。

## Preflight Evidence

- Read: `docs/task-closeout-rules.md`.
- Read: `docs/backend-development.md`.
- Read: `docs/frontend-development.md`.
- Read: `docs/powershell-encoding.md`.
- Read: `docs/powershell-memory.md`.
- Skills read: `backend-api-delivery`, `frontend-feature-delivery`, `bug-regression-fix-loop` and their contract references.
- Git preflight: `int_main`, `origin=https://github.com/jiazeyu1987/IntRuoyiAll.git`.
- Existing dirty state before this task preserved by baseline commits `e4a8226e6` and `6d090e257`.

## TDD Log

- RED: `node IntRuoyiFronted/tests/e2e/scheduler-workbench-shift-hours-default-static.spec.cjs` -> FAIL, expected reason: 页面缺少集中默认 `const DEFAULT_SHIFT_HOURS = 10.5`。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldUseDefaultShiftHoursWhenWorkbenchShiftHoursMissing,MesProScheduleOrderAdmissionTest#createFromWorkOrder_shouldUseDefaultShiftHoursWhenWorkstationShiftHoursMissing,MesProScheduleOrderServiceImplTest#createFromWorkOrder_shouldUseDefaultShiftHoursWhenWorkstationShiftHoursMissing,MesProScheduleOrderNoDefaultConfigContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: 自动重排和排产工单创建仍抛 `PRO_SCHEDULE_ORDER_SHIFT_HOURS_REQUIRED`，静态合同缺少显式默认值；`AdmissionTest` 首次进入成功路径后暴露测试夹具缺少 `syncRecordMapper`。
- GREEN: `node IntRuoyiFronted/tests/e2e/scheduler-workbench-shift-hours-default-static.spec.cjs` -> PASS。
- GREEN: `node IntRuoyiFronted/tests/e2e/mes-scheduler-workbench-shift-hours-static.spec.js` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldUseDefaultShiftHoursWhenWorkbenchShiftHoursMissing,MesProScheduleOrderAdmissionTest#createFromWorkOrder_shouldUseDefaultShiftHoursWhenWorkstationShiftHoursMissing,MesProScheduleOrderServiceImplTest#createFromWorkOrder_shouldUseDefaultShiftHoursWhenWorkstationShiftHoursMissing,MesProScheduleOrderNoDefaultConfigContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 4 tests, 0 failures, 0 errors.
- Note: `mvn -pl yudao-module-mes ... test` without `-am` failed before tests because local Maven repository had stale `system` API artifacts; per project reactor rule, final backend verification used `-am`.

## Implementation Notes

- Added explicit user-approved `10.5` default in `ScheduleDefaultCompatibilityPolicy`.
- Auto schedule refresh now uses the default when latest workstation or workbench shift hours are missing or non-positive.
- Schedule order resource snapshot now stores default `shiftHours=10.5` and calculated shift capacity when a workstation exists but lacks shift hours.
- Scheduler workbench settings now initialize and reload shift hours as `10.5`, reject empty/non-positive values, and preserve API error behavior.
- Existing frontend static test path was corrected from `RouteProcessList.vue` to the actual `RouteMesProcessList.vue`.

## Verification Evidence

- Frontend default static contract passed.
- Frontend existing shift-hours static contract passed.
- Backend targeted reactor Maven tests passed for auto schedule refresh, admission create, schedule order snapshot, and static default contract.
- `git diff --check -- IntRuoyiBackend/yudao-module-mes/src/main/java IntRuoyiBackend/yudao-module-mes/src/test/java IntRuoyiFronted/src IntRuoyiFronted/tests/e2e doc/tasks/20260806-schedule-default-shift-hours` -> PASS; only Git LF/CRLF warnings.
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260806-schedule-default-shift-hours/backend-api-evidence.md` -> PASS.
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-schedule-default-shift-hours/frontend-feature-evidence.md` -> PASS.
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260806-schedule-default-shift-hours/bug-regression-evidence.md` -> PASS.
- Experience consolidation: updated `docs/powershell-memory.md#maven-单模块陈旧依赖门禁` and `docs/experience-index.md` for stale local reactor dependency / `-am` verification routing.
- Experience index check: `rg -n "Maven 单模块陈旧依赖|stale local reactor dependencies|不带 -am 测试前失败" docs\experience-index.md docs\powershell-memory.md` -> PASS.
- Cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260806-schedule-default-shift-hours --mode preview` -> PASS; keep core task records, delete 3 temporary evidence files, no blockers/warnings.
- Cleanup apply: same script with `--mode apply` -> PASS; deleted `backend-api-evidence.md`, `bug-regression-evidence.md`, and `frontend-feature-evidence.md`.

## Blockers

- Pending commit and push. Working tree also contains unrelated modified source/test/task files from parallel work; they are not part of this task and must not be mixed into this task's commits.
