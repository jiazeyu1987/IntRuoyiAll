# Execution Log: DCC v1 backend workflow id generation fix

BDD: 提交人通过 DCC 提交接口创建一条受控文件审批单 -> Given 原件上传已经成功并返回 `originalFileId`，且类别目录绑定、审批路线、审批岗位和 BPM 定义都已经就绪 / When 前端或直接 API 调用 `/dcc/controlled-files/submit` / Then 后端必须先成功插入 `dcc_controlled_file` 与 `dcc_controlled_file_route_snapshot`，再继续发起 BPM，而不是因为主键缺失在本地持久化阶段失败

## Evidence

- M1: Completed. Previous backend task `doc/tasks/20260513-dcc-v1-backend-original-upload-contract/task.md` is completed and committed in `9ff2f86eee`.
- M2: Completed. This task document, execution log, and backend evidence file were created before production code changes.
- M3:
  - RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileMapperTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, H2 DCC schema was stale (`effective_date` missing), which exposed that the workflow persistence contract and test schema had drifted.
  - RED: direct submit verification against `48082` -> FAIL, `/dcc/controlled-files/submit` raised `Field 'id' doesn't have a default value` while inserting `dcc_controlled_file`.
- M4:
  - GREEN: schema alignment fix implemented in `sql/mysql/20260513_dcc_base_schema.sql`, new migration `sql/mysql/20260513_dcc_id_auto_increment_fix.sql`, and H2 test schema `src/test/resources/sql/create_tables.sql`.
  - GREEN: workflow tables now follow MySQL `AUTO_INCREMENT` convention instead of local `ASSIGN_ID` exceptions.
- M5:
  - GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileMapperTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
  - GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
  - GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-server -am -DskipTests package` -> PASS after stopping the running `48082` jar so Spring Boot could repackage the artifact.
  - GREEN: direct runtime verification against `48082` -> PASS, `/dcc/controlled-files/submit` returned `data=2054538769366069249`, and the database showed:
    - `dcc_controlled_file.status = APPROVING`
    - `dcc_controlled_file.process_instance_id = f78055b4-4ec6-11f1-a751-00155db32d8f`
    - one matching `dcc_controlled_file_route_snapshot`
    - `act_ru_task` count increased to `1`
- M6:
  - Pending Git commit in the owning backend repository after evidence finalization.
