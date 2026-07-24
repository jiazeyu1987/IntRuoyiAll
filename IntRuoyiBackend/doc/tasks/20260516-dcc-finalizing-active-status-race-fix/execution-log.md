# Execution Log: DCC 终审后状态滞留 FINALIZING 修复

BDD: final approval must hand off cleanly to finalization -> Given the last DCC
approval stage is completed and BPM reports no remaining running tasks, When
the backend approves the final task, Then the workflow layer must not persist a
stale `FINALIZING` status that can overwrite the subsequent finalization result.

BDD: published DCC files must become previewable after finalization -> Given the
finalization listener creates published-file side effects, When the backend
reads the controlled file afterward, Then the file should be in the published
state expected by preview/download rules rather than remaining blocked in
`FINALIZING`.

- M1: Completed. The latest backend task `20260516-dcc-special-position-list-visibility` was already completed, and this fix stayed scoped away from other dirty DCC preview-metadata files.
- RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, the new regression `approveTask_genericApproveTaskKey_finalStageDoesNotPersistFinalizingStatus` observed `controlledFileMapper.updateById(...status=FINALIZING)` on the last approval stage.
- GREEN: same Maven command -> PASS after removing the last-stage `FINALIZING` write from `syncStatusAfterApprove`.
- GREEN: follow-up real frontend E2E from `yudao-ui-admin-vue3` reached a published `现行` file again, proving the listener-owned `ACTIVE` transition now wins end to end.
