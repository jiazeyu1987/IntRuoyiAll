# QA Test Suite Evidence

## Scope And Target

- Feature under test: DCC training closed loop with real approval, real
  focused-view timing, and real acknowledgement persistence.
- Runtime target: `http://127.0.0.1:8081` frontend plus
  `http://127.0.0.1:48081` isolated backend.
- Task package:
  `doc/tasks/20260516-dcc-training-closed-loop/`

## Requirement To Test Matrix

- Requirement: training recipients must come from real distribution recipients.
  Test: live backend rows for generated files show progress rows for the
  distribution-recipient user set.
- Requirement: the training preview must enforce the 600-second focused-view
  threshold.
  Test: live UI/API runs created `view-session` rows and advanced one real
  submitter row to `accumulated_view_seconds >= 600`.
- Requirement: acknowledgement must persist and become visible to management.
  Test: live runtime rows show `acknowledged_at` populated, and
  `training-executions/page` returns the acknowledged row on the first page
  after the sort fix.

## Test Types

- E2E: applicable and required for the requested real user path.
- Regression: applicable because the repository previously had no training
  closed-loop chain.
- Accessibility: not part of this task scope.
- Compatibility: not part of this task scope.
- Performance: not part of this task scope.

## Test Data And Fixtures

- Real submitter account: `admin`
- Real approver accounts:
  - `yudao`
  - `yuanma`
  - `test`
  - `admin123`
- Real PDF:
  `D:/ocr2/resource/审核会签.pdf`
- Real category: `categoryId=1`
- Real training-recipient department: `deptId=103`

## RED:

- Long-chain verification initially failed for multiple genuine reasons that
  were then fixed in code/runtime:
  - missing progress/session tables
  - heartbeat session lookup using a null-equality mapper query
  - newest execution rows sorted off the first page
  - unstable direct page access to `training-mine`

## GREEN:

- Focused backend tests -> PASS
- Focused frontend ESLint -> PASS
- Focused frontend `vue-tsc --noEmit` -> PASS
- Real Playwright rerun -> PASS
- Live runtime facts -> PASS for the core business chain:
  - file `51` / submitter `userId=1` accumulated `606` seconds
  - file `51` / progress `67` wrote `acknowledged_at`
  - `training-executions/page` first page includes the acknowledged latest row

## Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccTrainingTaskServiceTest,DccTrainingAssignmentAckServiceTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileQueryServiceTest,DccBaseSchemaTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `pnpm exec eslint src/api/dcc/controlledFile/training.ts src/api/dcc/controlledFile/workflow.ts src/views/dcc/controlled-file/training/index.vue src/views/dcc/controlled-file/training/presentation.ts src/views/dcc/controlled-file/training/components/TrainingRulesReadonlyTab.vue src/views/dcc/controlled-file/training/components/TrainingExecutionTab.vue src/views/dcc/controlled-file/training/mine/index.vue src/views/dcc/controlled-file/training/task/index.vue src/views/dcc/controlled-file/detail/index.vue src/views/dcc/controlled-file/detail/presentation.ts src/router/modules/remaining.ts` -> PASS
- `set NODE_OPTIONS=--max-old-space-size=8192 && pnpm exec vue-tsc --noEmit` -> PASS
- Manual runtime verification via MySQL/API -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-training-closed-loop run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-training-closed-loop\scripts\verify-dcc-training-closed-loop-real-e2e.mjs` -> PASS
- Real rerun result:
  - controlled file `51`
  - training progress `67`
  - `accumulated_view_seconds >= 606`
  - `acknowledged_at` persisted
  - unauthorized non-recipient access attempt was rejected

## Blockers

- None for the task-scoped frontend closeout. Unrelated repository dirt stayed
  out of this task's staging set.
