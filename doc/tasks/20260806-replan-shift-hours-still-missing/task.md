# 重排班次小时缺失仍报错修复

## Task Goal

修复点击重排仍提示 `排产资源缺少班次小时配置，routeProcessId=926632, workstationId=980008` 的回归：当前工艺路线工作站 `shift_hours` 为空或非正数时，手动重排必须按默认 `10.5` 小时计算，不能阻断排产。

## Milestones

- [x] 建立任务记录并确认后端门禁。
- [x] 复现当前抛错路径并补充失败回归测试。
- [x] 实现最小修复，确保当前路线资源计算按默认 `10.5`。
- [x] 运行定向 Maven 回归并更新验证记录。
- [ ] 重启本机 `int_main` 后端到修复后的最新 Jar。
- [ ] 收尾清理、沉淀经验、提交并推送。

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldDefaultWorkbenchShiftHoursWhenWorkbenchValuesDiffer" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldUseDefaultShiftHoursWhenWorkbenchShiftHoursMissing+refreshScheduleOrderProcessesFromRouteConfig_shouldDefaultWorkbenchShiftHoursWhenAnyWorkstationMissing+refreshScheduleOrderProcessesFromRouteConfig_shouldDefaultWorkbenchShiftHoursWhenWorkbenchValuesDiffer" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-server -am "-DskipTests" package`
- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health`

## Applicable Gates

- `docs/backend-development.md#第三方报工直报正式链路门禁`：工作站 `shift_hours` 为空或非正数时，手动重排必须按默认 `10.5` 小时计算，不得把缺班次小时误判为 blocker。
- `docs/task-closeout-rules.md`：修复必须记录 BDD、RED、GREEN 和收尾证据。
- `bug-regression-fix-loop`：先复现并补失败回归，再实施最小修复。

## Current Status

in_progress

已完成 RED/GREEN 和相邻回归，准备提交实现并从干净 worktree 构建运行 Jar。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；这是用户明确要求的业务默认值规则，缺班次小时按正式默认 `10.5` 计算。
- `是否从根因和长期维护角度解决`：是；修正当前工作站资源校验路径，而不是隐藏错误。
- `是否存在临时补丁或绕过`：否。
