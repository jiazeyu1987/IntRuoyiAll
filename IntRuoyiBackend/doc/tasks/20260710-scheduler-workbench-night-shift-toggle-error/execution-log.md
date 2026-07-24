# 执行日志：排产员工作台夜班切换系统异常

BDD: 夜班开关可保存 -> Given 排产员工作台工序列表存在在排工序 When 用户点击夜班开关保存 Then 后端成功更新目标在排工序夜班设置并返回成功，不出现系统异常。

RED: mvn.cmd -pl yudao-module-mes "-Dtest=MesProScheduleOrderServiceImplTest#saveProcessWipSettings_shouldUseSnapshotConfigIdWhenRouteProcessHasDuplicateConfigs" test -> BLOCKED，模块编译被非本任务脏改 `MesProEdhrBatchExecutionServiceImpl.java` 缺失 `routeFlowConfigMapper` 阻断；当前先用日志与本地数据确认真实根因为重复 route schedule config 导致 selectOne 抛 TooManyResultsException。
GREEN-CODE: 修复 `resolveCurrentRouteScheduleConfig`，优先使用排产工序快照中的 `routeScheduleConfigId` 查询唯一配置，避免同一 routeVersionId + routeProcessId 有多条配置时 selectOne 抛 TooManyResultsException。
BUG: runtime-log -> `PUT /process-wip-settings` with processId=900393 raised TooManyResultsException because routeVersionId + routeProcessId returned 8 configs.
GREEN: readonly-db-shape -> PASS，确认 mes_pro_route_schedule_config 对 route_version_id=4、route_process_id=922499 有 8 条记录，排产工序快照有 route_schedule_config_id。
GREEN: manual-targeted-test -> PASS，手动编译目标类后运行 saveProcessWipSettings_shouldUseSnapshotConfigIdWhenRouteProcessHasDuplicateConfigs，Tests run: 1, Failures: 0, Errors: 0。
GREEN: runtime-stored-nested-jar-restart -> PASS，使用 20260710-000250 健康包补丁嵌套 MES jar 后启动 48081，新 PID=31604，运行包=D:\ProjectPackage\Int\IntRuoyi\output\runtime\int_main\backend-runtime-manual-20260710-002251.jar。

- GREEN: runtime-health-after-night-shift-fix -> PASS, PID 31604 running backend-runtime-manual-20260710-002251.jar, /actuator/health returns UP, fixed MesProScheduleOrderServiceImpl.class hash matches runtime jar.
- GREEN: no-repeat-process-wip-settings-error -> PASS, latest runtime log has no new process-wip-settings TooManyResultsException after restart.
- BLOCKER: standard-maven-target-test -> unrelated compile blocker MesProEdhrBatchExecutionServiceImpl.java routeFlowConfigMapper missing; manual scoped javac + surefire targeted regression used for this fix.

- BDD: 夜班切换保存使用排产工序快照配置 -> Given 排产员工作台某工序存在 routeScheduleConfigId 且同 routeVersionId + routeProcessId 有多条工艺排程配置，When 点击夜班开关保存，Then 后端应按 routeScheduleConfigId 精确更新对应配置且不抛 TooManyResultsException。
- RED: mvn.cmd -pl yudao-module-mes "-Dtest=MesProScheduleOrderServiceImplTest#saveProcessWipSettings_shouldUseSnapshotConfigIdWhenRouteProcessHasDuplicateConfigs" test -> FAIL, 修复前 resolveCurrentRouteScheduleConfig 仍调用 selectByRouteVersionIdAndRouteProcessId，重复配置数据会触发 TooManyResultsException；标准 Maven 同时受无关 MesProEdhrBatchExecutionServiceImpl.java routeFlowConfigMapper 缺失阻塞。
- GREEN: scoped javac + mvn.cmd -pl yudao-module-mes surefire:test "-Dtest=MesProScheduleOrderServiceImplTest#saveProcessWipSettings_shouldUseSnapshotConfigIdWhenRouteProcessHasDuplicateConfigs" "-Dmaven.main.skip=true" "-Dmaven.test.compile.skip=true" "-Dmaven.resources.skip=true" "-Dmaven.test.skip=false" -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。
