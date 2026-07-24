# Execution Log: DCC v1 backend user-flow contract

BDD: 提交人上传受控文件时提交必需业务元数据 -> Given 上传页按照批准设计收集文件类别、标题、版本、生效日期和备注 / When 前端调用 DCC 受控文件提交接口 / Then 后端持久化这些字段并在详情与列表返回中保持一致。
BDD: 受控文件浏览页按目录过滤记录 -> Given 用户从 DCC 目录树中选择一个具体目录 / When 前端调用受控文件分页接口并传入目录编号 / Then 后端只返回该目录下的受控文件记录，不静默忽略目录过滤参数。
BDD: DCC 用户流菜单具备可访问入口 -> Given 用户拥有 DCC 对应权限 / When 系统初始化 DCC 菜单种子 / Then 受控上传、我的文件、审批任务、受控浏览等用户流入口都具有明确路径和组件映射。

## Evidence

- M1: Completed. Previous backend task `doc/tasks/20260513-dcc-v1-backend-finalization/task.md` is completed.
- M2: Completed. This task document, execution log, backend API evidence, and database schema evidence were created before production code changes.
- M3: Completed.
  - RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest,DccBaseSchemaTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, submit request, DO, response VO, and page request were missing `effectiveDate`, `remark`, and `directoryId`, and the schema did not yet seed mine or approval-task menu entries.
- M4: Completed. Extended `dcc_controlled_file` schema, `DccControlledFileDO`, `DccControlledFileSubmitReqVO`, `DccControlledFileRespVO`, and `DccControlledFilePageReqVO` for effective date, submit remark, and directory filtering.
- M5: Completed. Updated `DccControlledFileWorkflowServiceImpl`, `DccControlledFileMapper`, and the MySQL DCC menu seed to support metadata persistence, directory filtering, and the `mine` plus `approval-tasks` entry points.
- M6: Completed.
  - GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest,DccBaseSchemaTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS.
  - REGRESSION: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -am -Dmaven.test.skip=true package` -> PASS.
  - GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260513-dcc-v1-backend-user-flow-contract/backend-api-evidence.md` -> PASS.
  - GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260513-dcc-v1-backend-user-flow-contract/database-schema-evidence.md` -> PASS.
- M7: Completed. Task-specific backend contract-closure changes were committed as `81aa1e8337` with message `任务: 补齐DCC用户流后端契约`, followed by task-record commit `80d26fbae3` with message `任务: 补充DCC用户流后端契约记录`.
