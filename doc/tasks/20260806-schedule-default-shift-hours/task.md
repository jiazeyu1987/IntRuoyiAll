# 排产班次小时默认值 10.5

## Task Goal

按用户确认的口径修正排产班次小时逻辑：工作站缺少班次小时配置时，排产默认按 `10.5` 小时计算；排产管理员的班次/小时设置不允许为空，默认值为 `10.5`。

## Milestones

- [x] 建立任务记录、BDD 场景和适用门禁。
- [x] 补充后端 RED 回归：缺工作站班次小时不再报错，按 10.5 计算。
- [x] 补充前端 RED 静态合同：排产管理员设置默认 10.5 且必填。
- [x] 实现后端默认班次小时和前端默认/必填。
- [x] 运行目标后端、前端验证并记录 GREEN。
- [x] 收尾清理、提交并推送。

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldUseDefaultShiftHoursWhenWorkbenchShiftHoursMissing,MesProScheduleOrderAdmissionTest#createFromWorkOrder_shouldUseDefaultShiftHoursWhenWorkstationShiftHoursMissing,MesProScheduleOrderServiceImplTest#createFromWorkOrder_shouldUseDefaultShiftHoursWhenWorkstationShiftHoursMissing,MesProScheduleOrderNoDefaultConfigContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `node IntRuoyiFronted/tests/e2e/scheduler-workbench-shift-hours-default-static.spec.cjs`
- `node IntRuoyiFronted/tests/e2e/mes-scheduler-workbench-shift-hours-static.spec.js`
- `git diff --check -- IntRuoyiBackend/yudao-module-mes/src/main/java IntRuoyiBackend/yudao-module-mes/src/test/java IntRuoyiFronted/src IntRuoyiFronted/tests/e2e doc/tasks/20260806-schedule-default-shift-hours`
- evidence validators for backend, frontend, and bug regression evidence.

## Applicable Gates

- `docs/backend-development.md`：排产服务行为变更必须 BDD/TDD，缺资源不得吞异常；本次用户明确指定唯一默认值 `10.5`，不是静默降级。
- `docs/frontend-development.md`：前端默认值、必填和 API 错误状态不得静默吞掉。
- `docs/powershell-memory.md`：提交前复扫脏工作区，明确基线提交与本任务实现提交边界。
- `docs/task-closeout-rules.md`：实现验证完成后进入 `ready_for_closeout`，运行 cleanup 后再标记 completed。

## Current Status

completed

后端与前端目标验证、diff check、evidence validator、经验沉淀、cleanup、实现提交和推送均已完成。实现提交 `84565e2ae` 已推送到 `origin/int_main`，本 completed 状态随最终收尾提交推送。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：是；用户明确要求缺班次小时时默认按 `10.5` 小时计算。触发条件仅限班次小时为空或无效；风险是隐藏基础资料遗漏，回滚方式是移除默认值并恢复 fail-fast 校验。
- `是否从根因和长期维护角度解决`：是；统一集中默认值，并用后端/前端合同锁定排产管理员默认值。
- `是否存在临时补丁或绕过`：否。
