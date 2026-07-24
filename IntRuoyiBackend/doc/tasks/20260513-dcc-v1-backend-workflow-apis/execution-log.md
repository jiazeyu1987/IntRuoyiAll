# Execution Log: DCC v1 backend workflow APIs

BDD: 提交受控文件触发 BPM 审批 -> Given 文件类别已绑定目录且存在激活的审批路线 / When 提交人提交受控文件 / Then 系统创建受控文件记录、固化路线快照、发起 BPM 流程，并将状态置为 APPROVING。

BDD: 提交人撤回审批中的受控文件 -> Given 受控文件处于 APPROVING 状态且属于当前提交人 / When 提交人发起撤回 / Then 系统取消对应 BPM 流程并将受控文件状态更新为 WITHDRAWN。

## Evidence

- M1: Completed. Previous backend task `doc/tasks/20260513-dcc-v1-backend-admin-apis/task.md` is completed.
- M2: Completed. This task document and execution log were created before backend workflow-API changes.
- M3: Completed.
  - RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, workflow request/response VOs, mapper helper methods, error codes, and service implementation did not exist.
- M4: Completed. Implemented controlled-file submission with category validation, directory binding lookup, active-route lookup, approver resolution, route snapshot persistence, and BPM process start.
- M5: Completed. Implemented controlled-file page query and detail query including route snapshot expansion.
- M6: Completed. Implemented submitter withdraw with BPM process cancellation and local DCC status update.
- M7: Completed.
  - GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS.
  - REGRESSION: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest,DccDirectoryAdminServiceImplTest,DccApprovalRouteAdminServiceImplTest,DccFileDirectoryServiceImplTest,DccFileCategoryMapperTest,DccBaseSchemaTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS.
  - REGRESSION: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -am -Dmaven.test.skip=true package` -> PASS.
- M8: Completed. Task-specific backend changes committed as `023e23889f` with message `任务: 完成DCC后端工作流接口`.
