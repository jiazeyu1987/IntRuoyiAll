# Task: DCC Training Closed Loop

## Goal

Implement the DCC training closed loop so that:

- training recipients exactly inherit the published distribution recipients
- training users can enter a dedicated training path even without existing DCC
  category/directory view permission
- focused viewing time accumulates to 600 seconds before training can be
  acknowledged
- the DCC training page can track which file and which user have completed
  training
- the backend and database support a truthful end-to-end real-data E2E chain

## Scope

- Create database changes for user-level training progress and training view
  sessions.
- Rewire DCC finalization so training assignments reuse distribution
  recipients.
- Add dedicated training-task read/preview/heartbeat/acknowledge APIs.
- Extend read-side detail/training execution responses with accumulated viewing
  progress and eligibility.
- Keep unrelated dirty repository changes out of scope.
- Do not add fallback or mock-success behavior.

## Previous Task Check

- Previous backend task:
  `doc/tasks/20260516-batch-record-single-page-layout-constraints/task.md`
- Status before this task: blocked by user reprioritization before closeout.
- Impact: the paused batch-record layout task is unrelated and does not block
  this DCC training delivery.

## Milestones

- [x] M1: Check the previous backend task state and create this task package.
- [x] M2: Record BDD scenarios and RED evidence for schema/API/runtime gaps.
- [x] M3: Add failing schema and backend tests for training progress, timing,
  permissions, and acknowledgement.
- [x] M4: Implement schema, persistence, finalization, query, preview timing,
  and acknowledgement changes.
- [x] M5: Run focused GREEN backend/database verification and update evidence.
- [ ] M6: Commit only task-scoped backend files after verification passes.

## Expected Verification

- Focused DCC schema regression tests
- Focused DCC service/controller tests for training progress and acknowledgement
- Runtime verification against the local backend where required

## Current Status

Implemented and backend-verified. The DCC backend now persists
`training_progress` and `training_view_session`, derives training users from
distribution recipients, enforces the 600-second acknowledgement threshold,
extends training read-side data with progress fields, exposes dedicated
training-task APIs, and seeds the new `DCC我的培训` permission/menu path in SQL
artifacts.

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccTrainingTaskServiceTest,DccTrainingAssignmentAckServiceTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileQueryServiceTest,DccBaseSchemaTest -Dsurefire.failIfNoSpecifiedTests=false test`
  -> PASS
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package`
  -> PASS
- Runtime schema/menu applied to local isolated MySQL `127.0.0.1:23306`:
  - created `dcc_controlled_file_training_progress`
  - created `dcc_controlled_file_training_view_session`
  - inserted menu `6816 / controlled-file/training-mine`
  - linked menu `6816` to admin role `1`
