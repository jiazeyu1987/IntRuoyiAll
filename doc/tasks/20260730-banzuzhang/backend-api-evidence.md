# F9/F10 Backend API Evidence

## Scope

F9/F10 班组长后端能力覆盖 `MES 工序池班组长工作台`：

- `MesProcessPoolTeamLeaderController` 提供提交看板、提交详情、提交复核、生产工单异常上报、员工绑定、员工禁用、不良原因维护、设备参数上下限维护接口。
- 服务层覆盖班组长负责范围、提交详情范围校验、复核记录、异常记录、员工候选绑定、维护审计和参数上下限校验。
- 时间轴查询扩展 `employeeUserIds`，用于班组长只查看负责员工/PQC 员工提交。

## Contract

- Endpoint: `/mes/pro/process-pool/team-leader/submission/page`，权限 `mes:pro-process-pool-team-leader:query`。
- Endpoint: `/mes/pro/process-pool/team-leader/submission/detail`，权限 `mes:pro-process-pool-team-leader:query`。
- Endpoint: `/mes/pro/process-pool/team-leader/submission/review`，权限 `mes:pro-process-pool-team-leader:review`。
- Endpoint: `/mes/pro/process-pool/team-leader/work-order/abnormal/report`，权限 `mes:pro-process-pool-team-leader:abnormal`。
- Endpoint: `/mes/pro/process-pool/team-leader/employee-binding/add`，权限 `mes:pro-process-pool-team-leader:maintain`。
- Endpoint: `/mes/pro/process-pool/team-leader/employee-binding/disable`，权限 `mes:pro-process-pool-team-leader:maintain`。
- Endpoint: `/mes/pro/process-pool/team-leader/defect-reason/create`，权限 `mes:pro-process-pool-team-leader:maintain`。
- Endpoint: `/mes/pro/process-pool/team-leader/device-parameter-rule/save`，权限 `mes:pro-process-pool-team-leader:maintain`。
- `leaderUserId` 不从前端输入读取；Controller 统一用 `SecurityFrameworkUtils.getLoginUserId()` 注入。
- 复核只新增复核记录，不改写原始工序池提交事件、记录本、报工、FIFO 或审核副本。
- 异常上报面向生产工单，但不得扩大非负责员工提交详情权限。

## Validation

- 负责员工为空时，提交看板返回空分页，不扩大查询范围。
- 查询指定员工时，必须先校验该员工属于当前班组长负责范围。
- 查询提交详情时，按提交事件中的实际员工校验负责范围。
- 复核状态只允许 `APPROVED/REJECTED`，非法状态失败。
- 员工绑定、禁用、设备参数维护必须校验班组长对目标工序有维护范围。
- 设备参数上下限要求 `lowerLimit <= upperLimit`。
- 缺少 leader、工序、员工、设备、参数、事件等正式上下文时 fail fast，不返回默认成功。

## BDD

- BDD: 生产班组长按负责范围查看员工提交 -> Given 班组长负责员工 E1001/E1002 且其他员工也有工序池提交 / When 打开提交看板 / Then 只展示负责员工提交并显示复核、PQC、异常和追溯入口。
- BDD: 班组长复核员工提交 -> Given 负责员工存在待复核工序池提交事件 / When 班组长提交复核说明 / Then 只写复核状态、复核人、服务端时间和说明，不改写原始 payload、报工、记录本、签名或提交时间。
- BDD: 班组长查看所有生产工单并异常上报 -> Given 班组长具备异常处理权限 / When 对生产工单标记异常并上报 / Then 保存异常和上报记录，且不得泄露非负责员工提交明细。
- BDD: 越权访问和维护失败 -> Given 班组长不负责目标员工或工序 / When 查看、复核或维护 / Then 系统拒绝并不返回原始详情或写配置。

## RED

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，expected reason: F9/F10 Controller、VO、服务和权限契约尚未实现。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderScopeServiceTest,MesTeamLeaderSubmissionReviewServiceTest,MesWorkOrderAbnormalReportServiceTest,MesTeamEmployeeBindingServiceTest,MesDefectReasonCatalogServiceTest,MesProcessDeviceParameterRuleServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，expected reason: 班组长范围、复核、异常和维护服务缺失。

## GREEN

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesProcessPoolTeamLeaderControllerTest,MesTeamLeaderScopeServiceTest,MesTeamLeaderSubmissionReviewServiceTest,MesWorkOrderAbnormalReportServiceTest,MesTeamEmployeeBindingServiceTest,MesDefectReasonCatalogServiceTest,MesProcessDeviceParameterRuleServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`BUILD SUCCESS`，16 tests，0 failures，0 errors，0 skipped。

## Verification

- Backend targeted regression passed on 2026-07-30 20:32 local time.
- Controller static contract is covered by `MesProcessPoolTeamLeaderControllerTest`.
- Service range and validation behavior is covered by six focused service tests.
- Timeline employee scope filter is covered through workbench and mapper contract checks.

## Observability

- 写入型动作全部落正式表和维护审计表。
- 复核、异常上报和维护动作使用服务端时间。
- 异常和维护写入保留 leader/user/process/source ID，便于后续审计和追溯。

## Blockers

- Real Playwright E2E 尚未运行：当前 worktree slot 前端 `8098`、后端 `48098` 未监听，缺真实运行态入口。
- 写入型真实 E2E 仍需测试租户、班组长账号、员工、工序、设备、生产工单和可清理测试数据。
