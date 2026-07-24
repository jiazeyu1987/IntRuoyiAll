# Execution Log: DCC Training Closed Loop

BDD: published training recipients inherit distribution recipients exactly ->
Given a controlled file category requires training and distribution / When the
 file is finalized successfully / Then the created training user assignments and
 file-user training progress rows must come from the resolved distribution
 recipients rather than independently resolved training departments.

BDD: training users accumulate focused preview time before acknowledgement ->
Given a training user opens the dedicated training preview path for an assigned
 file / When focused preview sessions and heartbeats accumulate at least 600
 seconds / Then the backend marks the progress row eligible for acknowledgement
 and rejects acknowledgement before the threshold is met.

BDD: training completion updates file-user and department-level status ->
Given one training user reaches the 600-second threshold / When the user
acknowledges training / Then the backend stores the acknowledgement timestamp,
updates every pending assignment for that file-user pair, and marks the
department-level training row acknowledged only after all users for that row
finish.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccBaseSchemaTest,DccControlledFileFinalizationServiceImplTest,DccTrainingAssignmentAckServiceTest,DccControlledFileQueryServiceTest,DccTrainingTaskServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
-> FAIL initially, because `dcc_controlled_file_training_progress`,
`dcc_controlled_file_training_view_session`, the dedicated training-task VOs,
and the new 600-second threshold error code did not exist yet.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccTrainingTaskServiceTest,DccTrainingAssignmentAckServiceTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileQueryServiceTest,DccBaseSchemaTest -Dsurefire.failIfNoSpecifiedTests=false test`
-> PASS, covering:
- schema baseline for the new training progress/session tables and menu seed
- finalization inheritance from distribution recipients
- 600-second acknowledgement threshold
- training-task heartbeat/session aggregation
- read-side assignment progress enrichment

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package`
-> PASS, producing a runnable `yudao-server.jar` after unrelated MES
test-compile blockers were bypassed with `maven.test.skip=true`.
