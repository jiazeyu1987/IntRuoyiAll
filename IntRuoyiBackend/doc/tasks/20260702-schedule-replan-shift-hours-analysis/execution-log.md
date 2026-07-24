# Execution Log

## BDD Scenarios

BDD: 工作台班时变更应影响手动重排计划完成日期 -> Given 排产工作台修改班时 / When 对相关排产工单执行手动重排 / Then 重排应使用最新可用产能并回写 `plannedEndTime`。

## Read-Only Evidence

- READONLY: `docs/powershell-memory.md` -> PASS，已确认 Windows PowerShell 与中文 UTF-8 读写门禁。
- READONLY: `docs/experience-index.md` -> PASS，命中 PowerShell 门禁；本次未命中真实 E2E、服务器、发布、备份、恢复或 worktree 高风险门禁。
- READONLY: `yudao-ui-admin-vue3/src/views/mes/pro/scheduleorder/index.vue` -> PASS，排产工单列表“计划完成”列绑定 `row.plannedEndTime`。
- READONLY: `yudao-ui-admin-vue3/src/views/mes/pro/scheduleorder/index.vue` -> PASS，手动重排请求传入 `scheduleOrderIds`、`startTime`、`capacityMode`、`preserveManualLockedTasks`，应用时调用 `ProTaskAutoScheduleApi.replanApply` 后刷新列表。
- READONLY: `ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImpl.java` -> PASS，`replanApply` 复用 `applyInternal`，`syncScheduleOrderPlanFields` 会根据最终步骤最大 `endTime` 回写排产工单 `plannedEndTime`。
- READONLY: `ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedulerworkbench/MesProSchedulerWorkbenchServiceImpl.java` -> PASS，工作台 `saveShiftHoursSetting` 只调用 `workstationMapper.updateAllShiftHours(shiftHours)`。
- READONLY: `ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/md/workstation/MesMdWorkstationMapper.java` -> PASS，`updateAllShiftHours` 只更新 `MesMdWorkstationDO::getShiftHours`。
- READONLY: `ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImpl.java` -> PASS，自动排产/重排加载产能时读取 `planShiftService.getPlanShiftListByPlanId`、`capacityPlanMapper.selectListByLineIdsAndDate`，再构建 `shiftWindowsByLineId`。
- READONLY: `ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProScheduleCalendarServiceImpl.java` -> PASS，计划产能覆盖由排程日历班次 `startTime/endTime` 计算 `capacityMinutes`，且已有日期班次产能存在时跳过生成，不会因工作站 `shiftHours` 改动自动重算。

## Root Cause

- 根因不是 `plannedEndTime` 回写缺失；源码中手动重排会回写排产工单计划开始和计划完成。
- 根因是班时配置入口和重排产能入口数据源不一致：
  - 工作台“班时”保存到工作站 `shiftHours`。
  - 手动重排使用排程日历班次 `MesCalPlanShiftDO.startTime/endTime` 和产能计划 `MesProCapacityPlanDO.capacityMinutes`。
  - 已生成的日期班次产能存在时，`generateCapacityPlans` 会以 `EXISTING_CAPACITY` 跳过，不根据工作站班时刷新。
- 因此更换工作台班时后，如果未同步更新排程日历班次窗口或重建对应日期产能计划，手动重排仍使用旧的班次分钟数，最终任务时间不变，排产工单 `plannedEndTime` 也不变。

## Verification

- VERIFICATION: 源码链路检查 -> PASS，已完成只读根因定位。
- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProSchedulerWorkbenchServiceImplTest#saveShiftHoursSetting_shouldUpdateAllWorkstationsAndReturnUnifiedSetting" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增接口方法后 `MesProScheduleCalendarServiceImpl` 尚未实现 `refreshPlanCapacityForShiftHours(BigDecimal)`，编译失败。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProSchedulerWorkbenchServiceImplTest#saveShiftHoursSetting_shouldUpdateAllWorkstationsAndReturnUnifiedSetting,MesProScheduleCalendarServiceImplTest#refreshPlanCapacityForShiftHours_shouldUpdatePrimaryShiftWindowAndFutureCapacityPlans" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 2, Failures: 0, Errors: 0, Skipped: 0。

## Completed Fix

- 工作台 `saveShiftHoursSetting` 增加事务，并在更新工作站 `shiftHours` 后调用排程日历刷新。
- 排程日历刷新从当前模拟日期起，将班时换算为分钟，更新启用产线首个排班班次的 `endTime`，并精确刷新对应产线/班次的已启用未来产能计划 `capacityMinutes`。
- 缺少启用产线、排班计划或班次配置时明确报错，不静默跳过或降级。

## Final Verification

- VERIFICATION: 目标 Maven 单测 -> PASS，工作台保存班时会联动排程日历刷新；10.50 小时被同步为一班 `08:00-18:30` 和 630 分钟产能计划。
- LIMITATION: 未执行真实 E2E、数据库写入或服务器操作；本次验证范围为本地后端单元回归。


## E2E Verification Attempt

- GREEN: experience-preflight -> PASS，本轮用户明确要求进行 E2E 验证；范围限定为本机 http://localhost:8081 前端与 48081 后端，使用测试租户 aoteman 真实登录路径；不访问测试服/正式服，不直接 SQL 写业务表。
- RED: login-preflight-default-browser -> FAIL，官方登录预检脚本启动 Playwright 默认 headless shell 失败，错误为 Invalid file descriptor to ICU data received，属于本机浏览器运行前置失败，非业务登录失败。
- BLOCKER: targeted-test-after-lazy-workorder -> FAIL，当前工作区存在本任务无关的 `MesProBatchRecordReportJsonBuilderTest#countNumericKeys` 测试编译脏改，导致 Maven testCompile 阶段失败；本轮 E2E 为恢复本地运行态，先执行主代码编译与运行包构建，不把该无关测试失败静默视为通过。
- GREEN: backend-local-runtime -> PASS，`mvn.cmd -pl yudao-module-mes -Dmaven.test.skip=true compile` 主代码编译通过；因无关测试源码脏改阻塞 testCompile，运行包使用 `mvn.cmd -pl yudao-server -am -Dmaven.test.skip=true package` 构建后通过本地脚本 `-SkipBuild` 启动，`http://localhost:48081/actuator/health` 返回 200。
- GREEN: login-preflight-system-chrome -> PASS，使用系统 Chrome `C:\Program Files\Google\Chrome\Application\chrome.exe` 真实登录测试租户 `测试租户/aoteman` 并进入 `/mes/pro/scheduler-workbench`。
- GREEN: e2e-shift-hours-replan -> PASS，Playwright 真实页面路径完成：登录测试租户 -> 排产工作台修改班时 `7.5 -> 9.5` 并保存 -> 排产工单页按工单编码筛选并勾选 `SMART-SCHED-20260630-RERUN5-MO` -> 手动重排预览 -> 应用重排 -> 接口复核 `plannedEndTime` 从 `1783562460000` 变为 `1783571280000`，预览生成任务 24 个，阻断问题 0 个；脚本 finally 已恢复班时到本轮开始值 `7.5`。
- RED: git-commit-without-java-test -> FAIL，提交门禁拒绝后端生产代码补充提交，原因是本次 `MesProScheduleCalendarServiceImpl` 启动循环依赖修复缺少同步变更的 Java 测试。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProScheduleCalendarServiceImplTest#circularReferenceProneCollaborators_shouldUseLazyInjection" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0，锁定 `workOrderService` 与 `routeService` 保持 `@Lazy` 注入，避免本地运行包启动时再次触发循环依赖。
