# Execution Log: DCC v1 backend finalization

BDD: BPM 审批通过后生成盖章版并通知提交人 -> Given 受控文件处于 APPROVING 状态且 BPM 返回通过结果 / When DCC 流程结果监听器处理该事件 / Then 系统生成盖章 PDF、更新受控文件状态为 STAMPED、保存站内信并可通过受控下载接口获取该文件。

BDD: 未授权用户预览或下载被拒绝并记录日志 -> Given 用户不具备目标目录的 preview 或 download 权限 / When 访问受控预览或下载接口 / Then 系统拒绝访问并写入访问日志，不暴露原始文件公共地址。

## Evidence

- M1: Completed. Previous backend task `doc/tasks/20260513-dcc-v1-backend-workflow-apis/task.md` is completed.
- M2: Completed. This task document and execution log were created before backend finalization changes.
- M3: Completed.
  - RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -Dtest=DccPdfStampServiceTest,DccControlledFileFinalizationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, finalization service, PDF stamp service, access-denial error code, listener, and preview/download gate classes did not exist.
- M4: Completed. Added `DccControlledFileStatusListener` and finalization service handling BPM approve/reject/cancel outcomes.
- M5: Completed. Added PDFBox-based `DccPdfStampService` and stamped-file persistence logic with `stampedFileId` linkage.
- M6: Completed. Added preview/download gates with current directory-rule evaluation and access-log persistence.
- M7: Completed. Added `sql/mysql/20260513_dcc_notify_template_seed.sql` and wired submitter notifications for approved, rejected, and stamp-failed outcomes.
- M8: Completed.
  - GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -Dtest=DccPdfStampServiceTest,DccControlledFileFinalizationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS.
  - REGRESSION: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileFinalizationServiceImplTest,DccPdfStampServiceTest,DccControlledFileWorkflowServiceImplTest,DccDirectoryAdminServiceImplTest,DccApprovalRouteAdminServiceImplTest,DccFileDirectoryServiceImplTest,DccFileCategoryMapperTest,DccBaseSchemaTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS.
  - REGRESSION: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -am -Dmaven.test.skip=true package` -> PASS.
- M9: Completed. Task-specific backend finalization changes were committed as `36e8c1af32` with message `任务: 完成DCC后端结果处理与受控访问`, followed by task-record commit `79c2a0bbf4` with message `任务: 补充DCC后端结果处理记录`.
