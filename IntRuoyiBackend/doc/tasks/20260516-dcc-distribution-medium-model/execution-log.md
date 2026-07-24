# Execution Log: DCC 发放方式后端模型整改

BDD: distribution rule keeps delivery medium -> Given the matrix distinguishes
`PUBLIC_FOLDER` and `PAPER`, When a DCC category distribution rule is saved and
read back, Then the backend contract must preserve the selected delivery
medium instead of collapsing everything into department-only targets.

BDD: publish finalization snapshots the delivery medium -> Given a controlled
file is approved and published, When the backend creates downstream
distribution records, Then each record must keep the configured delivery medium
so later read-side logic can distinguish public-folder delivery from paper
delivery.

BDD: public-folder and paper rules do not share the exact same branch -> Given
the current system only has one digital distribution branch, When this task is
implemented, Then `PUBLIC_FOLDER` and `PAPER` must no longer be indistinguishable
in persistence and service behavior.

- M1: Completed. Created the backend task package before production code
  changes.
- RED: `mvn --% -pl yudao-module-dcc -Dtest=DccBaseSchemaTest,DccCategoryDistributionRuleAdminServiceImplTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
  -> FAIL, test compile reported missing `DccDistributionMediumEnum` and the
  missing new medium fields across rule / distribution test expectations.
- RED: the same command then exposed an existing repository compile blocker in
  DCC tests because `DccControlledFileUploadNameOptionApiTest` and
  `DccControlledFileUploadNameOptionQueryServiceTest` referenced a missing
  `DccControlledFileUploadNameOptionRespVO`.
- M2: Completed. Recorded the RED evidence for the missing distribution-medium
  model and the existing DCC test compile blocker.
- M3: Completed. Added `DccDistributionMediumEnum`, persisted
  `distribution_medium` on category rules and distribution records, updated
  base/runtime/test SQL schemas, restored the missing upload-name-option VO,
  and wired finalization plus detail query logic to keep the new medium.
- GREEN: `mvn --% -pl yudao-module-dcc -Dtest=DccBaseSchemaTest,DccCategoryDistributionRuleAdminServiceImplTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
  -> PASS, 19 tests green after aligning the finalization tests with the
  current in-progress training-recipient inheritance behavior.
- M4: Completed. Targeted backend verification is green and evidence files are
  ready for validation.
