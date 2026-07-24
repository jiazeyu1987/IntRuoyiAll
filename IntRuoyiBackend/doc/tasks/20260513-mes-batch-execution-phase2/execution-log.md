BDD: create one execution from one template -> Given a saved batch-record template and an existing work order, When the operator creates an execution with templateId + workOrderId + batchCode, Then the system creates one draft execution using a template snapshot and stores batchCode explicitly.

BDD: save draft cell values and reopen -> Given an execution instance is still in draft status, When the operator saves draft cell values and a remark, Then the system replaces the current cell-value set and returns the same values on get.

BDD: submitted executions become read-only for draft save -> Given an execution instance has been submitted, When the operator tries to save draft values again, Then the request is rejected and the stored draft data remains unchanged.

BDD: submit only works from draft -> Given an execution instance is still draft, When the operator submits it, Then the status changes to submitted and later submit/save-draft requests fail fast.

RED: mvn -f yudao-module-mes/pom.xml "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, compilation breaks because the Phase 2 execution VO/DO/Mapper/Service/Controller classes do not exist yet.

RED: mvn -f yudao-module-mes/pom.xml "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, after the minimal execution code lands the focused tests still fail because the H2 test fixture for mes_pro_work_order is missing columns that MesProWorkOrderMapper.selectById queries.

GREEN: mvn -f yudao-module-mes/pom.xml "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS

GREEN: direct HTTP verification -> PASS, the clean backend on `http://127.0.0.1:48083/admin-api` accepted execution `page`, `create`, `get`, `save-draft`, and `submit` with real auth and real runtime data.

GREEN: Playwright integrated smoke support -> PASS, the clean backend successfully supported the frontend real-user flow for `execution list -> create -> save draft -> reopen -> submit -> read-only`.
