# Execution Log

- GREEN: experience-preflight -> PASS, this slice is code-only and read-only; no real tenant write, Playwright write chain, server restart, release, backup, restore, or schema migration is executed.
- BDD: 演练角色前置完整时预检通过 -> Given 执行人、审批人、归档员具备菜单权限、签名授权、BPM 发起资格和路线批记录配置 / When 调用 eDHR 演练预检 / Then 返回 overallStatus=PASS 且无 BLOCKER。
- BDD: 缺少关键前置时预检阻塞 -> Given 任一角色缺少菜单、签名授权、BPM startUserIds 或路线权限范围 / When 调用 eDHR 演练预检 / Then 返回 overallStatus=BLOCKED 且逐项说明 blocker code、责任角色和修复建议。
- BDD: 只读预检不自动修复数据 -> Given 预检发现缺口 / When 服务返回结果 / Then 不写入角色、菜单、BPM、签名或路线配置数据。
- BDD: 排产应用前预检发现批记录路线缺口必须阻塞 -> Given 排产工单启用了工艺批记录路线但缺少批次号、默认批记录或绑定无效 / When 应用自动排产 / Then 系统必须抛出明确阻塞错误且不写入任务。
- BDD: 最晚开工约束导致零任务时必须阻塞发布 -> Given 最晚开工约束排除了全部计划 / When 应用自动排产 / Then 系统必须抛出零任务阻塞错误且不发布空排程。

## Phase: task-package

- changed paths:
  - `ruoyi-vue-pro/doc/tasks/20260622-edhr-rehearsal-readiness-preflight/task.md`
  - `ruoyi-vue-pro/doc/tasks/20260622-edhr-rehearsal-readiness-preflight/execution-log.md`
  - `ruoyi-vue-pro/doc/tasks/20260622-edhr-rehearsal-readiness-preflight/backend-api-evidence.md`
- validation:
  - RED: mvn -pl yudao-module-mes "-Dtest=MesProEdhrRehearsalReadinessServiceTest,MesProEdhrBatchExecutionControllerTest" test -> FAIL, expected missing MesProEdhrRehearsalReadinessCommand/Result/Service/Impl and controller method

## Phase: implementation

- changed paths:
  - `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrBatchExecutionController.java`
  - `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrRehearsalReadinessCommand.java`
  - `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrRehearsalReadinessResult.java`
  - `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrRehearsalReadinessService.java`
  - `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrRehearsalReadinessServiceImpl.java`
  - `yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrBatchExecutionControllerTest.java`
  - `yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrRehearsalReadinessServiceTest.java`
  - `yudao-module-mes/src/test/resources/sql/create_tables.sql`
  - `yudao-module-mes/src/test/resources/sql/clean.sql`
- implemented behavior:
  - Added read-only eDHR rehearsal readiness service and controller endpoint.
  - Checks eDHR parent menu and role-specific menu permission for executor, approver, and archiver.
  - Checks DCC electronic signature authorization for executor, approver, and archiver.
  - Checks active BPM definition and executor start eligibility for `mes-edhr-approval-v1`.
  - Checks route BATCH batch-record binding and referenced permission scope.
  - Returns stable blocker codes without writing repair data.
- validation:
  - GREEN: mvn -pl yudao-module-mes "-Dtest=MesProEdhrRehearsalReadinessServiceTest,MesProEdhrBatchExecutionControllerTest" test -> PASS, 7 tests, 0 failures, 0 errors
  - GREEN: python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260622-edhr-rehearsal-readiness-preflight\backend-api-evidence.md -> PASS

## Phase: schedule-apply-preflight

- changed paths:
  - `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/enums/ErrorCodeConstants.java`
  - `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImpl.java`
  - `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/scheduleorder/MesProScheduleOrderServiceImpl.java`
  - `yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleAlgorithmContractTest.java`
  - `yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleContractTest.java`
  - `yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImplTest.java`
  - `yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/scheduleorder/MesProScheduleOrderPreflightServiceTest.java`
- implemented behavior:
  - 自动排产 apply 前调用排产工单预检，发现 BLOCKED 结果时抛出 `PRO_AUTO_SCHEDULE_PREFLIGHT_BLOCKED`。
  - 工艺批记录路线启用时，预检阻塞缺少批次号、缺少默认批记录或绑定无效的排产工单。
  - 最晚开工约束导致未生成任何任务时抛出 `PRO_AUTO_SCHEDULE_LATEST_START_ZERO_TASK_BLOCKED`，避免发布空排程。
- validation:
  - `GREEN: mvn -pl yudao-module-mes "-Dtest=MesProScheduleOrderPreflightServiceTest,MesProAutoScheduleAlgorithmContractTest,MesProAutoScheduleContractTest,MesProAutoScheduleServiceImplTest,MesProEdhrRehearsalReadinessServiceTest,MesProEdhrBatchExecutionControllerTest" test -> PASS, 46 tests, 0 failures, 0 errors`
